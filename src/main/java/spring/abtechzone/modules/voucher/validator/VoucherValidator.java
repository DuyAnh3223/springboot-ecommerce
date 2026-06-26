package spring.abtechzone.modules.voucher.validator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;

import org.springframework.stereotype.Component;

import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.voucher.constant.VoucherApplyScope;
import spring.abtechzone.modules.voucher.constant.VoucherType;
import spring.abtechzone.modules.voucher.dto.request.VoucherCreateRequest;
import spring.abtechzone.modules.voucher.dto.request.VoucherUpdateRequest;

@Component
public class VoucherValidator {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    public void validateCreate(VoucherCreateRequest request) {
        validateDates(request.getStartDate(), request.getEndDate());
        validateValue(request.getType(), request.getValue());
        validateApplyScope(request.getApplyScope(), request.getProductSkuIds());
    }

    private void validateDates(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            return;
        }

        if (!endDate.isAfter(startDate)) {
            throw new AppException(ErrorCode.VOUCHER_DATE_INVALID);
        }
    }

    private void validateValue(VoucherType type, BigDecimal value) {
        if (type == null || value == null) {
            return;
        }

        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.VOUCHER_VALUE_INVALID);
        }

        if (type == VoucherType.PERCENTAGE && value.compareTo(ONE_HUNDRED) >= 0) {
            throw new AppException(ErrorCode.VOUCHER_VALUE_INVALID);
        }
    }

    private void validateApplyScope(VoucherApplyScope applyScope, Collection<Long> productSkuIds) {
        if (applyScope == null) {
            return;
        }

        boolean hasProductSkus = productSkuIds != null && !productSkuIds.isEmpty();

        if (applyScope == VoucherApplyScope.ALL && hasProductSkus) {
            throw new AppException(ErrorCode.VOUCHER_SCOPE_INVALID);
        }

        if (applyScope == VoucherApplyScope.SPECIFIC && !hasProductSkus) {
            throw new AppException(ErrorCode.VOUCHER_SCOPE_INVALID);
        }
    }

    public void validateUpdate(VoucherUpdateRequest request) {
        validateDates(request.getStartDate(), request.getEndDate());
        validateValue(request.getType(), request.getValue());
        validateApplyScope(request.getApplyScope(), request.getProductSkuIds());
    }
}
