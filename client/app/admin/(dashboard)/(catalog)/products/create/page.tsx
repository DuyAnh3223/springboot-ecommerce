import { getCategories } from "@/features/(catalog)/categories/services/category.service";
import { ProductFormPage } from "@/features/(catalog)/products/components/ProductFormPage";
import { CategoryResponse } from "@/features/(catalog)/categories/category.type";

export const metadata = {
  title: "Thêm sản phẩm | Admin",
  description: "Tạo sản phẩm mới, cấu hình thuộc tính và sinh tổ hợp biến thể SKU.",
};

export default async function CreateProductPage() {
  let categories: CategoryResponse[] = [];

  try {
    const res = await getCategories({ size: 150 });
    categories = res.content;
  } catch (error) {
    console.error("Lỗi khi tải danh mục cho sản phẩm:", error);
  }

  return (
    <div className="animate-in fade-in duration-300">
      <ProductFormPage categories={categories} />
    </div>
  );
}
