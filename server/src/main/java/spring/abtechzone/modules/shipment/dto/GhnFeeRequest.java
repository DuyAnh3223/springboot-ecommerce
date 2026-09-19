package spring.abtechzone.modules.shipment.dto;

public record GhnFeeRequest(
        int fromDistrictId, String fromWardCode, int toDistrictId, String toWardCode, int serviceTypeId, int weight) {}
