package spring.abtechzone.modules.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import spring.abtechzone.common.BaseIT;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.modules.category.entity.Category;
import spring.abtechzone.modules.category.repository.CategoryRepository;
import spring.abtechzone.modules.inventory.constant.StockMovementReason;
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

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
        assertThat(stockMovementRepository.findAll())
                .extracting(movement -> List.of(movement.getReason(), movement.getChangeQty()))
                .containsExactlyInAnyOrder(
                        List.of(StockMovementReason.OPENING_BALANCE, 5), List.of(StockMovementReason.SALE_OUT, -3));

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

    @Test
    void movementHistory_filtersBySkuAndReasonWithStableOrdering() {
        ProductSku sku = createSku();
        inventoryService.createForSku(sku, 5);
        inventoryService.setOnHand(sku.getId(), 8);
        inventoryService.setOnHand(sku.getId(), 9);
        inventoryService.decreaseStock(sku, 2, null);

        sku.softDelete();
        productSkuRepository.saveAndFlush(sku);

        org.springframework.data.domain.Page<StockMovementRepository.StockMovementHistoryProjection> page;
        try {
            page = stockMovementRepository.searchHistory(
                    sku.getId(), StockMovementReason.MANUAL_ADJUSTMENT_IN.name(), PageRequest.of(0, 20));
        } finally {
            jdbcTemplate.update("update product_sku set deleted_at = null, is_active = true where id = ?", sku.getId());
        }

        assertThat(page.getContent()).hasSize(2).allSatisfy(movement -> {
            assertThat(movement.getSkuId()).isEqualTo(sku.getId());
            assertThat(movement.getSkuCode()).isEqualTo(sku.getSku());
            assertThat(movement.getReason()).isEqualTo(StockMovementReason.MANUAL_ADJUSTMENT_IN.name());
        });
        assertThat(page.getContent())
                .extracting(movement -> movement.getChangeQty())
                .containsExactly(1, 3);
        assertThat(page.getContent().get(0).getMovementId())
                .isGreaterThan(page.getContent().get(1).getMovementId());

        var allMovements = stockMovementRepository.searchHistory(null, null, PageRequest.of(0, 20));
        assertThat(allMovements.getContent()).hasSize(4);
    }

    @Test
    void concurrentSaleAndAuditedAdminAdjustTo_doNotLoseCommittedDelta() throws Exception {
        ProductSku sku = createSku();
        inventoryService.createForSku(sku, 10);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var sale = executor.submit(() -> {
                start.await();
                inventoryService.decreaseStock(sku, 2, null);
                return null;
            });
            var adjust = executor.submit(() -> {
                start.await();
                inventoryService.setOnHand(sku.getId(), 15);
                return null;
            });

            start.countDown();
            sale.get(10, TimeUnit.SECONDS);
            adjust.get(10, TimeUnit.SECONDS);
        }

        int finalOnHand =
                inventoryRepository.findById(sku.getId()).orElseThrow().getOnHand();
        List<spring.abtechzone.modules.inventory.entity.StockMovement> movements = stockMovementRepository.findAll();
        assertThat(finalOnHand).isIn(13, 15);
        assertThat(movements).hasSize(3);
        assertThat(movements.stream()
                        .mapToInt(movement -> movement.getChangeQty())
                        .sum())
                .isEqualTo(finalOnHand);
        assertThat(movements).anySatisfy(movement -> {
            assertThat(movement.getReason()).isEqualTo(StockMovementReason.SALE_OUT);
            assertThat(movement.getChangeQty()).isEqualTo(-2);
        });
        assertThat(movements).anySatisfy(movement -> {
            assertThat(movement.getReason()).isEqualTo(StockMovementReason.MANUAL_ADJUSTMENT_IN);
            assertThat(movement.getChangeQty()).isIn(5, 7);
        });
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
