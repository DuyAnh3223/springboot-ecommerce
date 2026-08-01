import { CatalogProductItem } from "@/features/(catalog)/catalog/types/catalog.types";

export interface HeroSlide {
  id: string;
  title: string;
  subtitle: string;
  badge: string;
  ctaText: string;
  ctaLink: string;
  image: string;
  highlightText?: string;
}

export interface SubBanner {
  id: string;
  title: string;
  subtitle: string;
  badge: string;
  ctaLink: string;
  image: string;
  bgGradient: string;
}

export interface TrustBadge {
  id: string;
  iconName: "truck" | "shield" | "creditCard" | "headphones";
  title: string;
  description: string;
}

export interface FlashSaleItem {
  id: number;
  productSlug: string;
  name: string;
  categoryName: string;
  originalPrice: number;
  salePrice: number;
  discountPercent: number;
  soldCount: number;
  totalStock: number;
  thumbnail: string;
}

export interface CategoryTile {
  title: string;
  slug: string;
  imageUrl?: string | null;
  itemCount?: number;
  iconName?: string;
}

export interface BrandItem {
  name: string;
  slug: string;
  logoUrl?: string;
}

export interface NewsCard {
  id: string;
  title: string;
  excerpt: string;
  date: string;
  category: string;
  image: string;
  readTime: string;
}

export interface HomeCatalogData {
  categories: CategoryTile[];
  trendingProducts: CatalogProductItem[];
  newArrivals: CatalogProductItem[];
  bestDeals: CatalogProductItem[];
  isLoading: boolean;
}
