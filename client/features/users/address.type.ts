export interface AddressResponse {
  id: string;
  recipientName: string;
  phone: string;
  province: string;
  district: string;
  ward: string;
  streetAddress: string;
  country: string;
  isDefault: boolean;
}

export interface GetAddressesParams {
  search?: string;
  isDefault?: boolean;
  page?: number;
  size?: number;
  sortBy?: string;
  order?: "asc" | "desc";
}

export interface AddressRequest {
  recipientName: string;
  phone: string;
  province: string;
  district: string;
  ward: string;
  streetAddress: string;
  country?: string;
  isDefault?: boolean;
}

