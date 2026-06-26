package spring.abtechzone.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import spring.abtechzone.dto.request.VoucherCreateRequest;
import spring.abtechzone.dto.response.VoucherResponse;
import spring.abtechzone.entity.ProductSku;
import spring.abtechzone.entity.Voucher;
import spring.abtechzone.exception.AppException;
import spring.abtechzone.exception.ErrorCode;
import spring.abtechzone.mapper.VoucherMapper;
import spring.abtechzone.repository.ProductSkuRepository;
import spring.abtechzone.repository.VoucherRepository;
import spring.abtechzone.service.validator.VoucherValidator;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VoucherService {

    /*
     *  1 - Generate Voucher Code
     *  2 - Get voucher amount for User
     *  3 - Get all voucher codes for User/Admin
     *  4 - Verify voucher code for User
     *  5 - Delete voucher code for User/Admin
     *  6 - Cancel voucher code for User
     **/

    VoucherRepository voucherRepository;
    ProductSkuRepository productSkuRepository;
    VoucherMapper voucherMapper;
    VoucherValidator voucherValidator;

    @Transactional
    public VoucherResponse create(VoucherCreateRequest request) {
        voucherValidator.validateCreate(request);

        Voucher voucher = voucherMapper.toVoucher(request);
        voucher.setProductSkuIds(resolveProductSkus(request.getProductSkuIds()));

        voucher = voucherRepository.save(voucher);
        return voucherMapper.toVoucherResponse(voucher);
    }

    private List<ProductSku> resolveProductSkus(List<Long> productSkuIds) {
        if (productSkuIds == null || productSkuIds.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Long> uniqueIds = new HashSet<>(productSkuIds);
        List<ProductSku> productSkus = productSkuRepository.findAllById(uniqueIds);

        if (productSkus.size() != uniqueIds.size()) {
            throw new AppException(ErrorCode.SKU_NOT_FOUND);
        }

        return productSkus;
    }
}
