package spring.abtechzone.modules.inventory.service;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.auth.service.AuthService;
import spring.abtechzone.modules.inventory.constant.StockAdjustmentOperation;
import spring.abtechzone.modules.inventory.constant.StockMovementReason;
import spring.abtechzone.modules.inventory.dto.request.StockAdjustmentRequest;
import spring.abtechzone.modules.inventory.dto.request.StockMovementSearchRequest;
import spring.abtechzone.modules.inventory.dto.response.StockAdjustmentResponse;
import spring.abtechzone.modules.inventory.dto.response.StockMovementResponse;
import spring.abtechzone.modules.inventory.entity.Inventory;
import spring.abtechzone.modules.inventory.entity.StockMovement;
import spring.abtechzone.modules.inventory.mapper.StockMovementMapper;
import spring.abtechzone.modules.inventory.repository.InventoryRepository;
import spring.abtechzone.modules.inventory.repository.StockMovementRepository;
import spring.abtechzone.modules.order.entity.Order;
import spring.abtechzone.modules.product.entity.ProductSku;
import spring.abtechzone.modules.product.repository.ProductSkuRepository;
import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.user.repository.UserRepository;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InventoryService {

    StockMovementRepository stockMovementRepository;
    InventoryRepository inventoryRepository;
    ProductSkuRepository productSkuRepository;
    UserRepository userRepository;
    AuthService authService;
    StockMovementMapper stockMovementMapper;

    private static final int MAX_ON_HAND = Integer.MAX_VALUE;

    @Transactional(readOnly = true)
    public OptionalInt findOnHand(Long skuId) {
        if (skuId == null) {
            return OptionalInt.empty();
        }
        return inventoryRepository
                .findById(skuId)
                .map(inventory -> OptionalInt.of(inventory.getOnHand()))
                .orElseGet(() -> {
                    // Missing rows are invariant violations, never a reason to use a catalog fallback.
                    log.warn("Missing inventory row for skuId={}", skuId);
                    return OptionalInt.empty();
                });
    }

    @Transactional(readOnly = true)
    public int getOnHandOrZero(Long skuId) {
        OptionalInt onHand = findOnHand(skuId);
        if (onHand.isEmpty()) {
            return 0;
        }
        return onHand.getAsInt();
    }

    @Transactional(readOnly = true)
    public Map<Long, Integer> getOnHandBySkuIds(Collection<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Integer> result = new HashMap<>();
        for (InventoryRepository.SkuOnHandProjection row : inventoryRepository.findOnHandBySkuIds(skuIds)) {
            result.put(row.getSkuId(), row.getOnHand());
        }
        for (Long skuId : skuIds) {
            if (skuId != null && !result.containsKey(skuId)) {
                log.warn("Missing inventory row in bulk read for skuId={}", skuId);
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Map<Long, Integer> getTotalOnHandByProductIds(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        logMissingInventoryRows(inventoryRepository.findActiveSkuIdsByProductIds(productIds));
        Map<Long, Integer> result = new HashMap<>();
        for (InventoryRepository.ProductOnHandProjection row : inventoryRepository.sumOnHandByProductIds(productIds)) {
            long total = row.getTotalOnHand() == null ? 0L : row.getTotalOnHand();
            result.put(row.getProductId(), (int) Math.min(Integer.MAX_VALUE, Math.max(0L, total)));
        }
        return result;
    }

    private void logMissingInventoryRows(Collection<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return;
        }
        Map<Long, Integer> existing = getOnHandBySkuIdsWithoutDiagnostics(skuIds);
        for (Long skuId : skuIds) {
            if (skuId != null && !existing.containsKey(skuId)) {
                log.warn("Missing inventory row while summing product stock for skuId={}", skuId);
            }
        }
    }

    private Map<Long, Integer> getOnHandBySkuIdsWithoutDiagnostics(Collection<Long> skuIds) {
        Map<Long, Integer> result = new HashMap<>();
        for (InventoryRepository.SkuOnHandProjection row : inventoryRepository.findOnHandBySkuIds(skuIds)) {
            result.put(row.getSkuId(), row.getOnHand());
        }
        return result;
    }

    @Transactional
    public Inventory createForSku(ProductSku sku, Integer onHand) {
        validateOnHand(onHand);
        if (sku == null || sku.getId() == null) {
            throw new AppException(ErrorCode.SYSTEM_ERROR);
        }
        if (!productSkuRepository.existsById(sku.getId())) {
            throw new AppException(ErrorCode.SKU_NOT_FOUND);
        }
        if (inventoryRepository.existsById(sku.getId())) {
            throw new AppException(ErrorCode.SYSTEM_ERROR);
        }
        Inventory inventory = inventoryRepository.save(Inventory.builder()
                .skuId(sku.getId())
                .productSku(sku)
                .onHand(onHand)
                .build());
        if (onHand > 0) {
            writeMovement(
                    sku,
                    onHand,
                    StockMovementReason.OPENING_BALANCE,
                    String.valueOf(sku.getId()),
                    resolveCurrentActorOrNull());
        }
        return inventory;
    }

    @Transactional
    public void setOnHand(Long skuId, Integer onHand) {
        validateOnHand(onHand);
        if (skuId == null) {
            throw new AppException(ErrorCode.SYSTEM_ERROR);
        }
        Inventory inventory = inventoryRepository.findByIdForUpdate(skuId).orElseGet(() -> {
            log.warn("Cannot set inventory; row missing for skuId={}", skuId);
            throw new AppException(ErrorCode.SYSTEM_ERROR);
        });
        int delta = onHand - inventory.getOnHand();
        if (delta == 0) {
            return;
        }
        inventory.setOnHand(onHand);
        inventoryRepository.save(inventory);
        StockMovementReason reason =
                delta > 0 ? StockMovementReason.MANUAL_ADJUSTMENT_IN : StockMovementReason.MANUAL_ADJUSTMENT_OUT;
        writeMovement(inventory.getProductSku(), delta, reason, String.valueOf(skuId), resolveCurrentActorOrNull());
    }

    @Transactional
    public void decreaseStock(ProductSku sku, int quantity, Order order) {
        validateQuantity(quantity);
        if (sku == null || sku.getId() == null) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
        }
        int rowsUpdated = inventoryRepository.decreaseOnHand(sku.getId(), quantity);

        if (rowsUpdated == 0) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
        }

        String referenceId = null;
        User actor = null;
        if (order != null) {
            referenceId = String.valueOf(order.getId());
            if (order.getUserId() != null) {
                actor = userRepository.findById(order.getUserId()).orElse(null);
            }
        }
        writeMovement(sku, -quantity, StockMovementReason.SALE_OUT, referenceId, actor);
    }

    @Transactional
    public void increaseStock(Long skuId, int quantity, Order order, ProductSku sku) {
        validateQuantity(quantity);
        if (skuId == null || sku == null) {
            throw new AppException(ErrorCode.SYSTEM_ERROR);
        }
        int rowsUpdated = inventoryRepository.increaseOnHand(skuId, quantity, MAX_ON_HAND);
        if (rowsUpdated != 1) {
            throw new AppException(ErrorCode.SYSTEM_ERROR);
        }

        String referenceId = order == null ? null : String.valueOf(order.getId());
        writeMovement(sku, quantity, StockMovementReason.ORDER_CANCEL_RETURN, referenceId, null);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public StockAdjustmentResponse adjustStock(Long skuId, StockAdjustmentRequest request) {
        validateAdjustmentRequest(request);
        if (skuId == null) {
            throw new AppException(ErrorCode.INVENTORY_NOT_FOUND);
        }

        Inventory inventory = inventoryRepository
                .findByIdForUpdate(skuId)
                .orElseThrow(() -> new AppException(ErrorCode.INVENTORY_NOT_FOUND));
        int signedDelta;
        if (request.getOperation() == StockAdjustmentOperation.INCREASE) {
            long nextOnHand = (long) inventory.getOnHand() + request.getQuantity();
            if (nextOnHand > MAX_ON_HAND) {
                throw new AppException(ErrorCode.INVENTORY_STOCK_OVERFLOW);
            }
            signedDelta = request.getQuantity();
        } else {
            if (inventory.getOnHand() < request.getQuantity()) {
                throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
            }
            signedDelta = -request.getQuantity();
        }

        inventory.setOnHand(inventory.getOnHand() + signedDelta);
        inventoryRepository.save(inventory);
        StockMovement movement =
                writeMovement(inventory.getProductSku(), signedDelta, request.getReason(), null, resolveCurrentActor());
        return stockMovementMapper.toAdjustmentResponse(skuId, inventory.getOnHand(), movement);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Page<StockMovementResponse> getStockMovements(StockMovementSearchRequest request) {
        int page = request == null || request.getPage() == null ? 0 : Math.max(0, request.getPage());
        int size = request == null || request.getSize() == null ? 20 : Math.min(100, Math.max(1, request.getSize()));
        Long skuId = request == null ? null : request.getSkuId();
        StockMovementReason reason = request == null ? null : request.getReason();
        PageRequest pageable = PageRequest.of(page, size);
        return stockMovementRepository
                .searchHistory(skuId, reason == null ? null : reason.name(), pageable)
                .map(stockMovementMapper::toResponse);
    }

    private StockMovement writeMovement(
            ProductSku sku, int changeQty, StockMovementReason reason, String referenceId, User actor) {
        if (sku == null || changeQty == 0 || reason == null) {
            throw new AppException(ErrorCode.SYSTEM_ERROR);
        }
        return stockMovementRepository.save(
                stockMovementMapper.toEntity(sku, changeQty, reason, referenceId, actor, OffsetDateTime.now()));
    }

    private User resolveCurrentActorOrNull() {
        if (!authService.isAuthenticated()) {
            return null;
        }
        return userRepository
                .findByUsername(authService.getCurrentUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private User resolveCurrentActor() {
        return userRepository
                .findByUsername(authService.getCurrentUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private void validateAdjustmentRequest(StockAdjustmentRequest request) {
        if (request == null
                || request.getOperation() == null
                || request.getReason() == null
                || request.getQuantity() == null
                || request.getQuantity() <= 0) {
            throw new AppException(ErrorCode.INVENTORY_ADJUSTMENT_INVALID);
        }
        boolean validReason =
                switch (request.getOperation()) {
                    case INCREASE ->
                        request.getReason() == StockMovementReason.PURCHASE_IN
                                || request.getReason() == StockMovementReason.MANUAL_ADJUSTMENT_IN;
                    case DECREASE ->
                        request.getReason() == StockMovementReason.DAMAGE_OUT
                                || request.getReason() == StockMovementReason.MANUAL_ADJUSTMENT_OUT;
                };
        if (!validReason) {
            throw new AppException(ErrorCode.INVENTORY_ADJUSTMENT_INVALID);
        }
    }

    private void validateOnHand(Integer onHand) {
        if (onHand == null || onHand < 0) {
            throw new AppException(ErrorCode.PRODUCT_STOCK_INVALID);
        }
    }

    private void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new AppException(ErrorCode.SYSTEM_ERROR);
        }
    }
}
