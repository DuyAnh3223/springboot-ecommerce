package spring.abtechzone.modules.inventory.service;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.inventory.entity.StockMovement;
import spring.abtechzone.modules.inventory.repository.StockMovementRepository;
import spring.abtechzone.modules.order.entity.Order;
import spring.abtechzone.modules.product.entity.ProductSku;
import spring.abtechzone.modules.product.repository.ProductSkuRepository;
import spring.abtechzone.modules.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InventoryService {

    StockMovementRepository stockMovementRepository;
    ProductSkuRepository productSkuRepository;
    UserRepository userRepository;

    @Transactional
    public void reserveStock(ProductSku sku, int quantity, Order order) {
        int rowsUpdated = productSkuRepository.decreaseStock(sku.getId(), quantity);

        if (rowsUpdated == 0) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
        }

        StockMovement movement = new StockMovement();
        movement.setSku(sku);
        movement.setChangeQty(-quantity);
        movement.setReason("SALE_OUT");
        if (order != null) {
            movement.setReferenceId(String.valueOf(order.getId()));
            if (order.getUserId() != null) {
                movement.setCreatedBy(userRepository.findById(order.getUserId()).orElse(null));
            }
        }
        movement.setCreatedAt(OffsetDateTime.now());
        stockMovementRepository.save(movement);
    }
}
