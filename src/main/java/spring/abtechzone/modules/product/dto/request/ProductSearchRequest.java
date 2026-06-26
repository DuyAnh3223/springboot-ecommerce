package spring.abtechzone.modules.product.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;

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
public class ProductSearchRequest {
    String search;
    Long categoryId;
    BigDecimal minPrice;
    BigDecimal maxPrice;

    @Min(value = 1, message = "PRODUCT_PAGE_INVALID")
    @Builder.Default
    Integer page = 1;

    @Min(value = 1, message = "PRODUCT_SIZE_INVALID")
    @Builder.Default
    Integer size = 20;

    @Builder.Default
    String sortBy = "name";

    @Builder.Default
    String order = "desc";

    public Pageable toPageable() {
        int pageNumber = page == null ? 1 : page;
        int pageSize = size == null ? 20 : size;
        String requestedSortBy = sortBy == null || sortBy.isBlank() ? "name" : sortBy.toLowerCase();
        String requestedOrder = order == null ? "desc" : order;

        int pageIndex = pageNumber - 1;
        Sort.Direction direction = "asc".equalsIgnoreCase(requestedOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortProperty = normalizeSortProperty(requestedSortBy);

        return PageRequest.of(pageIndex, pageSize, Sort.by(direction, sortProperty));
    }

    private String normalizeSortProperty(String requestedSortBy) {
        return switch (requestedSortBy) {
            case "price" -> "skus.price";
            case "name", "slug", "rating" -> requestedSortBy;
            default -> "name";
        };
    }
}
