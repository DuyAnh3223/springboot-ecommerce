package spring.abtechzone.modules.catalog.dto.request;

import java.util.Set;

import jakarta.validation.constraints.Min;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AttributeSearchRequest {

    String keyword;

    @Min(value = 1, message = "CATEGORY_PAGE_INVALID")
    @Builder.Default
    Integer page = 1;

    @Min(value = 1, message = "CATEGORY_SIZE_INVALID")
    @Builder.Default
    Integer size = 20;

    @Builder.Default
    String sortBy = "name";

    @Builder.Default
    String order = "asc";

    public Pageable toPageable() {
        int pageNumber = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 100);

        Sort.Direction direction = "asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;

        return PageRequest.of(pageNumber - 1, pageSize, Sort.by(direction, normalizeSortField(sortBy)));
    }

    private static final Set<String> SORT_FIELDS = Set.of("code", "name", "dataType", "unit");

    private String normalizeSortField(String field) {
        return SORT_FIELDS.contains(field) ? field : "name";
    }
}
