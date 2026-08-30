package spring.abtechzone.modules.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderCancelRequest {
    @NotBlank(message = "reason is required")
    @Size(max = 500, message = "reason is too long")
    String reason;
}
