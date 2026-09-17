package spring.abtechzone.modules.shipment.dto;

import java.util.UUID;

/** Normalized recipient and GHN route data used only for a fee quote. */
public record ShippingAddressData(
        UUID addressId,
        String recipientName,
        String phone,
        String province,
        String district,
        String ward,
        String street,
        Integer provinceId,
        Integer districtId,
        String wardCode) {}
