package spring.abtechzone.modules.inventory.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.*;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.modules.inventory.constant.StockAdjustmentOperation;
import spring.abtechzone.modules.inventory.constant.StockMovementReason;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StockAdjustmentRequest {
    @NotNull(message = "INVENTORY_ADJUSTMENT_INVALID")
    StockAdjustmentOperation operation;

    @NotNull(message = "PRODUCT_STOCK_INVALID")
    @Positive(message = "PRODUCT_STOCK_INVALID")
    Integer quantity;

    @NotNull(message = "INVENTORY_ADJUSTMENT_INVALID")
    StockMovementReason reason;
}
