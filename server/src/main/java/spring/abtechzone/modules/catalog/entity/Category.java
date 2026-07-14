package spring.abtechzone.modules.catalog.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.hibernate.annotations.ColumnDefault;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "category")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @Size(max = 150)
    @NotNull
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Size(max = 150)
    @NotNull
    @Column(name = "slug", nullable = false, length = 150)
    private String slug;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "sort_order", nullable = true)
    private Integer sortOrder = 0;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    private String thumbnail;
}
