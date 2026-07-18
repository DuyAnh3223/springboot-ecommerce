package spring.abtechzone.modules.product.entity;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Pattern;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import lombok.*;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.modules.category.entity.Brand;
import spring.abtechzone.modules.category.entity.Category;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SQLRestriction("deleted_at IS NULL")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false)
    String name;

    @Column(nullable = false)
    String slug;

    String thumbnail;

    @Column(length = 2000)
    String description;

    @DecimalMin("0.0")
    @DecimalMax("5.0")
    Double rating;

    boolean isDraft;

    boolean isPublished;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "sku_count", nullable = false)
    @Builder.Default
    Integer skuCount = 0;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "active_sku_count", nullable = false)
    @Builder.Default
    Integer activeSkuCount = 0;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "total_stock", nullable = false)
    @Builder.Default
    Integer totalStock = 0;

    @Column(name = "price_min")
    BigDecimal priceMin;

    @Column(name = "price_max")
    BigDecimal priceMax;

    @JdbcTypeCode(SqlTypes.JSON)
    Map<String, Object> attributes = new HashMap<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    List<ProductSku> skus;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    Brand brand;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "review_count", nullable = false)
    @Builder.Default
    Integer reviewCount = 0;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP(6)")
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    OffsetDateTime createdAt = OffsetDateTime.now();

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP(6)")
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    OffsetDateTime updatedAt = OffsetDateTime.now();

    @Column(name = "deleted_at")
    OffsetDateTime deletedAt;

    @Column(name = "seller_id")
    UUID sellerId;

    @Transient
    String originalName;

    @PostLoad
    protected void onLoad() {
        this.originalName = this.name;
    }

    @PrePersist
    protected void onPrePersist() {
        if (this.slug == null || this.slug.isBlank()) {
            this.slug = generateSlug(this.name);
        }
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
        this.updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onPreUpdate() {
        if (this.originalName == null || !this.originalName.equals(this.name)) {
            this.slug = generateSlug(this.name);
        }
        this.updatedAt = OffsetDateTime.now();
    }

    public void softDelete() {
        this.deletedAt = OffsetDateTime.now();
        if (this.skus != null) {
            this.skus.forEach(ProductSku::softDelete);
        }
    }

    public String generateSlug(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }
        // Normalize and remove accents
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String slug = pattern.matcher(normalized).replaceAll("");

        // Handle Vietnamese specific characters 'Đ' and 'đ'
        slug = slug.replace("đ", "d").replace("Đ", "D");

        return slug.toLowerCase(Locale.ENGLISH)
                .replaceAll("[^a-z0-9\\s-]", "") // Remove special characters
                .replaceAll("\\s+", "-") // Replace spaces with hyphens
                .replaceAll("-+", "-") // Remove duplicate hyphens
                .replaceAll("^-|-$", ""); // Remove leading/trailing hyphens
    }
}
