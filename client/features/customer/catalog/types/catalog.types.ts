export interface BrandFacet {
  id: number;
  name: string;
  slug: string;
}

export interface AttributeFacet {
  attributeId: number;
  code: string;
  name: string;
  dataType: "STRING" | "NUMBER" | "BOOLEAN" | "ENUM";
  unit?: string;
  enumValues?: string[];
  isFilterable: boolean;
  isSortable: boolean;
  isVariantDefining: boolean;
  isMultiValue: boolean;
  sortOrder: number;
  minBound?: number;
  maxBound?: number;
}

export interface CategoryFacetData {
  categoryId: number;
  categoryName: string;
  categorySlug: string;
  brands: BrandFacet[];
  priceMin: number;
  priceMax: number;
  attributes: AttributeFacet[];
}

export interface CatalogProductItem {
  id: number;
  name: string;
  slug: string;
  thumbnail?: string;
  primaryImageUrl?: string;
  description?: string;
  rating?: number;
  reviewCount: number;
  totalStock: number;
  skuCount: number;
  activeSkuCount: number;
  priceMin: number;
  priceMax: number;
  brand?: {
    id: number;
    name: string;
  };
  singleSkuId?: number;
  attributes?: Record<string, string | number | boolean | string[]>;
}

export interface CatalogFilterParams {
  search?: string;
  brandId?: number;
  minPrice?: number;
  maxPrice?: number;
  inStock?: boolean;
  page?: number;
  size?: number;
  sortBy?: string;
  order?: "asc" | "desc";
  attributes?: Record<string, string[]>;
}
