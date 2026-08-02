import { getCategories } from "@/features/categories/services/category.service";
import { catalogService } from "@/features/customer/catalog/services/catalogService";
import { CategoryTile } from "../home.types";
import { CatalogProductItem } from "@/features/customer/catalog/types/catalog.types";
import { categoriesData } from "@/constants/data";

export async function fetchHomeCategories(): Promise<CategoryTile[]> {
  try {
    const res = await getCategories({ size: 50, isActive: true });
    if (res && res.content && res.content.length > 0) {
      return res.content.map((c) => ({
        title: c.name,
        slug: c.slug,
        imageUrl: c.thumbnailUrl || c.thumbnail,
      }));
    }
  } catch (err) {
    console.warn("Lỗi khi tải danh mục trang chủ từ API, chuyển sang dữ liệu fallback:", err);
  }

  // Fallback if API returns empty or fails
  return categoriesData.map((c) => ({
    title: c.title,
    slug: c.href,
  }));
}

export async function fetchHomeProductsByCategory(categorySlug: string): Promise<CatalogProductItem[]> {
  try {
    const res = await catalogService.getCatalogProducts(categorySlug, {
      size: 8,
      inStock: true,
    });
    if (res && res.content && res.content.length > 0) {
      return res.content;
    }
  } catch (err) {
    console.warn(`Lỗi khi tải sản phẩm cho category ${categorySlug}:`, err);
  }
  return [];
}
