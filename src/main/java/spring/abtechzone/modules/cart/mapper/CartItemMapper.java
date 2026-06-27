package spring.abtechzone.modules.cart.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import spring.abtechzone.modules.cart.dto.request.CartItemRequest;
import spring.abtechzone.modules.cart.dto.response.CartItemRespone;
import spring.abtechzone.modules.cart.entity.CartItem;

@Mapper(componentModel = "spring")
public interface CartItemMapper {

    CartItem toCartItem(CartItemRequest cartItemRequest);

    CartItemRespone toCartItemRespone(CartItem cartItem);

    void update(@MappingTarget CartItem cartItem, CartItemRequest cartItemRequest);
}
