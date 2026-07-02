package spring.abtechzone.modules.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode
@Embeddable
public class ProductBundleItemId implements Serializable {
    private static final long serialVersionUID = 3914367832507867921L;
    @NotNull
    @Column(name = "bundle_id", nullable = false)
    private Long bundleId;

    @NotNull
    @Column(name = "sku_id", nullable = false)
    private Long skuId;


}