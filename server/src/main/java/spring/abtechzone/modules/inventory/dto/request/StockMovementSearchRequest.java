package spring.abtechzone.modules.inventory.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import lombok.*;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.modules.inventory.constant.StockMovementReason;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StockMovementSearchRequest {
    Long skuId;
    StockMovementReason reason;

    @Builder.Default
    @Min(value = 0, message = "PRODUCT_PAGE_INVALID")
    Integer page = 0;

    @Builder.Default
    @Min(value = 1, message = "PRODUCT_SIZE_INVALID")
    @Max(value = 100, message = "PRODUCT_SIZE_INVALID")
    Integer size = 20;
}
