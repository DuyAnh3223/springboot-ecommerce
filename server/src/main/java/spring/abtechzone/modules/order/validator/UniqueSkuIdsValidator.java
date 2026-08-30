package spring.abtechzone.modules.order.validator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import spring.abtechzone.modules.order.dto.request.ReviewedCheckoutItemRequest;

public class UniqueSkuIdsValidator implements ConstraintValidator<UniqueSkuIds, List<ReviewedCheckoutItemRequest>> {

    @Override
    public boolean isValid(List<ReviewedCheckoutItemRequest> items, ConstraintValidatorContext context) {
        if (items == null) {
            return true; // @NotNull handles null
        }
        Set<Long> seen = new HashSet<>();
        for (ReviewedCheckoutItemRequest item : items) {
            if (item != null && item.getSkuId() != null && !seen.add(item.getSkuId())) {
                return false;
            }
        }
        return true;
    }
}
