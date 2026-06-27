package spring.abtechzone.modules.cart.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.modules.cart.constant.CartStatus;
import spring.abtechzone.modules.cart.entity.CartItem;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartResponse {
    CartStatus cartStatus;
    List<CartItemRespone> items;
    String userId;
}
