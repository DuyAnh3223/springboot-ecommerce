package spring.abtechzone.modules.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.auth.service.AuthService;
import spring.abtechzone.modules.inventory.constant.StockAdjustmentOperation;
import spring.abtechzone.modules.inventory.constant.StockMovementReason;
import spring.abtechzone.modules.inventory.dto.request.StockAdjustmentRequest;
import spring.abtechzone.modules.inventory.dto.request.StockMovementSearchRequest;
import spring.abtechzone.modules.inventory.dto.response.StockAdjustmentResponse;
import spring.abtechzone.modules.inventory.entity.Inventory;
import spring.abtechzone.modules.inventory.entity.StockMovement;
import spring.abtechzone.modules.inventory.mapper.StockMovementMapper;
import spring.abtechzone.modules.inventory.repository.InventoryRepository;
import spring.abtechzone.modules.inventory.repository.StockMovementRepository;
import spring.abtechzone.modules.inventory.service.InventoryService;
import spring.abtechzone.modules.order.entity.Order;
import spring.abtechzone.modules.product.entity.ProductSku;
import spring.abtechzone.modules.product.repository.ProductSkuRepository;
import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    StockMovementRepository stockMovementRepository;

    @Mock
    InventoryRepository inventoryRepository;

    @Mock
    ProductSkuRepository productSkuRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    AuthService authService;

    @Spy
    StockMovementMapper stockMovementMapper = Mappers.getMapper(StockMovementMapper.class);

    @InjectMocks
    InventoryService inventoryService;

    @Test
    void decreaseStock_success_writesSaleOutMovement() {
        ProductSku sku = sku(10L);
        Order order = Order.builder().id(99L).build();
        when(inventoryRepository.decreaseOnHand(10L, 2)).thenReturn(1);

        inventoryService.decreaseStock(sku, 2, order);

        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(captor.capture());
        assertThat(captor.getValue().getSku()).isSameAs(sku);
        assertThat(captor.getValue().getChangeQty()).isEqualTo(-2);
        assertThat(captor.getValue().getReason()).isEqualTo(StockMovementReason.SALE_OUT);
        assertThat(captor.getValue().getReferenceId()).isEqualTo("99");
    }

    @Test
    void decreaseStock_zeroRows_throwsInsufficientAndWritesNoMovement() {
        ProductSku sku = sku(10L);
        when(inventoryRepository.decreaseOnHand(10L, 2)).thenReturn(0);

        assertThatThrownBy(() -> inventoryService.decreaseStock(sku, 2, null))
                .isInstanceOf(AppException.class)
                .hasMessageContaining(ErrorCode.INSUFFICIENT_STOCK.getMessage());

        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void increaseStock_success_writesCancellationMovement() {
        ProductSku sku = sku(10L);
        Order order = Order.builder().id(99L).build();
        when(inventoryRepository.increaseOnHand(eq(10L), eq(2), eq(Integer.MAX_VALUE)))
                .thenReturn(1);

        inventoryService.increaseStock(10L, 2, order, sku);

        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(captor.capture());
        assertThat(captor.getValue().getChangeQty()).isEqualTo(2);
        assertThat(captor.getValue().getReason()).isEqualTo(StockMovementReason.ORDER_CANCEL_RETURN);
    }

    @Test
    void increaseStock_zeroRows_failsClosedWithoutMovement() {
        ProductSku sku = sku(10L);
        when(inventoryRepository.increaseOnHand(eq(10L), eq(2), eq(Integer.MAX_VALUE)))
                .thenReturn(0);

        assertThatThrownBy(() -> inventoryService.increaseStock(10L, 2, null, sku))
                .isInstanceOf(AppException.class)
                .hasMessageContaining(ErrorCode.SYSTEM_ERROR.getMessage());

        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void createForSku_rejectsNegativeOnHand() {
        ProductSku sku = sku(10L);

        assertThatThrownBy(() -> inventoryService.createForSku(sku, -1))
                .isInstanceOf(AppException.class)
                .hasMessageContaining(ErrorCode.PRODUCT_STOCK_INVALID.getMessage());
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void createForSku_rejectsUnknownSkuAndPersistsKnownSkuBalanceWithOpeningMovement() {
        ProductSku sku = sku(10L);
        when(productSkuRepository.existsById(10L)).thenReturn(true);
        when(inventoryRepository.existsById(10L)).thenReturn(false);
        when(inventoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(inventoryService.createForSku(sku, 4).getOnHand()).isEqualTo(4);
        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(captor.capture());
        assertThat(captor.getValue().getChangeQty()).isEqualTo(4);
        assertThat(captor.getValue().getReason()).isEqualTo(StockMovementReason.OPENING_BALANCE);
        assertThat(captor.getValue().getReferenceId()).isEqualTo("10");

        when(productSkuRepository.existsById(99L)).thenReturn(false);
        assertThatThrownBy(() -> inventoryService.createForSku(sku(99L), 4))
                .isInstanceOf(AppException.class)
                .hasMessageContaining(ErrorCode.SKU_NOT_FOUND.getMessage());
    }

    @Test
    void createForSku_zeroOnHand_doesNotWriteZeroMovement() {
        ProductSku sku = sku(10L);
        when(productSkuRepository.existsById(10L)).thenReturn(true);
        when(inventoryRepository.existsById(10L)).thenReturn(false);
        when(inventoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(inventoryService.createForSku(sku, 0).getOnHand()).isZero();

        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void setOnHand_increase_writesAuditedDeltaFromLockedBalance() {
        ProductSku sku = sku(10L);
        Inventory inventory =
                Inventory.builder().skuId(10L).productSku(sku).onHand(10).build();
        when(inventoryRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(inventory));

        inventoryService.setOnHand(10L, 15);

        assertThat(inventory.getOnHand()).isEqualTo(15);
        ArgumentCaptor<StockMovement> captor = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(captor.capture());
        assertThat(captor.getValue().getChangeQty()).isEqualTo(5);
        assertThat(captor.getValue().getReason()).isEqualTo(StockMovementReason.MANUAL_ADJUSTMENT_IN);
    }

    @Test
    void setOnHand_sameBalance_isNoOpWithoutMovement() {
        ProductSku sku = sku(10L);
        Inventory inventory =
                Inventory.builder().skuId(10L).productSku(sku).onHand(10).build();
        when(inventoryRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(inventory));

        inventoryService.setOnHand(10L, 10);

        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void adjustStock_increase_writesActorAndReturnsAuthoritativeBalance() {
        ProductSku sku = sku(10L);
        Inventory inventory =
                Inventory.builder().skuId(10L).productSku(sku).onHand(10).build();
        User admin = User.builder().id(UUID.randomUUID()).username("admin").build();
        when(inventoryRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(inventory));
        when(authService.getCurrentUsername()).thenReturn("admin");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(stockMovementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        StockAdjustmentResponse response = inventoryService.adjustStock(
                10L,
                StockAdjustmentRequest.builder()
                        .operation(StockAdjustmentOperation.INCREASE)
                        .quantity(5)
                        .reason(StockMovementReason.PURCHASE_IN)
                        .build());

        assertThat(inventory.getOnHand()).isEqualTo(15);
        assertThat(response.getOnHand()).isEqualTo(15);
        assertThat(response.getMovement().getCreatedBy()).isEqualTo("admin");
        assertThat(response.getMovement().getReason()).isEqualTo(StockMovementReason.PURCHASE_IN);
    }

    @Test
    void adjustStock_rejectsReasonThatDoesNotMatchDirection() {
        StockAdjustmentRequest request = StockAdjustmentRequest.builder()
                .operation(StockAdjustmentOperation.DECREASE)
                .quantity(2)
                .reason(StockMovementReason.PURCHASE_IN)
                .build();

        assertThatThrownBy(() -> inventoryService.adjustStock(10L, request))
                .isInstanceOf(AppException.class)
                .hasMessageContaining(ErrorCode.INVENTORY_ADJUSTMENT_INVALID.getMessage());

        verify(inventoryRepository, never()).findByIdForUpdate(any());
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void adjustStock_decreaseInsufficient_doesNotChangeBalanceOrWriteMovement() {
        ProductSku sku = sku(10L);
        Inventory inventory =
                Inventory.builder().skuId(10L).productSku(sku).onHand(1).build();
        when(inventoryRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.adjustStock(
                        10L,
                        StockAdjustmentRequest.builder()
                                .operation(StockAdjustmentOperation.DECREASE)
                                .quantity(2)
                                .reason(StockMovementReason.DAMAGE_OUT)
                                .build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining(ErrorCode.INSUFFICIENT_STOCK.getMessage());

        assertThat(inventory.getOnHand()).isEqualTo(1);
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void getStockMovements_filtersAndUsesStableNewestFirstOrdering() {
        ProductSku sku = sku(10L);
        StockMovementRepository.StockMovementHistoryProjection movement =
                org.mockito.Mockito.mock(StockMovementRepository.StockMovementHistoryProjection.class);
        when(movement.getMovementId()).thenReturn(7L);
        when(movement.getSkuId()).thenReturn(10L);
        when(movement.getSkuCode()).thenReturn(sku.getSku());
        when(movement.getChangeQty()).thenReturn(5);
        when(movement.getReason()).thenReturn(StockMovementReason.PURCHASE_IN.name());
        when(movement.getCreatedAt()).thenReturn(OffsetDateTime.now());
        when(stockMovementRepository.searchHistory(eq(10L), eq(StockMovementReason.PURCHASE_IN.name()), any()))
                .thenReturn(new PageImpl<>(List.of(movement)));

        var page = inventoryService.getStockMovements(StockMovementSearchRequest.builder()
                .skuId(10L)
                .reason(StockMovementReason.PURCHASE_IN)
                .page(0)
                .size(20)
                .build());

        assertThat(page.getContent()).singleElement().satisfies(response -> {
            assertThat(response.getMovementId()).isEqualTo(7L);
            assertThat(response.getSkuId()).isEqualTo(10L);
            assertThat(response.getReason()).isEqualTo(StockMovementReason.PURCHASE_IN);
        });
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(stockMovementRepository)
                .searchHistory(eq(10L), eq(StockMovementReason.PURCHASE_IN.name()), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void getOnHandOrZero_failsClosedForMissingRow() {
        when(inventoryRepository.findById(10L)).thenReturn(Optional.empty());

        assertThat(inventoryService.getOnHandOrZero(10L)).isZero();
    }

    private ProductSku sku(Long id) {
        return ProductSku.builder()
                .id(id)
                .sku("SKU-" + id)
                .price(BigDecimal.ONE)
                .build();
    }
}
