package spring.abtechzone.modules.inventory.service;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.catalog.entity.ProductSku;
import spring.abtechzone.modules.catalog.repository.ProductSkuRepository;
import spring.abtechzone.modules.inventory.entity.InventoryReservation;
import spring.abtechzone.modules.inventory.entity.StockMovement;
import spring.abtechzone.modules.inventory.repository.InventoryReservationRepository;
import spring.abtechzone.modules.inventory.repository.StockMovementRepository;
import spring.abtechzone.modules.order.entity.Order;
import spring.abtechzone.modules.user.repository.UserRepository;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InventoryService {

    InventoryReservationRepository inventoryReservationRepository;
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

        InventoryReservation reservation = new InventoryReservation();
        reservation.setSku(sku);
        reservation.setQuantity(quantity);
        reservation.setOrder(order);
        reservation.setStatus("ACTIVE");
        reservation.setExpiresAt(OffsetDateTime.now().plusMinutes(15));
        reservation.setCreatedAt(OffsetDateTime.now());
        inventoryReservationRepository.save(reservation);
    }

    @Transactional
    public void releaseStock(Long reservationId) {
        InventoryReservation reservation = inventoryReservationRepository
                .findById(reservationId)
                .orElseThrow(() -> new AppException(ErrorCode.RESERVATION_NOT_FOUND));

        if (!"ACTIVE".equals(reservation.getStatus())) {
            return;
        }

        reservation.setStatus("RELEASED");
        inventoryReservationRepository.save(reservation);

        ProductSku sku = reservation.getSku();
        sku.setStock(sku.getStock() + reservation.getQuantity());
        productSkuRepository.save(sku);

        StockMovement movement = new StockMovement();
        movement.setSku(sku);
        movement.setChangeQty(reservation.getQuantity());
        movement.setReason("RETURN_IN");
        if (reservation.getOrder() != null) {
            movement.setReferenceId(String.valueOf(reservation.getOrder().getId()));
            if (reservation.getOrder().getUserId() != null) {
                movement.setCreatedBy(userRepository
                        .findById(reservation.getOrder().getUserId())
                        .orElse(null));
            }
        }
        movement.setCreatedAt(OffsetDateTime.now());
        stockMovementRepository.save(movement);
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void reclaimExpiredReservations() {
        log.info("Scanning for expired inventory reservations...");
        List<InventoryReservation> expiredReservations =
                inventoryReservationRepository.findByStatusAndExpiresAtBefore("ACTIVE", OffsetDateTime.now());

        for (InventoryReservation res : expiredReservations) {
            log.info("Reclaiming reservation: id={}, quantity={}", res.getId(), res.getQuantity());
            res.setStatus("EXPIRED");
            inventoryReservationRepository.save(res);

            ProductSku sku = res.getSku();
            sku.setStock(sku.getStock() + res.getQuantity());
            productSkuRepository.save(sku);

            StockMovement movement = new StockMovement();
            movement.setSku(sku);
            movement.setChangeQty(res.getQuantity());
            movement.setReason("RETURN_IN");
            if (res.getOrder() != null) {
                movement.setReferenceId(String.valueOf(res.getOrder().getId()));
                if (res.getOrder().getUserId() != null) {
                    movement.setCreatedBy(
                            userRepository.findById(res.getOrder().getUserId()).orElse(null));
                }
            }
            movement.setCreatedAt(OffsetDateTime.now());
            stockMovementRepository.save(movement);
        }
    }
}
