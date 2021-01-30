package sv.ufg.ordenaenlinea.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Set;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "producto_nombre_unq", columnNames = "nombre")
})
@Getter @Setter @NoArgsConstructor
public class Producto {
    @Id
    @SequenceGenerator(name = "producto_id_seq", sequenceName = "producto_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "producto_id_seq")
    private Integer id;

    @NotBlank
    @Column(nullable = false)
    private String nombre;

    private String urlImagen;

    @ManyToOne
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @NotNull
    @DecimalMin(value = "0.01")
    @Column(nullable = false)
    private BigDecimal precio;

    @OneToMany(mappedBy = "producto")
    private Set<OrdenDetalle> detalles;
}
