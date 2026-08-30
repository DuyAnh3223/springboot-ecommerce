package spring.abtechzone.modules.order.dto.request;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import lombok.*;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.modules.order.validator.UniqueSkuIds;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewedCheckoutRequest {

    @NotNull(message = "reviewedCheckout.items is required")
    @NotEmpty(message = "reviewedCheckout.items must not be empty")
    @UniqueSkuIds
    List<@NotNull @Valid ReviewedCheckoutItemRequest> items;

    @NotNull(message = "subtotal is required")
    @DecimalMin(value = "0.0", message = "subtotal must be non-negative")
    BigDecimal subtotal;

    @NotNull(message = "eligibleSubtotal is required")
    @DecimalMin(value = "0.0", message = "eligibleSubtotal must be non-negative")
    BigDecimal eligibleSubtotal;

    @NotNull(message = "shippingFee is required")
    @DecimalMin(value = "0.0", message = "shippingFee must be non-negative")
    BigDecimal shippingFee;

    @NotNull(message = "discountAmount is required")
    @DecimalMin(value = "0.0", message = "discountAmount must be non-negative")
    BigDecimal discountAmount;

    @NotNull(message = "totalAmount is required")
    @DecimalMin(value = "0.0", message = "totalAmount must be non-negative")
    BigDecimal totalAmount;

    @Valid
    ReviewedVoucherRequest voucher;

    @NotNull(message = "canPlaceOrder is required")
    @AssertTrue(message = "canPlaceOrder must be true")
    Boolean canPlaceOrder;
}
