package spring.abtechzone.modules.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import spring.abtechzone.common.BaseIT;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.modules.category.entity.Category;
import spring.abtechzone.modules.category.repository.CategoryRepository;
import spring.abtechzone.modules.inventory.entity.Inventory;
import spring.abtechzone.modules.inventory.repository.InventoryRepository;
import spring.abtechzone.modules.inventory.repository.StockMovementRepository;
import spring.abtechzone.modules.inventory.service.InventoryService;
import spring.abtechzone.modules.product.entity.Product;
import spring.abtechzone.modules.product.entity.ProductSku;
import spring.abtechzone.modules.product.repository.ProductRepository;
import spring.abtechzone.modules.product.repository.ProductSkuRepository;

class InventoryPersistenceIT extends BaseIT {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductSkuRepository productSkuRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Autowired
    private InventoryService inventoryService;

    @BeforeEach
    void clean() {
        stockMovementRepository.deleteAll();
        inventoryRepository.deleteAll();
        productSkuRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void sharedPrimaryKeyAndGuardedCommandsUseInventoryAsSourceOfTruth() {
        ProductSku sku = createSku();
        Inventory inventory = inventoryService.createForSku(sku, 5);

        assertThat(inventory.getSkuId()).isEqualTo(sku.getId());
        assertThat(inventoryRepository.findById(sku.getId()).orElseThrow().getOnHand())
                .isEqualTo(5);

        inventoryService.decreaseStock(sku, 3, null);
        assertThat(inventoryRepository.findById(sku.getId()).orElseThrow().getOnHand())
                .isEqualTo(2);
        assertThat(stockMovementRepository.findAll()).singleElement().satisfies(movement -> {
            assertThat(movement.getReason()).isEqualTo("SALE_OUT");
            assertThat(movement.getChangeQty()).isEqualTo(-3);
        });

        assertThatThrownBy(() -> inventoryService.decreaseStock(sku, 3, null)).isInstanceOf(AppException.class);
        assertThat(inventoryRepository.findById(sku.getId()).orElseThrow().getOnHand())
                .isEqualTo(2);
    }

    @Test
    void invalidAndDuplicateBalancesAreRejected() {
        ProductSku sku = createSku();
        assertThatThrownBy(() -> inventoryService.createForSku(sku, -1)).isInstanceOf(AppException.class);
        inventoryService.createForSku(sku, 0);
        assertThatThrownBy(() -> inventoryService.createForSku(sku, 0)).isInstanceOf(AppException.class);
    }

    private ProductSku createSku() {
        Category category = new Category();
        category.setName("Inventory Category");
        category.setSlug("inventory-category");
        category.setIsActive(true);
        category = categoryRepository.save(category);
        Product product = productRepository.save(Product.builder()
                .name("Inventory Product")
                .slug("inventory-product")
                .category(category)
                .published(true)
                .draft(false)
                .build());
        return productSkuRepository.save(ProductSku.builder()
                .product(product)
                .sku("INV-SKU-1")
                .price(BigDecimal.ONE)
                .build());
    }
}
