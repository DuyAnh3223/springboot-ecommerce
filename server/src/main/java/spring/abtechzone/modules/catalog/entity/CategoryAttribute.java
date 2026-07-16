package spring.abtechzone.modules.catalog.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "category_attribute")
public class CategoryAttribute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "attribute_id", nullable = false)
    private Attribute attribute;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "is_filterable", nullable = false)
    private Boolean isFilterable;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "is_variant_defining", nullable = false)
    private Boolean isVariantDefining;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "is_compatibility_key", nullable = false)
    private Boolean isCompatibilityKey;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "is_required", nullable = false)
    private Boolean isRequired;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
