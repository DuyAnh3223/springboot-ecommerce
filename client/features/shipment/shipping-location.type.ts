export interface ShippingProvince {
  id: number;
  name: string;
}

export interface ShippingDistrict {
  id: number;
  provinceId: number;
  name: string;
  deliverySupported: boolean;
}

export interface ShippingWard {
  code: string;
  districtId: number;
  name: string;
  deliverySupported: boolean;
}

export interface ShippingAddressSelection {
  provinceId: number | null;
  province: string;
  districtId: number | null;
  district: string;
  wardCode: string;
  ward: string;
}
