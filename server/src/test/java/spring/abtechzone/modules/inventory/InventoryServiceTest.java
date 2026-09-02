package spring.abtechzone.modules.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.inventory.entity.StockMovement;
import spring.abtechzone.modules.inventory.repository.InventoryRepository;
import spring.abtechzone.modules.inventory.repository.StockMovementRepository;
import spring.abtechzone.modules.inventory.service.InventoryService;
import spring.abtechzone.modules.order.entity.Order;
import spring.abtechzone.modules.product.entity.ProductSku;
import spring.abtechzone.modules.product.repository.ProductSkuRepository;
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
        assertThat(captor.getValue().getReason()).isEqualTo("SALE_OUT");
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
        assertThat(captor.getValue().getReason()).isEqualTo("ORDER_CANCEL_RETURN");
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
    void createForSku_rejectsUnknownSkuAndPersistsKnownSkuBalance() {
        ProductSku sku = sku(10L);
        when(productSkuRepository.existsById(10L)).thenReturn(true);
        when(inventoryRepository.existsById(10L)).thenReturn(false);
        when(inventoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(inventoryService.createForSku(sku, 4).getOnHand()).isEqualTo(4);

        when(productSkuRepository.existsById(99L)).thenReturn(false);
        assertThatThrownBy(() -> inventoryService.createForSku(sku(99L), 4))
                .isInstanceOf(AppException.class)
                .hasMessageContaining(ErrorCode.SKU_NOT_FOUND.getMessage());
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
