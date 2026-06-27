package spring.abtechzone.modules.cart.dto.request;

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
public class CartRequest {

    CartStatus cartStatus;
    List<CartItem> items;
    String userId;
}
