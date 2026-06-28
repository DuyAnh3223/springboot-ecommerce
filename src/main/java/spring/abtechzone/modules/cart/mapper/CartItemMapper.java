package spring.abtechzone.modules.cart.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import spring.abtechzone.modules.cart.dto.response.CartItemResponse;
import spring.abtechzone.modules.cart.entity.CartItem;

@Mapper(componentModel = "spring")
public interface CartItemMapper {

    @Mapping(source = "productSku.id", target = "productSkuId")
    @Mapping(source = "productSku.sku", target = "skuCode")
    @Mapping(source = "productSku.product.name", target = "productName")
    @Mapping(source = "productSku.imageUrl", target = "imageUrl")
    CartItemResponse toCartItemResponse(CartItem cartItem);
}
