import { getCategories } from "@/features/(catalog)/categories/services/category.service";
import { getProduct } from "@/features/(catalog)/products/services/product.service";
import { ProductFormPage } from "@/features/(catalog)/products/components/ProductFormPage";
import { notFound } from "next/navigation";

interface PageProps {
  params: Promise<{
    id: string;
  }>;
}

export const metadata = {
  title: "Chỉnh sửa sản phẩm | Admin",
  description: "Cập nhật thông tin chi tiết, thuộc tính và danh sách biến thể SKU sản phẩm.",
};

export default async function EditProductPage({ params }: PageProps) {
  const { id } = await params;
  const productId = parseInt(id);

  if (isNaN(productId)) {
    notFound();
  }

  let product = null;
  let categories = [];

  try {
    const [prodRes, catRes] = await Promise.all([
      getProduct(productId),
      getCategories({ size: 150 }),
    ]);
    product = prodRes;
    categories = catRes.content;
  } catch (error) {
    console.error("Lỗi khi tải dữ liệu cho chỉnh sửa sản phẩm:", error);
    notFound();
  }

  return (
    <div className="animate-in fade-in duration-300">
      <ProductFormPage product={product} categories={categories} />
    </div>
  );
}
