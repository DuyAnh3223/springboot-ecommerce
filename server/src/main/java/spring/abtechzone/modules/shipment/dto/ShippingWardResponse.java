package spring.abtechzone.modules.shipment.dto;

public record ShippingWardResponse(String code, int districtId, String name, boolean deliverySupported) {}
