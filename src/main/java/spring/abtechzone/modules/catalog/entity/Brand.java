package spring.abtechzone.modules.catalog.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "brand")
public class Brand {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 150)
    @NotNull
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Size(max = 150)
    @NotNull
    @Column(name = "slug", nullable = false, length = 150)
    private String slug;

    @Size(max = 500)
    @Column(name = "logo_url", length = 500)
    private String logoUrl;
}
