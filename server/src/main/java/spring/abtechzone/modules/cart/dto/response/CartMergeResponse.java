package spring.abtechzone.modules.cart.dto.response;

import java.util.List;
import java.util.UUID;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartMergeResponse {

    UUID mergeId;
    List<CartMergeItemResponse> items;
}
