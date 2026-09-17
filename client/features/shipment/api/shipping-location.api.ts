import type {
  ShippingDistrict,
  ShippingProvince,
  ShippingWard,
} from "../shipping-location.type";

// Browser API boundary used by both the Address and Checkout pickers.
async function getJson<T>(url: string, signal?: AbortSignal): Promise<T> {
  const response = await fetch(url, { signal, cache: "no-store" });
  if (!response.ok) {
    throw new Error("Không thể tải dữ liệu địa chỉ GHN.");
  }
  return response.json() as Promise<T>;
}

export function getShippingProvinces(signal?: AbortSignal): Promise<ShippingProvince[]> {
  return getJson<ShippingProvince[]>("/api/shipping/locations/provinces", signal);
}

export function getShippingDistricts(
  provinceId: number,
  signal?: AbortSignal,
): Promise<ShippingDistrict[]> {
  return getJson<ShippingDistrict[]>(
    `/api/shipping/locations/districts?provinceId=${encodeURIComponent(provinceId)}`,
    signal,
  );
}

export function getShippingWards(
  districtId: number,
  signal?: AbortSignal,
): Promise<ShippingWard[]> {
  return getJson<ShippingWard[]>(
    `/api/shipping/locations/wards?districtId=${encodeURIComponent(districtId)}`,
    signal,
  );
}
