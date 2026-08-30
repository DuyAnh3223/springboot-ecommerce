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

export type CatalogAttributeValue = string | number | boolean;

export interface CatalogAttributeDefinition {
  code: string;
  name: string;
  unit: string | null;
  dataType: "STRING" | "NUMBER" | "BOOLEAN" | "ENUM";
  sortOrder: number;
}

export interface CatalogProductReference {
  id: number;
  name: string;
  slug: string;
}

export interface CatalogProductImage {
  id: number;
  url: string;
  altText: string | null;
  sortOrder: number;
  primary: boolean;
}

export interface CatalogProductSku {
  id: number;
  sku: string;
  price: number;
  stock: number;
  currency: string;
  weightGram: number | null;
  attributes: Record<string, CatalogAttributeValue>;
  primaryImageUrl: string | null;
  images: CatalogProductImage[];
}

export interface CatalogProductDetail {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  primaryImageUrl: string | null;
  rating: number | null;
  reviewCount: number;
  category: CatalogProductReference;
  brand: CatalogProductReference | null;
  attributes: Record<string, CatalogAttributeValue | string[]>;
  specificationDefinitions: CatalogAttributeDefinition[];
  variantDefinitions: CatalogAttributeDefinition[];
  priceMin: number | null;
  priceMax: number | null;
  totalStock: number;
  skus: CatalogProductSku[];
}
