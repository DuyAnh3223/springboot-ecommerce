package spring.abtechzone.modules.order.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.*;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewedCheckoutItemRequest {

    @NotNull(message = "skuId is required")
    @Positive(message = "skuId must be positive")
    Long skuId;

    @NotNull(message = "quantity is required")
    @Positive(message = "quantity must be positive")
    Integer quantity;

    @NotNull(message = "unitPrice is required")
    @DecimalMin(value = "0.0", message = "unitPrice must be non-negative")
    BigDecimal unitPrice;

    @NotNull(message = "lineTotal is required")
    @DecimalMin(value = "0.0", message = "lineTotal must be non-negative")
    BigDecimal lineTotal;
}
