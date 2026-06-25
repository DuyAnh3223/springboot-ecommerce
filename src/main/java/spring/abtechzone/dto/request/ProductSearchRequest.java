package spring.abtechzone.dto.request;

import java.math.BigDecimal;

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
    Integer page = 1;
    Integer size = 20;
    String sortBy = "createdAt";
    String order = "desc";

    public Pageable toPageable() {
        int pageIndex = Math.max(0, page - 1); // Tránh lỗi page âm
        Sort.Direction direction = "asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;

        // Xử lý sort đặc biệt cho quan hệ bảng (ví dụ: bảng skus)
        String sortProperty = "price".equalsIgnoreCase(sortBy) ? "skus.price" : sortBy;

        return PageRequest.of(pageIndex, size, Sort.by(direction, sortProperty));
    }
}
