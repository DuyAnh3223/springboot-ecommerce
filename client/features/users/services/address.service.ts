import { api } from "@/lib/axios";
import {
  AddressResponse,
  GetAddressesParams,
  AddressRequest,
} from "../address.type";
import { PageResponse } from "@/types/page.type";

export async function getAddress(addressId: string): Promise<AddressResponse> {
  const response = await api.get(`/addresses/${addressId}`);
  return response.data.result;
}

export async function getAddresses(
  params?: GetAddressesParams,
): Promise<PageResponse<AddressResponse>> {
  const response = await api.get(`/addresses`, { params });
  return response.data.result;
}

export async function createAddress(
  values: AddressRequest,
): Promise<AddressResponse> {
  const response = await api.post("/addresses", values);
  return response.data.result;
}

export async function updateAddress(
  addressId: string,
  values: AddressRequest,
): Promise<AddressResponse> {
  const response = await api.patch(`/addresses/${addressId}`, values);
  return response.data.result;
}

export async function deleteAddress(addressId: string): Promise<void> {
  await api.delete(`/addresses/${addressId}`);
}

