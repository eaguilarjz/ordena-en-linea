package sv.ufg.ordenaenlinea.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import sv.ufg.ordenaenlinea.model.Categoria;
import sv.ufg.ordenaenlinea.repository.ArchivoRepository;
import sv.ufg.ordenaenlinea.repository.CategoriaRepository;
import sv.ufg.ordenaenlinea.util.ArchivoUtil;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class CategoriaService {
    private final CategoriaRepository categoriaRepository;
    private final ArchivoRepository archivoRepository;
    private final ArchivoUtil archivoUtil;
    private final String CARPETA = "categoria";

    public Page<Categoria> obtenerCategorias(Pageable pageable) {
        return categoriaRepository.findAll(pageable);
    }

    public Categoria obtenerCategoriaPorId(Integer idCategoria) {
        Categoria categoria = categoriaRepository.findById(idCategoria).orElseThrow(
                () -> new EntityNotFoundException(String.format("La categoria con id %s no existe", idCategoria))
        );
        return categoria;
    }

    public byte[] obtenerImagenCategoria(Integer idCategoria) {
        Categoria categoria = categoriaRepository.findById(idCategoria).orElseThrow(
                () -> new EntityNotFoundException(String.format("La categoria con id %s no existe", idCategoria))
        );

        if (categoria.getUrlImagen() == null || categoria.getUrlImagen().isBlank())
            return new byte[0];
        else
            return archivoRepository.descargar(CARPETA, categoria.getUrlImagen());
    }

    public Categoria postearCategoria(Categoria categoria) {
        Optional<Categoria> categoriaHomonima = categoriaRepository.findByNombre(categoria.getNombre());
        if (categoriaHomonima.isPresent())
            throw new EntityExistsException(String.format("La categoria '%s' ya existe", categoria.getNombre()));

        return categoriaRepository.save(categoria);
    }

    public Categoria modificarCategoria(Integer idCategoria, Categoria categoria) {
        Categoria categoriaAModificar = categoriaRepository.findById(idCategoria).orElseThrow(
                () -> new EntityNotFoundException(String.format("La categoria con id %s no existe", idCategoria))
        );

        // Si el nombre no ha sido modificado, no realizar ninguna acción
        if (Objects.equals(categoria.getNombre(), categoriaAModificar.getNombre()))
            return categoriaAModificar;

        Optional<Categoria> categoriaHomonima = categoriaRepository.findByNombre(categoria.getNombre());
        if (categoriaHomonima.isPresent())
            throw new EntityExistsException(String.format("La categoria '%s' ya existe", categoria.getNombre()));

        categoriaAModificar.setNombre(categoria.getNombre());
        return categoriaRepository.save(categoriaAModificar);
    }

    public void modificarImagenCategoria(Integer idCategoria, MultipartFile archivo) {
        // Validaciones
        archivoUtil.esArchivoNoVacio(archivo);
        archivoUtil.esImagen(archivo);

        // Obtener categoría a actualizar
        Categoria categoria = categoriaRepository.findById(idCategoria).orElseThrow(
                () -> new EntityNotFoundException(String.format("La categoria con id %s no existe", idCategoria))
        );

        // Extraer metadatos del archivo
        Map<String, String> metadata = archivoUtil.extraerMetadata(archivo);

        // Guardar el nombre del archivo actual (para borrarlo después de subir el nuevo)
        String nombreArchivoAnterior = categoria.getUrlImagen();

        // Guardar imagen en S3 y actualizar ruta en la categoria
        try {
            String nombreArchivo = archivoUtil.obtenerNuevoNombreArchivo(archivo);
            archivoRepository.subir(CARPETA, nombreArchivo, Optional.of(metadata), archivo.getInputStream());
            categoria.setUrlImagen(nombreArchivo); // Actualizar la URL de la imagen
            categoriaRepository.save(categoria);

            // Borrar el archivo anterior (de existir)
            if (nombreArchivoAnterior != null && !nombreArchivoAnterior.isBlank())
                archivoRepository.borrar(CARPETA, nombreArchivoAnterior);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    String.format("No se pudo actualizar la imagen de la categoria %s", idCategoria), e
            );
        }
    }

    public void borrarCategoria(Integer idCategoria) {
        // Obtener categoría a actualizar. De no existir, no realizar ninguna accion (DELETE is idempotent)
        Optional<Categoria> categoria = categoriaRepository.findById(idCategoria);
        if (categoria.isEmpty()) return;

        // Guardar la URL de la imagen actual
        String urlImagen = categoria.get().getUrlImagen();

        // Borrar la categoria
        categoriaRepository.delete(categoria.get());

        // Si la categoria tenia una imagen asociada, borrarla del repositorio
        if (urlImagen != null && !urlImagen.isBlank())
            archivoRepository.borrar(CARPETA, urlImagen);
    }

    public void borrarImagenCategoria(Integer idCategoria) {
        // Obtener categoría a actualizar. De no existir, no realizar ninguna accion (DELETE is idempotent)
        Optional<Categoria> categoria = categoriaRepository.findById(idCategoria);
        if (categoria.isEmpty()) return;

        // Si no hay una imagen asociada a la categoría, no realizar ninguna acción
        if (categoria.get().getUrlImagen() == null || categoria.get().getUrlImagen().isBlank()) return;

        // Borrar la imagen del repositorio
        archivoRepository.borrar(CARPETA, categoria.get().getUrlImagen());

        // Actualizar la URL de la imagen
        categoria.get().setUrlImagen(null);

        categoriaRepository.save(categoria.get());
    }
}