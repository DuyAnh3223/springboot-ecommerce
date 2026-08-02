package spring.abtechzone.modules.catalog.dto.request;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.Min;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CatalogSearchRequest {
    String search;
    Long brandId;
    BigDecimal minPrice;
    BigDecimal maxPrice;
    Boolean inStock;

    @Builder.Default
    Map<String, List<String>> attributes = new HashMap<>();

    @Min(value = 1, message = "PRODUCT_PAGE_INVALID")
    @Builder.Default
    Integer page = 1;

    @Min(value = 1, message = "PRODUCT_SIZE_INVALID")
    @Builder.Default
    Integer size = 20;

    @Builder.Default
    String sortBy = "name";

    @Builder.Default
    String order = "asc";
}
