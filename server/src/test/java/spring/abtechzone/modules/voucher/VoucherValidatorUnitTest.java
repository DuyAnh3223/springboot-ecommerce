package spring.abtechzone.modules.voucher;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.user.entity.User;
import spring.abtechzone.modules.voucher.constant.VoucherApplyScope;
import spring.abtechzone.modules.voucher.constant.VoucherType;
import spring.abtechzone.modules.voucher.dto.request.VoucherCreateRequest;
import spring.abtechzone.modules.voucher.dto.request.VoucherUpdateRequest;
import spring.abtechzone.modules.voucher.entity.Voucher;
import spring.abtechzone.modules.voucher.repository.VoucherRepository;
import spring.abtechzone.modules.voucher.validator.VoucherValidator;

class VoucherValidatorUnitTest {

    private VoucherRepository voucherRepository;
    private VoucherValidator voucherValidator;
    private final User testUser = User.builder()
            .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
            .username("testuser")
            .build();

    @BeforeEach
    void setUp() {
        voucherRepository = mock(VoucherRepository.class);
        voucherValidator = new VoucherValidator(voucherRepository);
    }

    @Nested
    @DisplayName("validateCreate Tests")
    class ValidateCreateTests {

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

            AppException exception100 =
                    assertThrows(AppException.class, () -> voucherValidator.validateCreate(request100));
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

            AppException exception150 =
                    assertThrows(AppException.class, () -> voucherValidator.validateCreate(request150));
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
    }

    @Nested
    @DisplayName("validateUpdate Tests")
    class ValidateUpdateTests {

        @Test
        void validateUpdate_validRequest_noException() {
            VoucherUpdateRequest request = VoucherUpdateRequest.builder()
                    .name("Updated Discount")
                    .type(VoucherType.PERCENTAGE)
                    .value(BigDecimal.valueOf(20))
                    .startDate(LocalDateTime.now().plusDays(1))
                    .endDate(LocalDateTime.now().plusDays(3))
                    .applyScope(VoucherApplyScope.ALL)
                    .productSkuIds(Collections.emptySet())
                    .build();

            assertDoesNotThrow(() -> voucherValidator.validateUpdate(request));
        }
    }

    @Nested
    @DisplayName("validateForCheckout Tests")
    class ValidateForCheckoutTests {

        @Test
        @DisplayName("Valid voucher with ALL scope succeeds")
        void validateForCheckout_validAllScope_succeeds() {
            Voucher voucher = Voucher.builder()
                    .id(1L)
                    .code("ALL10")
                    .type(VoucherType.PERCENTAGE)
                    .value(BigDecimal.valueOf(10))
                    .applyScope(VoucherApplyScope.ALL)
                    .isActive(true)
                    .build();

            assertDoesNotThrow(() -> voucherValidator.validateForCheckout(
                    voucher, testUser, BigDecimal.valueOf(500000), BigDecimal.valueOf(500000)));
        }

        @Test
        @DisplayName("Valid voucher with SPECIFIC scope and eligibleSubtotal > 0 succeeds")
        void validateForCheckout_validSpecificScope_succeeds() {
            Voucher voucher = Voucher.builder()
                    .id(2L)
                    .code("SPECIFIC10")
                    .type(VoucherType.PERCENTAGE)
                    .value(BigDecimal.valueOf(10))
                    .applyScope(VoucherApplyScope.SPECIFIC)
                    .isActive(true)
                    .build();

            assertDoesNotThrow(() -> voucherValidator.validateForCheckout(
                    voucher, testUser, BigDecimal.valueOf(1000000), BigDecimal.valueOf(400000)));
        }

        @Test
        @DisplayName("SPECIFIC scope with 0 eligible subtotal throws VOUCHER_SCOPE_INVALID")
        void validateForCheckout_specificScopeNoEligible_throwsScopeInvalid() {
            Voucher voucher = Voucher.builder()
                    .id(3L)
                    .code("SPECIFIC_NO_MATCH")
                    .type(VoucherType.PERCENTAGE)
                    .value(BigDecimal.valueOf(10))
                    .applyScope(VoucherApplyScope.SPECIFIC)
                    .isActive(true)
                    .build();

            AppException ex = assertThrows(
                    AppException.class,
                    () -> voucherValidator.validateForCheckout(
                            voucher, testUser, BigDecimal.valueOf(1000000), BigDecimal.ZERO));
            assertEquals(ErrorCode.VOUCHER_SCOPE_INVALID, ex.getErrorCode());
        }

        @Test
        @DisplayName("fullSubtotal below minOrderValue throws VOUCHER_MIN_ORDER_VALUE_INVALID")
        void validateForCheckout_belowMinOrderValue_throwsMinOrderInvalid() {
            Voucher voucher = Voucher.builder()
                    .id(4L)
                    .code("MIN_ORDER")
                    .type(VoucherType.FIXED_AMOUNT)
                    .value(BigDecimal.valueOf(50000))
                    .minOrderValue(BigDecimal.valueOf(500000))
                    .applyScope(VoucherApplyScope.ALL)
                    .isActive(true)
                    .build();

            AppException ex = assertThrows(
                    AppException.class,
                    () -> voucherValidator.validateForCheckout(
                            voucher, testUser, BigDecimal.valueOf(400000), BigDecimal.valueOf(400000)));
            assertEquals(ErrorCode.VOUCHER_MIN_ORDER_VALUE_INVALID, ex.getErrorCode());
        }

