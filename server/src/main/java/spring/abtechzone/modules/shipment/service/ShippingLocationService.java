package spring.abtechzone.modules.shipment.service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import spring.abtechzone.common.exception.AppException;
import spring.abtechzone.common.exception.ErrorCode;
import spring.abtechzone.modules.shipment.client.GhnLocationClient;
import spring.abtechzone.modules.shipment.config.GhnShippingProperties;
import spring.abtechzone.modules.shipment.dto.GhnDistrictData;
import spring.abtechzone.modules.shipment.dto.GhnProvinceData;
import spring.abtechzone.modules.shipment.dto.GhnWardData;
import spring.abtechzone.modules.shipment.dto.ResolvedShippingRoute;
import spring.abtechzone.modules.shipment.dto.ShippingDistrictResponse;
import spring.abtechzone.modules.shipment.dto.ShippingProvinceResponse;
import spring.abtechzone.modules.shipment.dto.ShippingWardResponse;

@Service
@RequiredArgsConstructor
public class ShippingLocationService {
    private final GhnLocationClient client;
    private final GhnShippingProperties properties;
    private volatile CacheEntry<List<ShippingProvinceResponse>> provinces;
    private final ConcurrentHashMap<Integer, CacheEntry<List<ShippingDistrictResponse>>> districts =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, CacheEntry<List<ShippingWardResponse>>> wards = new ConcurrentHashMap<>();

    public List<ShippingProvinceResponse> getProvinces() {
        CacheEntry<List<ShippingProvinceResponse>> cached = provinces;
        if (isFresh(cached)) {
            return cached.value();
        }
        List<ShippingProvinceResponse> loaded =
                client.getProvinces().stream().map(this::toProvinceResponse).toList();
        provinces = new CacheEntry<>(loaded, expiresAt());
        return loaded;
    }

    public List<ShippingDistrictResponse> getDistricts(int provinceId) {
        requirePositive(provinceId);
        CacheEntry<List<ShippingDistrictResponse>> cached = districts.get(provinceId);
        if (isFresh(cached)) {
            return cached.value();
        }
        List<ShippingDistrictResponse> loaded = client.getDistricts(provinceId).stream()
                .map(data -> toDistrictResponse(data, provinceId))
                .toList();
        districts.put(provinceId, new CacheEntry<>(loaded, expiresAt()));
        return loaded;
    }

    public List<ShippingWardResponse> getWards(int districtId) {
        requirePositive(districtId);
        CacheEntry<List<ShippingWardResponse>> cached = wards.get(districtId);
        if (isFresh(cached)) {
            return cached.value();
        }
        List<ShippingWardResponse> loaded = client.getWards(districtId).stream()
                .map(data -> toWardResponse(data, districtId))
                .toList();
        wards.put(districtId, new CacheEntry<>(loaded, expiresAt()));
        return loaded;
    }

    public ResolvedShippingRoute resolveRoute(int provinceId, int districtId, String wardCode) {
        requirePositive(provinceId);
        requirePositive(districtId);
        if (wardCode == null || wardCode.isBlank()) {
            throw new AppException(ErrorCode.SHIPPING_ADDRESS_INVALID);
        }
        ShippingProvinceResponse province = getProvinces().stream()
                .filter(item -> item.id() == provinceId)
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.SHIPPING_ADDRESS_INVALID));
        ShippingDistrictResponse district = getDistricts(provinceId).stream()
                .filter(item -> item.id() == districtId && item.deliverySupported())
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.SHIPPING_ADDRESS_INVALID));
        ShippingWardResponse ward = getWards(districtId).stream()
                .filter(item -> item.code().equals(wardCode.trim()) && item.deliverySupported())
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.SHIPPING_ADDRESS_INVALID));
        return new ResolvedShippingRoute(
                province.id(), province.name(), district.id(), district.name(), ward.code(), ward.name());
    }

    private ShippingProvinceResponse toProvinceResponse(GhnProvinceData data) {
        return new ShippingProvinceResponse(data.id(), data.name());
    }

    private ShippingDistrictResponse toDistrictResponse(GhnDistrictData data, int provinceId) {
        return new ShippingDistrictResponse(data.id(), provinceId, data.name(), supportsDelivery(data.supportType()));
    }

    private ShippingWardResponse toWardResponse(GhnWardData data, int districtId) {
        return new ShippingWardResponse(data.code(), districtId, data.name(), supportsDelivery(data.supportType()));
    }

    private boolean supportsDelivery(Integer supportType) {
        return supportType != null && (supportType == 2 || supportType == 3);
    }

    private void requirePositive(int value) {
        if (value <= 0) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }
    }

    private boolean isFresh(CacheEntry<?> entry) {
        return entry != null && entry.expiresAt().isAfter(Instant.now());
    }

    private Instant expiresAt() {
        long ttl = Math.max(1, properties.getLocationCacheTtlSeconds());
        return Instant.now().plusSeconds(ttl);
    }

    private record CacheEntry<T>(T value, Instant expiresAt) {}
}
