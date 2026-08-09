import { api } from "@/shared/http/api";
import { PageResponse } from "@/shared/types/page.type";
import {
  VoucherResponse,
  VoucherSearchParams,
  VoucherCreateRequest,
  VoucherUpdateRequest,
  VoucherDiscountRequest,
  VoucherDiscountResponse,
} from "../voucher.type";

export async function getVouchers(
  params?: VoucherSearchParams,
): Promise<PageResponse<VoucherResponse>> {
  const response = await api.get("/vouchers", { params });
  return response.data.result;
}

export async function getVoucher(
  voucherCode: string,
): Promise<VoucherResponse> {
  const response = await api.get(`/vouchers/${voucherCode}`);
  return response.data.result;
}

export async function createVoucher(
  values: VoucherCreateRequest,
): Promise<VoucherResponse> {
  const response = await api.post("/vouchers", values);
  return response.data.result;
}

export async function updateVoucher(
  voucherCode: string,
  values: VoucherUpdateRequest,
): Promise<VoucherResponse> {
  const response = await api.patch(`/vouchers/${voucherCode}`, values);
  return response.data.result;
}

export async function deleteVoucher(voucherCode: string): Promise<void> {
  await api.delete(`/vouchers/${voucherCode}`);
}

export async function validateVoucher(
  values: VoucherDiscountRequest,
): Promise<VoucherDiscountResponse> {
  const response = await api.post("/vouchers/validate", values);
  return response.data.result;
}
