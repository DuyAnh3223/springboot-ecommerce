package spring.abtechzone.modules.voucher.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.product.dto.response.ProductSkuResponse;
import spring.abtechzone.modules.product.entity.ProductSku;
import spring.abtechzone.modules.product.mapper.ProductSkuMapper;
import spring.abtechzone.modules.product.repository.ProductSkuRepository;
import spring.abtechzone.modules.voucher.constant.VoucherApplyScope;
import spring.abtechzone.modules.voucher.constant.VoucherType;
import spring.abtechzone.modules.voucher.dto.request.VoucherCreateRequest;
import spring.abtechzone.modules.voucher.dto.request.VoucherDiscountRequest;
import spring.abtechzone.modules.voucher.dto.request.VoucherSearchRequest;
import spring.abtechzone.modules.voucher.dto.request.VoucherUpdateRequest;
import spring.abtechzone.modules.voucher.dto.response.VoucherDiscountResponse;
import spring.abtechzone.modules.voucher.dto.response.VoucherResponse;
import spring.abtechzone.modules.voucher.entity.Voucher;
import spring.abtechzone.modules.voucher.mapper.VoucherMapper;
import spring.abtechzone.modules.voucher.repository.VoucherRepository;
import spring.abtechzone.modules.voucher.repository.specification.VoucherSpecifications;
import spring.abtechzone.modules.voucher.validator.VoucherValidator;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VoucherService {
    VoucherRepository voucherRepository;
    ProductSkuRepository productSkuRepository;
    VoucherMapper voucherMapper;
    VoucherValidator voucherValidator;
    ProductSkuMapper productSkuMapper;

    @Transactional
    public VoucherResponse create(VoucherCreateRequest request) {
        if (voucherRepository.existsByCode(request.getCode())) {
            throw new AppException(ErrorCode.VOUCHER_EXISTED);
        }

        voucherValidator.validateCreate(request);

        Voucher voucher = voucherMapper.toVoucher(request);

        if (request.getApplyScope() == VoucherApplyScope.ALL) {
            voucher.setProductSkus(new HashSet<>());
        } else {
            var productSkus = productSkuRepository.findAllById(request.getProductSkuIds());
            if (productSkus.size() != request.getProductSkuIds().size()) {
                throw new AppException(ErrorCode.SKU_NOT_FOUND);
            }
            voucher.setProductSkus(new HashSet<>(productSkus));
        }

        voucher = voucherRepository.save(voucher);
        return voucherMapper.toVoucherResponse(voucher);
    }

    private Voucher findVoucherByCode(String code) {
        return voucherRepository.findByCode(code).orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND));
    }

    public Page<VoucherResponse> getVouchers(VoucherSearchRequest request) {
        Specification<Voucher> spec = Specification.where(VoucherSpecifications.hasActive(request.getActive()))
                .and(VoucherSpecifications.hasStatus(request.getStatus()))
                .and(VoucherSpecifications.fetchProductSkus());

        Page<Voucher> vouchersPage = voucherRepository.findAll(spec, request.toPageable());

        return vouchersPage.map(voucherMapper::toVoucherResponse);
    }

    public VoucherResponse getVoucher(String code) {
        return voucherMapper.toVoucherResponse(findVoucherByCode(code));
    }

    @Transactional
    public VoucherResponse update(String code, VoucherUpdateRequest request) {
        Voucher voucher = findVoucherByCode(code);

        if (!voucher.getCode().equals(request.getCode()) && voucherRepository.existsByCode(request.getCode())) {
            throw new AppException(ErrorCode.VOUCHER_EXISTED);
        }

        voucherValidator.validateUpdate(request);
        voucherMapper.updateVoucher(voucher, request);

        if (request.getApplyScope() == VoucherApplyScope.ALL) {
            voucher.setProductSkus(new HashSet<>());
        } else {
            var productSkus = productSkuRepository.findAllById(request.getProductSkuIds());
            if (productSkus.size() != request.getProductSkuIds().size()) {
                throw new AppException(ErrorCode.SKU_NOT_FOUND);
            }
            voucher.setProductSkus(new HashSet<>(productSkus));
        }

        return voucherMapper.toVoucherResponse(voucherRepository.save(voucher));
    }

    public void delete(String code) {
        Voucher voucher = findVoucherByCode(code);
        voucher.setActive(false);
        voucherRepository.save(voucher);
    }

    @Transactional(readOnly = true)
    public List<ProductSkuResponse> getAllProductSkusByVoucherCode(String code) {
        Voucher voucher = findVoucherByCode(code);

        List<ProductSku> skus;
        if (voucher.getApplyScope() == VoucherApplyScope.ALL) {
            skus = productSkuRepository.findAll();
        } else {
            skus = List.copyOf(voucher.getProductSkus());
        }

        return skus.stream().map(productSkuMapper::toProductSkuResponse).toList();
    }

    public VoucherDiscountResponse calculateDiscount(VoucherDiscountRequest request) {
        Voucher voucher = findVoucherByCode(request.getCode());

        voucherValidator.validateVoucher(voucher, request.getTotalOrder());

        BigDecimal totalOrder = request.getTotalOrder() != null ? request.getTotalOrder() : BigDecimal.ZERO;

        BigDecimal discountAmount = getDiscount(voucher, totalOrder);

        BigDecimal totalPrice = totalOrder.subtract(discountAmount);

        return VoucherDiscountResponse.builder()
                .discountAmount(discountAmount)
                .totalOrder(totalOrder)
                .totalPrice(totalPrice)
                .build();
    }

    private static BigDecimal getDiscount(Voucher voucher, BigDecimal totalOrder) {
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (voucher.getType() == VoucherType.FIXED_AMOUNT) {
            discountAmount = voucher.getValue() != null ? voucher.getValue() : BigDecimal.ZERO;
        } else if (voucher.getType() == VoucherType.PERCENTAGE) {
            BigDecimal percentage = voucher.getValue() != null ? voucher.getValue() : BigDecimal.ZERO;
            discountAmount = totalOrder.multiply(percentage).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        if (discountAmount.compareTo(totalOrder) > 0) {
            discountAmount = totalOrder;
        }
        return discountAmount;
    }
}
