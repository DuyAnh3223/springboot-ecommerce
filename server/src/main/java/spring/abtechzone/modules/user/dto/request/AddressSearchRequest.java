package spring.abtechzone.modules.user.dto.request;

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
public class AddressSearchRequest {
    String search;
    Boolean isDefault;

    @Min(value = 1, message = "ADDRESS_PAGE_INVALID")
    @Builder.Default
    Integer page = 1;

    @Min(value = 1, message = "ADDRESS_SIZE_INVALID")
    @Builder.Default
    Integer size = 20;

    @Builder.Default
    String sortBy = "id";

    @Builder.Default
    String order = "desc";

    public Pageable toPageable() {
        int pageNumber = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 100);

        Sort.Direction direction = "asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;

        return PageRequest.of(pageNumber - 1, pageSize, Sort.by(direction, normalizeSortProperty(sortBy)));
    }

    private static final Set<String> SORT_FIELDS = Set.of("province", "district", "ward", "streetAddress", "country");

    private String normalizeSortProperty(String sortBy) {
        return SORT_FIELDS.contains(sortBy) ? sortBy : "id";
    }
}
