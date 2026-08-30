import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { cache } from "react";
import { ChevronRight, Home } from "lucide-react";
import { ProductDetailClient } from "@/features/customer/catalog/components/product-detail/ProductDetailClient";
import { ProductSpecifications } from "@/features/customer/catalog/components/product-detail/ProductSpecifications";
import {
  CatalogProductApiError,
  catalogService,
} from "@/features/customer/catalog/services/catalog.service";

export const dynamic = "force-dynamic";

type ProductPageProps = {
  params: Promise<{ slug: string }>;
};

const loadProduct = cache((slug: string) => catalogService.getProductDetail(slug));

function isProductNotFound(error: unknown) {
  return error instanceof CatalogProductApiError && (error.status === 404 || error.code === 1008);
}

export async function generateMetadata({ params }: ProductPageProps): Promise<Metadata> {
  const { slug } = await params;

  try {
    const product = await loadProduct(slug);
    const description =
      product.description?.trim().slice(0, 160) ||
      `Xem thông tin và lựa chọn phiên bản ${product.name} tại ABTechZone.`;

    return {
      title: product.name,
      description,
      alternates: { canonical: `/products/${product.slug}` },
      openGraph: {
        title: `${product.name} | ABTechZone`,
        description,
        type: "website",
        images: product.primaryImageUrl ? [{ url: product.primaryImageUrl, alt: product.name }] : [],
      },
    };
  } catch (error) {
    if (isProductNotFound(error)) {
      return { title: "Không tìm thấy sản phẩm", robots: { index: false, follow: false } };
    }
    return { title: "Chi tiết sản phẩm" };
  }
}

export default async function ProductPage({ params }: ProductPageProps) {
  const { slug } = await params;

  let product;
  try {
    product = await loadProduct(slug);
  } catch (error) {
    if (isProductNotFound(error)) notFound();
    throw error;
  }

  return (
    <main className="min-h-screen bg-slate-50 pb-16 text-slate-900">
      <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
        <nav aria-label="Đường dẫn" className="mb-5 flex flex-wrap items-center gap-1.5 text-xs text-slate-500">
          <Link href="/" className="inline-flex items-center gap-1 hover:text-emerald-700">
            <Home className="h-3.5 w-3.5" /> Trang chủ
          </Link>
          <ChevronRight className="h-3.5 w-3.5" aria-hidden="true" />
          <Link href={`/category/${product.category.slug}`} className="hover:text-emerald-700">
            {product.category.name}
          </Link>
          <ChevronRight className="h-3.5 w-3.5" aria-hidden="true" />
          <span className="max-w-64 truncate font-medium text-slate-700" aria-current="page">
            {product.name}
          </span>
        </nav>

        <ProductDetailClient product={product} />
        <ProductSpecifications product={product} />
      </div>
    </main>
  );
}
