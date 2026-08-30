package spring.abtechzone.modules.order.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import spring.abtechzone.modules.order.dto.response.OrderItemResponse;
import spring.abtechzone.modules.order.entity.OrderItem;
import spring.abtechzone.modules.product.entity.ProductSku;

class OrderMapperTest {

    private final OrderMapper mapper = Mappers.getMapper(OrderMapper.class);

    @Test
    @DisplayName("OrderItem maps to the response with skuId, code, name, quantity, unit price and line total")
    void orderItem_toResponse() {
        ProductSku sku = ProductSku.builder()
                .id(100L)
                .sku("IPHONE-15-256GB")
                .price(BigDecimal.valueOf(1000000))
                .build();
        OrderItem item = OrderItem.builder()
                .sku(sku)
                .skuId(100L)
                .productNameSnapshot("iPhone 15 Pro Max")
                .skuSnapshot("ORDERED-IPHONE-15-256GB")
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(1000000))
                .imageUrl("https://example.com/img.png")
                .build();

        OrderItemResponse response = mapper.toOrderItemResponse(item);

        assertThat(response.getSkuId()).isEqualTo(100L);
        assertThat(response.getSkuCode()).isEqualTo("ORDERED-IPHONE-15-256GB");
        assertThat(response.getProductName()).isEqualTo("iPhone 15 Pro Max");
        assertThat(response.getQuantity()).isEqualTo(2);
        assertThat(response.getUnitPrice()).isEqualByComparingTo(BigDecimal.valueOf(1000000));
        assertThat(response.getLineTotal()).isEqualByComparingTo(BigDecimal.valueOf(2000000));
        assertThat(response.getImageUrl()).isEqualTo("https://example.com/img.png");
    }
}
