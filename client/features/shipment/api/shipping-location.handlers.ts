import "server-only";
import { isAxiosError } from "axios";
import { NextRequest, NextResponse } from "next/server";
import {
  getShippingDistricts,
  getShippingProvinces,
  getShippingWards,
} from "../services/shipping-location.service";

function errorResponse(error: unknown) {
  const status = isAxiosError(error) && error.response?.status === 401 ? 401 : 503;
  return NextResponse.json({ message: "Không thể tải dữ liệu địa chỉ GHN." }, { status });
}

export async function getProvincesHandler() {
  try {
    return NextResponse.json(await getShippingProvinces());
  } catch (error) {
    return errorResponse(error);
  }
}

export async function getDistrictsHandler(request: NextRequest) {
  const provinceId = request.nextUrl.searchParams.get("provinceId");
  if (!provinceId || !/^\d+$/.test(provinceId) || Number(provinceId) <= 0) {
    return NextResponse.json({ message: "provinceId không hợp lệ." }, { status: 400 });
  }
  try {
    return NextResponse.json(await getShippingDistricts(Number(provinceId)));
  } catch (error) {
    return errorResponse(error);
  }
}

export async function getWardsHandler(request: NextRequest) {
  const districtId = request.nextUrl.searchParams.get("districtId");
  if (!districtId || !/^\d+$/.test(districtId) || Number(districtId) <= 0) {
    return NextResponse.json({ message: "districtId không hợp lệ." }, { status: 400 });
  }
  try {
    return NextResponse.json(await getShippingWards(Number(districtId)));
  } catch (error) {
    return errorResponse(error);
  }
}
