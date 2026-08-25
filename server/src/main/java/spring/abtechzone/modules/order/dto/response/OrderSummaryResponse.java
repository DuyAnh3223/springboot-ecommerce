package spring.abtechzone.modules.order.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderSummaryResponse {
    Long id;
    String orderCode;
    String status;
    String paymentMethod;
    String paymentStatus;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
    BigDecimal subtotalAmount;
    BigDecimal shippingFee;
    BigDecimal discountAmount;
    BigDecimal totalAmount;
    int itemCount;
    List<String> allowedTransitions;
    OrderItemResponse previewItem;
}
