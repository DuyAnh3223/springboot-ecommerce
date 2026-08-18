package spring.abtechzone.modules.order.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import spring.abtechzone.modules.cart.entity.CartItem;
import spring.abtechzone.modules.order.dto.response.OrderResponse;
import spring.abtechzone.modules.order.entity.Order;
import spring.abtechzone.modules.order.entity.OrderItem;
import spring.abtechzone.modules.product.entity.ProductSku;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderResponse toOrderResponse(Order order);

    @Mapping(source = "cartItem.quantity", target = "quantity")
    @Mapping(source = "sku.price", target = "unitPrice")
    @Mapping(source = "sku.product.name", target = "productNameSnapshot")
    @Mapping(source = "sku.sku", target = "skuSnapshot")
    @Mapping(source = "sku.imageUrl", target = "imageUrl")
    @Mapping(source = "sku", target = "sku")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    OrderItem toOrderItem(CartItem cartItem, ProductSku sku);
}
