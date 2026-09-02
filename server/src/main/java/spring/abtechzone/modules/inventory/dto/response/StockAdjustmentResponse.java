package spring.abtechzone.modules.inventory.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StockAdjustmentResponse {
    Long skuId;
    Integer onHand;
    StockMovementResponse movement;
}
