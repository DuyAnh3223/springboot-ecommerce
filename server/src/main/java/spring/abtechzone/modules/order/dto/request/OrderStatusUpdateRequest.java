package spring.abtechzone.modules.order.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;
import spring.abtechzone.modules.order.constant.OrderStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderStatusUpdateRequest {
    @NotNull(message = "status is required")
    OrderStatus status;

    @Size(max = 500, message = "note is too long")
    String note;
}
