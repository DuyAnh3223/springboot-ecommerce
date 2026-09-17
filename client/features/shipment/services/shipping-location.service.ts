import "server-only";
import { api } from "@/shared/http/api";
import type {
  ShippingDistrict,
  ShippingProvince,
  ShippingWard,
} from "../shipping-location.type";

export async function getShippingProvinces(): Promise<ShippingProvince[]> {
  const response = await api.get<{ result?: ShippingProvince[] }>("/shipping/locations/provinces");
  return response.data.result ?? [];
}

export async function getShippingDistricts(
  provinceId: number,
): Promise<ShippingDistrict[]> {
  const response = await api.get<{ result?: ShippingDistrict[] }>(
    "/shipping/locations/districts",
    { params: { provinceId } },
  );
  return response.data.result ?? [];
}

export async function getShippingWards(
  districtId: number,
): Promise<ShippingWard[]> {
  const response = await api.get<{ result?: ShippingWard[] }>(
    "/shipping/locations/wards",
    { params: { districtId } },
  );
  return response.data.result ?? [];
}
