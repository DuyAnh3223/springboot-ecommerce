package spring.abtechzone.modules.order.dto.response;

import java.time.OffsetDateTime;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderHistoryResponse {
    String fromStatus;
    String toStatus;
    String status;
    String actorType;
    String note;
    OffsetDateTime createdAt;
}
