package spring.abtechzone.modules.cart.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartMergeRequest {

    @NotNull(message = "INVALID_KEY")
    UUID mergeId;

    @NotEmpty(message = "INVALID_KEY")
    @Size(max = 100, message = "INVALID_KEY")
    List<@Valid CartMergeItemRequest> items;
}
