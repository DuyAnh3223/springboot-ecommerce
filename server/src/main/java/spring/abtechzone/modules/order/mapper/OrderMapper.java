package spring.abtechzone.modules.order.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import spring.abtechzone.modules.cart.entity.CartItem;
import spring.abtechzone.modules.catalog.entity.ProductSku;
import spring.abtechzone.modules.order.dto.response.CheckoutItemResponse;
import spring.abtechzone.modules.order.dto.response.OrderResponse;
import spring.abtechzone.modules.order.entity.Order;
import spring.abtechzone.modules.order.entity.OrderItem;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(source = "productSku.id", target = "productSkuId")
    @Mapping(source = "productSku.product.name", target = "productName")
    @Mapping(source = "productSku.sku", target = "skuCode")
    @Mapping(source = "productSku.imageUrl", target = "imageUrl")
    @Mapping(source = "productSku.price", target = "unitPrice")
    @Mapping(
            target = "totalPrice",
            expression =
                    "java(cartItem.getProductSku().getPrice().multiply(java.math.BigDecimal.valueOf(cartItem.getQuantity())))")
    CheckoutItemResponse toCheckoutItemResponse(CartItem cartItem);

    @Mapping(source = "id", target = "orderId")
    @Mapping(source = "status", target = "orderStatus")
    @Mapping(source = "subtotalAmount", target = "subtotal")
    @Mapping(source = "discountAmount", target = "totalDiscount")
    @Mapping(source = "totalAmount", target = "totalCheckout")
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
