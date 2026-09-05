package spring.abtechzone.modules.inventory.dto.response;

import java.time.OffsetDateTime;

import lombok.*;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.modules.inventory.constant.StockMovementReason;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StockMovementResponse {
    Long movementId;
    Long skuId;
    String skuCode;
    Integer changeQty;
    StockMovementReason reason;
    String referenceId;
    String createdBy;
    OffsetDateTime createdAt;
}
