package spring.abtechzone.modules.order.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import spring.abtechzone.modules.order.dto.response.OrderDetailResponse;
import spring.abtechzone.modules.order.dto.response.OrderHistoryResponse;
import spring.abtechzone.modules.order.dto.response.OrderItemResponse;
import spring.abtechzone.modules.order.dto.response.OrderResponse;
import spring.abtechzone.modules.order.dto.response.OrderSummaryResponse;
import spring.abtechzone.modules.order.entity.Order;
import spring.abtechzone.modules.order.entity.OrderItem;
import spring.abtechzone.modules.order.entity.OrderStatusHistory;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderResponse toOrderResponse(Order order);

    @Mapping(target = "itemCount", expression = "java(order.getItems() == null ? 0 : order.getItems().size())")
    @Mapping(target = "allowedTransitions", ignore = true)
    @Mapping(target = "previewItem", ignore = true)
    OrderSummaryResponse toOrderSummaryResponse(Order order);

    @Mapping(
            target = "lineTotal",
            expression =
                    "java(item.getUnitPrice() == null ? null : item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())))")
    @Mapping(target = "skuCode", source = "skuSnapshot")
    @Mapping(target = "skuId", source = "skuId")
    @Mapping(target = "productName", source = "productNameSnapshot")
    OrderItemResponse toOrderItemResponse(OrderItem item);

    @Mapping(target = "status", source = "status")
    @Mapping(target = "createdAt", source = "createdAt")
    OrderHistoryResponse toOrderHistoryResponse(OrderStatusHistory history);

    @Mapping(target = "allowedTransitions", ignore = true)
    @Mapping(target = "history", ignore = true)
    @Mapping(target = "items", ignore = true)
    OrderDetailResponse toOrderDetailResponse(Order order);

    List<OrderItemResponse> toOrderItemResponses(List<OrderItem> items);

    List<OrderHistoryResponse> toOrderHistoryResponses(List<OrderStatusHistory> history);
}
