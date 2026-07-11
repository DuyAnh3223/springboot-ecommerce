package spring.abtechzone.modules.cart.dto.response;

import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.modules.cart.constant.CartStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartResponse {
    Long cartId;
    CartStatus status;
    List<CartItemResponse> items;
    String userId;
}
