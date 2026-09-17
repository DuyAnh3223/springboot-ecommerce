package spring.abtechzone.modules.shipment.dto;

/** Canonical GHN route resolved from the provider catalogue. */
public record ResolvedShippingRoute(
        int provinceId, String province, int districtId, String district, String wardCode, String ward) {}
