package spring.abtechzone.modules.voucher.dto.request;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VoucherSearchRequest {
    Boolean active;
    String status;

    @Builder.Default
    Integer page = 1;

    @Builder.Default
    Integer size = 20;

    @Builder.Default
    String sortBy = "id";

    @Builder.Default
    String order = "desc";

    public Pageable toPageable() {
        int pageNumber = page == null ? 1 : page;
        int pageSize = size == null ? 20 : size;
        String requestedSortBy = sortBy == null || sortBy.isBlank() ? "id" : sortBy;
        String requestedOrder = order == null ? "desc" : order;

        int pageIndex = pageNumber - 1;
        if (pageIndex < 0) pageIndex = 0;
        Sort.Direction direction = "asc".equalsIgnoreCase(requestedOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;

        return PageRequest.of(pageIndex, pageSize, Sort.by(direction, requestedSortBy));
    }
}
