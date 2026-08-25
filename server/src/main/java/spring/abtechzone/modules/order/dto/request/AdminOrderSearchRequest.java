package spring.abtechzone.modules.order.dto.request;

import java.time.Instant;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminOrderSearchRequest {
    String search;

    String status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    Instant fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    Instant toDate;

    @Builder.Default
    int page = 0;

    @Builder.Default
    int size = 20;
}
