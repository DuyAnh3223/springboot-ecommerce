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
public class OrderDetailResponse {
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
    List<String> allowedTransitions;
    // Address snapshot
    String recipientName;
    String phone;
    String fullAddress;
    // Voucher snapshot
    String voucherCode;
    // Items + history
    List<OrderItemResponse> items;
    List<OrderHistoryResponse> history;
}
