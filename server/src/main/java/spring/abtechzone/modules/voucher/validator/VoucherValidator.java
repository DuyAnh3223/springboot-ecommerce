package spring.abtechzone.modules.voucher.validator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;

import org.springframework.stereotype.Component;

import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.voucher.constant.VoucherApplyScope;
import spring.abtechzone.modules.voucher.constant.VoucherRedemptionStatus;
import spring.abtechzone.modules.voucher.constant.VoucherType;
import spring.abtechzone.modules.voucher.dto.request.VoucherCreateRequest;
import spring.abtechzone.modules.voucher.dto.request.VoucherUpdateRequest;
import spring.abtechzone.modules.voucher.entity.Voucher;
import spring.abtechzone.modules.voucher.repository.VoucherRedemptionRepository;

@Component
public class VoucherValidator {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private final VoucherRedemptionRepository voucherRedemptionRepository;

    public VoucherValidator(VoucherRedemptionRepository voucherRedemptionRepository) {
        this.voucherRedemptionRepository = voucherRedemptionRepository;
    }

    public void validateCreate(VoucherCreateRequest request) {
        validateDates(request.getStartDate(), request.getEndDate());
        validateFutureStartDate(request.getStartDate());
        validateValue(request.getType(), request.getValue(), request.getMaxDiscountAmount());
        validateApplyScope(request.getApplyScope(), request.getProductSkuIds());
    }

    public void validateUpdate(VoucherUpdateRequest request) {
        validateDates(request.getStartDate(), request.getEndDate());
        validateValue(request.getType(), request.getValue(), request.getMaxDiscountAmount());
        validateApplyScope(request.getApplyScope(), request.getProductSkuIds());
    }

    public void validateForCheckout(Voucher voucher, User user, BigDecimal fullSubtotal, BigDecimal eligibleSubtotal) {
        validateActive(voucher);
        validateExpiry(voucher);
        validateUsageLimit(voucher);
        validatePerUserLimit(voucher, user);
        validateMinOrderValue(voucher, fullSubtotal);
        validateSpecificScopeEligibility(voucher, eligibleSubtotal);
    }

    private void validateActive(Voucher voucher) {
        if (!Boolean.TRUE.equals(voucher.getIsActive())) {
            throw new AppException(ErrorCode.VOUCHER_EXPIRED);
        }
    }

    private void validateExpiry(Voucher voucher) {
        LocalDateTime now = LocalDateTime.now();
        if (voucher.getStartDate() != null && now.isBefore(voucher.getStartDate())) {
            throw new AppException(ErrorCode.VOUCHER_EXPIRED);
        }
        if (voucher.getEndDate() != null && now.isAfter(voucher.getEndDate())) {
            throw new AppException(ErrorCode.VOUCHER_EXPIRED);
        }
    }

    private void validateUsageLimit(Voucher voucher) {
        int used = voucher.getUsedCount() != null ? voucher.getUsedCount() : 0;
        if (voucher.getMaxUses() != null && used >= voucher.getMaxUses()) {
            throw new AppException(ErrorCode.VOUCHER_ARE_OUT);
        }
    }

    private void validatePerUserLimit(Voucher voucher, User user) {
        if (voucher.getMaxPerUser() == null || user == null) {
            return;
        }
        long userUsageCount = voucherRedemptionRepository.countByVoucherIdAndUserIdAndStatus(
                voucher.getId(), user.getId(), VoucherRedemptionStatus.REDEEMED);
        if (userUsageCount >= voucher.getMaxPerUser()) {
            throw new AppException(ErrorCode.VOUCHER_PER_USER_LIMIT_REACHED);
        }
    }

    private void validateMinOrderValue(Voucher voucher, BigDecimal totalOrder) {
        BigDecimal orderVal = totalOrder != null ? totalOrder : BigDecimal.ZERO;
        if (voucher.getMinOrderValue() != null && orderVal.compareTo(voucher.getMinOrderValue()) < 0) {
            throw new AppException(ErrorCode.VOUCHER_MIN_ORDER_VALUE_INVALID);
        }
    }

    private void validateSpecificScopeEligibility(Voucher voucher, BigDecimal eligibleSubtotal) {
        if (voucher.getApplyScope() == VoucherApplyScope.SPECIFIC) {
            BigDecimal eligible = eligibleSubtotal != null ? eligibleSubtotal : BigDecimal.ZERO;
            if (eligible.compareTo(BigDecimal.ZERO) <= 0) {
                throw new AppException(ErrorCode.VOUCHER_SCOPE_INVALID);
            }
        }
    }

    private void validateDates(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            return;
        }

        if (!endDate.isAfter(startDate)) {
            throw new AppException(ErrorCode.VOUCHER_DATE_INVALID);
        }
    }

    private void validateFutureStartDate(LocalDateTime startDate) {
        if (startDate != null && startDate.isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.VOUCHER_DATE_INVALID);
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

    public void validateValue(VoucherType type, BigDecimal value, BigDecimal maxDiscountAmount) {
        if (type == null || value == null) return;

        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.VOUCHER_VALUE_INVALID);
        }

        if (type == VoucherType.FIXED_AMOUNT && maxDiscountAmount != null) {
            throw new AppException(ErrorCode.VOUCHER_MAX_DISCOUNT_INVALID);
        }

        if (type == VoucherType.PERCENTAGE) {
            if (value.compareTo(ONE_HUNDRED) >= 0) {
                throw new AppException(ErrorCode.VOUCHER_VALUE_INVALID);
            }

            if (maxDiscountAmount != null && maxDiscountAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new AppException(ErrorCode.VOUCHER_MAX_DISCOUNT_INVALID);
            }
        }
    }
}
