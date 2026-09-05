package spring.abtechzone.modules.inventory.repository.specification;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.springframework.data.jpa.domain.Specification;

import spring.abtechzone.modules.inventory.entity.Inventory;
import spring.abtechzone.modules.product.entity.Product;

/** Inventory-owned predicates consumed by catalog/product queries. */
public final class InventorySpecifications {

    private InventorySpecifications() {}

    public static Specification<Product> hasAvailableSku() {
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<Inventory> inventoryRoot = subquery.from(Inventory.class);
            subquery.select(inventoryRoot.get("skuId"))
                    .where(
                            cb.equal(inventoryRoot.get("productSku").get("product"), root),
                            cb.isNull(inventoryRoot.get("productSku").get("deletedAt")),
                            cb.equal(inventoryRoot.get("productSku").get("active"), true),
                            cb.greaterThan(inventoryRoot.get("onHand"), 0));
            return cb.exists(subquery);
        };
    }
}
