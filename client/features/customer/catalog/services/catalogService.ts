import { api } from "@/shared/http/api";
import { CategoryFacetData, CatalogProductItem, CatalogFilterParams } from "../types/catalog.types";

export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number; // 0-indexed
  first: boolean;
  last: boolean;
}

export const catalogService = {
  getCategoryFacets: async (categorySlug: string): Promise<CategoryFacetData> => {
    const res = await api.get(`/api/v1/public/catalog/category/${categorySlug}/facets`);
    return res.data.result;
  },

  getCatalogProducts: async (
    categorySlug: string,
    params: CatalogFilterParams
  ): Promise<PageResponse<CatalogProductItem>> => {
    const queryParams = new URLSearchParams();

    if (params.search) queryParams.set("search", params.search);
    if (params.brandId) queryParams.set("brandId", String(params.brandId));
    if (params.minPrice !== undefined) queryParams.set("minPrice", String(params.minPrice));
    if (params.maxPrice !== undefined) queryParams.set("maxPrice", String(params.maxPrice));
    if (params.inStock !== undefined) queryParams.set("inStock", String(params.inStock));
    if (params.page) queryParams.set("page", String(params.page));
    if (params.size) queryParams.set("size", String(params.size));
    if (params.sortBy) queryParams.set("sortBy", params.sortBy);
    if (params.order) queryParams.set("order", params.order);

    if (params.attributes) {
      Object.entries(params.attributes).forEach(([code, values]) => {
        if (values && values.length > 0) {
          queryParams.set(`attr_${code}`, values.join(","));
        }
      });
    }

    const res = await api.get(
      `/api/v1/public/catalog/category/${categorySlug}/products?${queryParams.toString()}`
    );
    return res.data.result;
  },
};
