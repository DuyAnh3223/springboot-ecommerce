package spring.abtechzone.modules.cart.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartMergeItemRequest {

    @NotNull(message = "INVALID_KEY")
    @Positive(message = "INVALID_KEY")
    Long skuId;

    @NotNull(message = "INVALID_KEY")
    @Positive(message = "INVALID_KEY")
    Integer quantity;
}
