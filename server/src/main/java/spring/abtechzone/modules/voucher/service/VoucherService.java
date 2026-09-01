package spring.abtechzone.modules.voucher.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import spring.abtechzone.modules.inventory.service.InventoryService;
import spring.abtechzone.modules.product.dto.response.ProductSkuResponse;
import spring.abtechzone.modules.product.entity.ProductSku;
import spring.abtechzone.modules.product.repository.ProductSkuRepository;
import spring.abtechzone.modules.product.service.ProductSkuService;
import spring.abtechzone.modules.voucher.constant.VoucherApplyScope;
import spring.abtechzone.modules.voucher.constant.VoucherType;
import spring.abtechzone.modules.voucher.dto.request.VoucherCreateRequest;
import spring.abtechzone.modules.voucher.dto.request.VoucherSearchRequest;
import spring.abtechzone.modules.voucher.dto.request.VoucherUpdateRequest;
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
    ProductSkuService productSkuService;
    InventoryService inventoryService;

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
        return toVoucherResponse(voucher);
    }

    private Voucher findVoucherByCode(String code) {
        return voucherRepository.findByCode(code).orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND));
    }

    public Page<VoucherResponse> getVouchers(VoucherSearchRequest request) {
        Specification<Voucher> spec = Specification.where(VoucherSpecifications.hasActive(request.getActive()))
                .and(VoucherSpecifications.hasStatus(request.getStatus()))
                .and(VoucherSpecifications.hasCodeOrNameLike(request.getSearch()))
                .and(VoucherSpecifications.fetchProductSkus());

        Page<Voucher> vouchersPage = voucherRepository.findAll(spec, request.toPageable());

        return vouchersPage.map(this::toVoucherResponse);
    }

    public VoucherResponse getVoucher(String code) {
        return toVoucherResponse(findVoucherByCode(code));
    }

    @Transactional
    public VoucherResponse update(String code, VoucherUpdateRequest request) {
        Voucher voucher = findVoucherByCode(code);

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

        return toVoucherResponse(voucherRepository.save(voucher));
    }

    public void delete(String code) {
        Voucher voucher = findVoucherByCode(code);
        voucher.setIsActive(false);
        voucherRepository.save(voucher);
    }

    @Transactional
    public VoucherResponse reactivate(String code) {
        Voucher voucher = findVoucherByCode(code);
        voucher.setIsActive(true);
        return toVoucherResponse(voucherRepository.save(voucher));
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

        return productSkuService.toSkuResponseList(skus);
    }

    private VoucherResponse toVoucherResponse(Voucher voucher) {
        VoucherResponse response = voucherMapper.toVoucherResponse(voucher);
        if (response == null
                || response.getProductSkus() == null
                || response.getProductSkus().isEmpty()) {
            return response;
        }
        Map<Long, Integer> onHandBySkuId = inventoryService.getOnHandBySkuIds(response.getProductSkus().stream()
                .map(ProductSkuResponse::getId)
                .toList());
        response.getProductSkus().forEach(sku -> sku.setStock(onHandBySkuId.getOrDefault(sku.getId(), 0)));
        return response;
    }

    public BigDecimal calculateEligibleSubtotal(
            Voucher voucher, Map<Long, BigDecimal> skuSubtotals, BigDecimal fullSubtotal) {
        if (voucher == null || voucher.getApplyScope() == null || voucher.getApplyScope() == VoucherApplyScope.ALL) {
            return fullSubtotal != null ? fullSubtotal : BigDecimal.ZERO;
        }

        if (skuSubtotals == null || skuSubtotals.isEmpty() || voucher.getProductSkus() == null) {
            return BigDecimal.ZERO;
        }

        Set<Long> eligibleSkuIds =
                voucher.getProductSkus().stream().map(ProductSku::getId).collect(Collectors.toSet());

        BigDecimal eligibleSubtotal = BigDecimal.ZERO;
        for (Map.Entry<Long, BigDecimal> entry : skuSubtotals.entrySet()) {
            if (entry.getKey() != null && eligibleSkuIds.contains(entry.getKey()) && entry.getValue() != null) {
                eligibleSubtotal = eligibleSubtotal.add(entry.getValue());
            }
        }
        return eligibleSubtotal;
    }

    public BigDecimal getDiscount(Voucher voucher, BigDecimal eligibleSubtotal) {
        if (voucher == null || eligibleSubtotal == null || eligibleSubtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal voucherValue = voucher.getValue() != null ? voucher.getValue() : BigDecimal.ZERO;

        if (voucher.getType() == VoucherType.FIXED_AMOUNT) {
            discountAmount = voucherValue.min(eligibleSubtotal);
        } else if (voucher.getType() == VoucherType.PERCENTAGE) {
            discountAmount =
                    eligibleSubtotal.multiply(voucherValue).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            if (voucher.getMaxDiscountAmount() != null
                    && voucher.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                discountAmount = discountAmount.min(voucher.getMaxDiscountAmount());
            }
        }

        // Discount Amount must not higher than eligibleSubtotal (applies to both FIXED_AMOUNT and PERCENTAGE)
        if (discountAmount.compareTo(eligibleSubtotal) > 0) {
            discountAmount = eligibleSubtotal;
        }
        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            discountAmount = BigDecimal.ZERO;
        }
        return discountAmount.setScale(2, RoundingMode.HALF_UP);
    }
}
