package spring.abtechzone.voucher;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.voucher.constant.VoucherApplyScope;
import spring.abtechzone.modules.voucher.constant.VoucherType;
import spring.abtechzone.modules.voucher.dto.request.VoucherCreateRequest;
import spring.abtechzone.modules.voucher.dto.request.VoucherUpdateRequest;
import spring.abtechzone.modules.voucher.validator.VoucherValidator;

class VoucherValidatorUnitTest {

    private VoucherValidator voucherValidator;

    @BeforeEach
    void setUp() {
        voucherValidator = new VoucherValidator();
    }

    @Test
    void validateCreate_validRequest_noException() {
        VoucherCreateRequest request = VoucherCreateRequest.builder()
                .name("Discount 10%")
                .type(VoucherType.PERCENTAGE)
                .value(BigDecimal.valueOf(10))
                .code("DISCOUNT10")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .applyScope(VoucherApplyScope.ALL)
                .productSkuIds(Collections.emptySet())
                .build();

        assertDoesNotThrow(() -> voucherValidator.validateCreate(request));
    }

    @Test
    void validateCreate_invalidDates_throwsAppException() {
        VoucherCreateRequest request = VoucherCreateRequest.builder()
                .name("Discount 10%")
                .type(VoucherType.PERCENTAGE)
                .value(BigDecimal.valueOf(10))
                .code("DISCOUNT10")
                .startDate(LocalDateTime.now().plusDays(2))
                .endDate(LocalDateTime.now().plusDays(1))
                .applyScope(VoucherApplyScope.ALL)
                .productSkuIds(Collections.emptySet())
                .build();

        AppException exception = assertThrows(AppException.class, () -> voucherValidator.validateCreate(request));
        assertEquals(ErrorCode.VOUCHER_DATE_INVALID, exception.getErrorCode());
    }

    @Test
    void validateCreate_zeroOrNegativeValue_throwsAppException() {
        VoucherCreateRequest requestZero = VoucherCreateRequest.builder()
                .name("Discount Zero")
                .type(VoucherType.FIXED_AMOUNT)
                .value(BigDecimal.ZERO)
                .code("ZERO")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .applyScope(VoucherApplyScope.ALL)
                .build();

        AppException exceptionZero =
                assertThrows(AppException.class, () -> voucherValidator.validateCreate(requestZero));
        assertEquals(ErrorCode.VOUCHER_VALUE_INVALID, exceptionZero.getErrorCode());

        VoucherCreateRequest requestNegative = VoucherCreateRequest.builder()
                .name("Discount Negative")
                .type(VoucherType.FIXED_AMOUNT)
                .value(BigDecimal.valueOf(-5))
                .code("NEGATIVE")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .applyScope(VoucherApplyScope.ALL)
                .build();

        AppException exceptionNegative =
                assertThrows(AppException.class, () -> voucherValidator.validateCreate(requestNegative));
        assertEquals(ErrorCode.VOUCHER_VALUE_INVALID, exceptionNegative.getErrorCode());
    }

    @Test
    void validateCreate_percentageValueGreaterThanOrEqual100_throwsAppException() {
        VoucherCreateRequest request100 = VoucherCreateRequest.builder()
                .name("Discount 100%")
                .type(VoucherType.PERCENTAGE)
                .value(BigDecimal.valueOf(100))
                .code("100PERCENT")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .applyScope(VoucherApplyScope.ALL)
                .build();

        AppException exception100 = assertThrows(AppException.class, () -> voucherValidator.validateCreate(request100));
        assertEquals(ErrorCode.VOUCHER_VALUE_INVALID, exception100.getErrorCode());

        VoucherCreateRequest request150 = VoucherCreateRequest.builder()
                .name("Discount 150%")
                .type(VoucherType.PERCENTAGE)
                .value(BigDecimal.valueOf(150))
                .code("150PERCENT")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .applyScope(VoucherApplyScope.ALL)
                .build();

        AppException exception150 = assertThrows(AppException.class, () -> voucherValidator.validateCreate(request150));
        assertEquals(ErrorCode.VOUCHER_VALUE_INVALID, exception150.getErrorCode());
    }

    @Test
    void validateCreate_scopeAllWithSkuIds_throwsAppException() {
        VoucherCreateRequest request = VoucherCreateRequest.builder()
                .name("Scope All With Skus")
                .type(VoucherType.FIXED_AMOUNT)
                .value(BigDecimal.valueOf(50))
                .code("ALL_WITH_SKUS")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .applyScope(VoucherApplyScope.ALL)
                .productSkuIds(Set.of(1L, 2L))
                .build();

        AppException exception = assertThrows(AppException.class, () -> voucherValidator.validateCreate(request));
        assertEquals(ErrorCode.VOUCHER_SCOPE_INVALID, exception.getErrorCode());
    }

    @Test
    void validateCreate_scopeSpecificWithoutSkuIds_throwsAppException() {
        VoucherCreateRequest requestNull = VoucherCreateRequest.builder()
                .name("Scope Specific Null Skus")
                .type(VoucherType.FIXED_AMOUNT)
                .value(BigDecimal.valueOf(50))
                .code("SPECIFIC_NULL")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .applyScope(VoucherApplyScope.SPECIFIC)
                .productSkuIds(null)
                .build();

        AppException exceptionNull =
                assertThrows(AppException.class, () -> voucherValidator.validateCreate(requestNull));
        assertEquals(ErrorCode.VOUCHER_SCOPE_INVALID, exceptionNull.getErrorCode());

        VoucherCreateRequest requestEmpty = VoucherCreateRequest.builder()
                .name("Scope Specific Empty Skus")
                .type(VoucherType.FIXED_AMOUNT)
                .value(BigDecimal.valueOf(50))
                .code("SPECIFIC_EMPTY")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .applyScope(VoucherApplyScope.SPECIFIC)
                .productSkuIds(Collections.emptySet())
                .build();

        AppException exceptionEmpty =
                assertThrows(AppException.class, () -> voucherValidator.validateCreate(requestEmpty));
        assertEquals(ErrorCode.VOUCHER_SCOPE_INVALID, exceptionEmpty.getErrorCode());
    }

    @Test
    void validateUpdate_validRequest_noException() {
        VoucherUpdateRequest request = VoucherUpdateRequest.builder()
                .name("Updated Discount")
                .type(VoucherType.PERCENTAGE)
                .value(BigDecimal.valueOf(20))
                .code("UPDATE20")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(3))
                .applyScope(VoucherApplyScope.ALL)
                .productSkuIds(Collections.emptySet())
                .build();

        assertDoesNotThrow(() -> voucherValidator.validateUpdate(request));
    }
}
