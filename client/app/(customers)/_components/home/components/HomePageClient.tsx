"use client";

import { useEffect, useState } from "react";
import HeroSection from "./HeroSection";
import TrustBadges from "./TrustBadges";
import FlashSaleSection from "./FlashSaleSection";
import CategoryGrid from "./CategoryGrid";
import TrendingProducts from "./TrendingProducts";
import PcBuilderBanner from "./PcBuilderBanner";
import BrandStrip from "./BrandStrip";
import TechNewsSection from "./TechNewsSection";
import { fetchHomeCategories, fetchHomeProductsByCategory } from "../services/homeCatalog.service";
import { CategoryTile } from "../home.types";
import { CatalogProductItem } from "@/features/customer/catalog/types/catalog.types";

export default function HomePageClient() {
  const [categories, setCategories] = useState<CategoryTile[]>([]);
  const [products, setProducts] = useState<CatalogProductItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let isMounted = true;

    async function loadHomeData() {
      try {
        setIsLoading(true);
        // Load categories and products in parallel
        const [catsRes, prodsRes] = await Promise.all([
          fetchHomeCategories(),
          fetchHomeProductsByCategory("cpu"), // Try fetching default hardware category
        ]);

        if (isMounted) {
          setCategories(catsRes);
          setProducts(prodsRes);
        }
      } catch (err) {
        console.warn("Lỗi khi tải dữ liệu trang chủ:", err);
      } finally {
        if (isMounted) setIsLoading(false);
      }
    }

    loadHomeData();

    return () => {
      isMounted = false;
    };
  }, []);

  return (
    <div className="bg-slate-50 min-h-screen text-slate-900 font-sans selection:bg-rose-600 selection:text-white">
      {/* 1. Hero Section (Slider + Sub Banners) */}
      <HeroSection />

      {/* 2. Trust Badges (4 Cam kết dịch vụ) */}
      <TrustBadges />

      {/* 3. Flash Sale Giờ Vàng (Count-down + Cards) */}
      <FlashSaleSection />

      {/* 4. Danh Mục Nổi Bật Grid */}
      <CategoryGrid categories={categories} />

      {/* 5. Sản Phẩm Hot Tabs (Bán chạy / Mới về / Giá tốt) */}
      <TrendingProducts products={products} isLoading={isLoading} />

      {/* 6. Banner Công Cụ Xây Dựng Cấu Hình PC */}
      <PcBuilderBanner />

      {/* 7. Thương Hiệu Đồng Hành */}
      <BrandStrip />

      {/* 8. Tin Tức & Đánh Giá Công Nghệ */}
      <TechNewsSection />
    </div>
  );
}
