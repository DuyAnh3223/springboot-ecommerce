"use client";

import type { SelectHTMLAttributes } from "react";
import { useShippingLocations } from "../hooks/useShippingLocations";
import type { ShippingAddressSelection } from "../shipping-location.type";

interface ShippingAddressFieldsProps {
  provinceId?: number | null;
  districtId?: number | null;
  wardCode?: string | null;
  onChange: (selection: ShippingAddressSelection) => void;
  provinceError?: string;
  districtError?: string;
  wardError?: string;
}

const selectClass =
  "h-11 w-full rounded-lg border border-input bg-white px-3 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:cursor-not-allowed disabled:bg-slate-50";

function LocationSelect(props: SelectHTMLAttributes<HTMLSelectElement>) {
  return <select {...props} className={`${selectClass} ${props.className || ""}`} />;
}

export function ShippingAddressFields({
  provinceId,
  districtId,
  wardCode,
  onChange,
  provinceError,
  districtError,
  wardError,
}: ShippingAddressFieldsProps) {
  const {
    provinces,
    districts,
    wards,
    loadingProvinces,
    loadingDistricts,
    loadingWards,
    error,
    retry,
  } = useShippingLocations({ provinceId, districtId });
  const provinceValue = provinceId == null ? "" : String(provinceId);
  const districtValue = districtId == null ? "" : String(districtId);

  const handleProvinceChange = (value: string) => {
    const selected = provinces.find((item) => String(item.id) === value);
    onChange({
      provinceId: selected?.id ?? null,
      province: selected?.name ?? "",
      districtId: null,
      district: "",
      wardCode: "",
      ward: "",
    });
  };

  const handleDistrictChange = (value: string) => {
    const selected = districts.find((item) => String(item.id) === value);
    onChange({
      provinceId: provinceId ?? null,
      province: provinces.find((item) => item.id === provinceId)?.name ?? "",
      districtId: selected?.id ?? null,
      district: selected?.name ?? "",
      wardCode: "",
      ward: "",
    });
  };

  const handleWardChange = (value: string) => {
    const selected = wards.find((item) => item.code === value);
    onChange({
      provinceId: provinceId ?? null,
      province: provinces.find((item) => item.id === provinceId)?.name ?? "",
      districtId: districtId ?? null,
      district: districts.find((item) => item.id === districtId)?.name ?? "",
      wardCode: selected?.code ?? "",
      ward: selected?.name ?? "",
    });
  };

  return (
    <div className="space-y-3">
      <div className="grid gap-4 sm:grid-cols-3">
        <div className="space-y-2">
          <label className="text-sm font-medium">Tỉnh/Thành phố</label>
          <LocationSelect
            aria-label="Tỉnh/Thành phố"
            value={provinceValue}
            disabled={loadingProvinces}
            onChange={(event) => handleProvinceChange(event.target.value)}
          >
            <option value="">{loadingProvinces ? "Đang tải..." : "Chọn tỉnh/thành phố"}</option>
            {provinces.map((item) => (
              <option key={item.id} value={item.id}>{item.name}</option>
            ))}
          </LocationSelect>
          {provinceError && <p className="text-xs text-destructive">{provinceError}</p>}
        </div>
        <div className="space-y-2">
          <label className="text-sm font-medium">Quận/Huyện</label>
          <LocationSelect
            aria-label="Quận/Huyện"
            value={districtValue}
            disabled={!provinceId || loadingDistricts}
            onChange={(event) => handleDistrictChange(event.target.value)}
          >
            <option value="">
              {loadingDistricts ? "Đang tải..." : !provinceId ? "Chọn tỉnh trước" : "Chọn quận/huyện"}
            </option>
            {districts.map((item) => (
              <option key={item.id} value={item.id} disabled={!item.deliverySupported}>
                {item.name}{item.deliverySupported ? "" : " (chưa hỗ trợ giao)"}
              </option>
            ))}
          </LocationSelect>
          {districtError && <p className="text-xs text-destructive">{districtError}</p>}
        </div>
        <div className="space-y-2">
          <label className="text-sm font-medium">Phường/Xã</label>
          <LocationSelect
            aria-label="Phường/Xã"
            value={wardCode || ""}
            disabled={!districtId || loadingWards}
            onChange={(event) => handleWardChange(event.target.value)}
          >
            <option value="">
              {loadingWards ? "Đang tải..." : !districtId ? "Chọn quận/huyện trước" : "Chọn phường/xã"}
            </option>
            {wards.map((item) => (
              <option key={item.code} value={item.code} disabled={!item.deliverySupported}>
                {item.name}{item.deliverySupported ? "" : " (chưa hỗ trợ giao)"}
              </option>
            ))}
          </LocationSelect>
          {wardError && <p className="text-xs text-destructive">{wardError}</p>}
        </div>
      </div>
      {error && (
        <div className="flex items-center gap-3 text-xs text-amber-700">
          <span>{error}</span>
          <button type="button" onClick={retry} className="font-semibold underline">Thử lại</button>
        </div>
      )}
    </div>
  );
}
