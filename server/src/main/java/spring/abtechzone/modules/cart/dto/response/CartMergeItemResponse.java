package spring.abtechzone.modules.cart.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.modules.cart.constant.CartMergeItemStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartMergeItemResponse {

    Long skuId;
    Integer requestedQuantity;
    Integer mergedQuantity;
    CartMergeItemStatus status;
    String reasonCode;
}
