export interface AddressResponse {
    id: string;
    recipientName: string;
    phone: string;
    province: string;
    district?: string | null;
    ward: string;
    ghnProvinceId?: number | null;
    ghnDistrictId?: number | null;
    ghnWardCode?: string | null;
    street: string;
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
    ghnProvinceId?: number;
    ghnDistrictId?: number;
    ghnWardCode?: string;
    street: string;
    country?: string;
    isDefault?: boolean;
}

export type AddressUpdateRequest = Partial<AddressRequest>;
