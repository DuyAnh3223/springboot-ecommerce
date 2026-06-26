package spring.abtechzone.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import spring.abtechzone.dto.request.VoucherCreateRequest;
import spring.abtechzone.dto.request.VoucherUpdateRequest;
import spring.abtechzone.dto.response.VoucherResponse;
import spring.abtechzone.entity.Voucher;

@Mapper(componentModel = "spring")
public interface VoucherMapper {

    @Mapping(target = "productSkuIds", ignore = true)
    Voucher toVoucher(VoucherCreateRequest request);

    @Mapping(source = "productSkuIds", target = "productSkus")
    VoucherResponse toVoucherResponse(Voucher voucher);

    @Mapping(target = "productSkuIds", ignore = true)
    void updateVoucher(@MappingTarget Voucher voucher, VoucherUpdateRequest request);
}
