package spring.abtechzone.modules.shipment.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.shipment.client.GhnFeeClient;
import spring.abtechzone.modules.shipment.config.GhnShippingProperties;
import spring.abtechzone.modules.shipment.dto.GhnFeeRequest;
import spring.abtechzone.modules.shipment.dto.GhnFeeResult;
import spring.abtechzone.modules.shipment.dto.ShippingAddressData;

@Service
@RequiredArgsConstructor
public class ShippingFeeService {
    private final GhnFeeClient ghnFeeClient;
    private final GhnShippingProperties properties;

    public GhnFeeResult calculate(ShippingAddressData address) {
        if (address == null
                || address.districtId() == null
                || address.districtId() <= 0
                || address.wardCode() == null
                || address.wardCode().isBlank()
                || properties.getFromDistrictId() <= 0
                || properties.getFromWardCode() == null
                || properties.getFromWardCode().isBlank()
                || properties.getFixedWeightGram() <= 0) {
            throw new AppException(ErrorCode.SHIPPING_ADDRESS_INVALID);
        }
        int total = ghnFeeClient.calculateFee(new GhnFeeRequest(
                properties.getFromDistrictId(),
                properties.getFromWardCode(),
                address.districtId(),
                address.wardCode(),
                properties.getServiceTypeId(),
                properties.getFixedWeightGram()));
        return new GhnFeeResult(BigDecimal.valueOf(total), OffsetDateTime.now());
    }
}
