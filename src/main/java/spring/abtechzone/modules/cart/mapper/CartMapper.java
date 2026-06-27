package spring.abtechzone.modules.cart.mapper;

import org.mapstruct.Mapper;
import spring.abtechzone.modules.cart.dto.request.CartRequest;
import spring.abtechzone.modules.cart.dto.response.CartResponse;
import spring.abtechzone.modules.cart.entity.Cart;

@Mapper(componentModel = "spring")
public interface CartMapper {

    Cart toCart(CartRequest cartRequest);

    CartResponse toCartResponse(Cart cart);

}