        @Test
        @DisplayName("minOrderValue is evaluated on fullSubtotal even when eligibleSubtotal is lower")
        void validateForCheckout_minOrderEvaluatedOnFullSubtotal_succeeds() {
            Voucher voucher = Voucher.builder()
                    .id(5L)
                    .code("MIN_ORDER_SPECIFIC")
                    .type(VoucherType.FIXED_AMOUNT)
                    .value(BigDecimal.valueOf(50000))
                    .minOrderValue(BigDecimal.valueOf(500000))
                    .applyScope(VoucherApplyScope.SPECIFIC)
                    .isActive(true)
                    .build();

            // fullSubtotal = 600k (>= 500k min), eligibleSubtotal = 300k (< 500k min) -> valid!
            assertDoesNotThrow(() -> voucherValidator.validateForCheckout(
                    voucher, testUser, BigDecimal.valueOf(600000), BigDecimal.valueOf(300000)));
        }

        @Test
        @DisplayName("User reached maxPerUser limit throws VOUCHER_PER_USER_LIMIT_REACHED")
        void validateForCheckout_maxPerUserReached_throwsPerUserLimitReached() {
            Voucher voucher = Voucher.builder()
                    .id(6L)
                    .code("PER_USER")
                    .type(VoucherType.FIXED_AMOUNT)
                    .value(BigDecimal.valueOf(50000))
                    .maxPerUser(1)
                    .applyScope(VoucherApplyScope.ALL)
                    .isActive(true)
                    .build();

            when(voucherRepository.countUsageByVoucherIdAndUserId(eq(6L), eq(testUser.getId())))
                    .thenReturn(1L);

            AppException ex = assertThrows(
                    AppException.class,
                    () -> voucherValidator.validateForCheckout(
                            voucher, testUser, BigDecimal.valueOf(500000), BigDecimal.valueOf(500000)));
            assertEquals(ErrorCode.VOUCHER_PER_USER_LIMIT_REACHED, ex.getErrorCode());
        }

        @Test
        @DisplayName("Inactive voucher throws VOUCHER_EXPIRED")
        void validateForCheckout_inactive_throwsExpired() {
            Voucher voucher = Voucher.builder()
                    .id(7L)
                    .code("INACTIVE")
                    .type(VoucherType.FIXED_AMOUNT)
                    .value(BigDecimal.valueOf(50000))
                    .applyScope(VoucherApplyScope.ALL)
                    .isActive(false)
                    .build();

            AppException ex = assertThrows(
                    AppException.class,
                    () -> voucherValidator.validateForCheckout(
                            voucher, testUser, BigDecimal.valueOf(500000), BigDecimal.valueOf(500000)));
            assertEquals(ErrorCode.VOUCHER_EXPIRED, ex.getErrorCode());
        }

        @Test
        @DisplayName("Expired voucher throws VOUCHER_EXPIRED")
        void validateForCheckout_expired_throwsExpired() {
            Voucher voucher = Voucher.builder()
                    .id(8L)
                    .code("EXPIRED")
                    .type(VoucherType.FIXED_AMOUNT)
                    .value(BigDecimal.valueOf(50000))
                    .endDate(LocalDateTime.now().minusDays(1))
                    .applyScope(VoucherApplyScope.ALL)
                    .isActive(true)
                    .build();

            AppException ex = assertThrows(
                    AppException.class,
                    () -> voucherValidator.validateForCheckout(
                            voucher, testUser, BigDecimal.valueOf(500000), BigDecimal.valueOf(500000)));
            assertEquals(ErrorCode.VOUCHER_EXPIRED, ex.getErrorCode());
        }

        @Test
        @DisplayName("Exhausted maxUses throws VOUCHER_ARE_OUT")
        void validateForCheckout_maxUsesExhausted_throwsAreOut() {
            Voucher voucher = Voucher.builder()
                    .id(9L)
                    .code("MAX_OUT")
                    .type(VoucherType.FIXED_AMOUNT)
                    .value(BigDecimal.valueOf(50000))
                    .maxUses(10)
                    .usedCount(10)
                    .applyScope(VoucherApplyScope.ALL)
                    .isActive(true)
                    .build();

            AppException ex = assertThrows(
                    AppException.class,
                    () -> voucherValidator.validateForCheckout(
                            voucher, testUser, BigDecimal.valueOf(500000), BigDecimal.valueOf(500000)));
            assertEquals(ErrorCode.VOUCHER_ARE_OUT, ex.getErrorCode());
        }
    }
}
