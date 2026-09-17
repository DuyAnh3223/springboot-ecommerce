package spring.abtechzone.modules.shipment.dto;

import java.util.UUID;

/** Address and resolved route returned in a checkout review. */
public record ShippingAddressSnapshot(
        UUID addressId,
        String recipientName,
        String phone,
        String province,
        String district,
        String ward,
        String street,
        Integer ghnProvinceId,
        Integer ghnDistrictId,
        String ghnWardCode) {}
