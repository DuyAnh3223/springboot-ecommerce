import { getProducts } from "@/features/(catalog)/products/services/product.service";
import { getCategories } from "@/features/(catalog)/categories/services/category.service";
import { ProductsClient } from "@/features/(catalog)/products/components/ProductsClient";
import { CategoryResponse } from "@/features/(catalog)/categories/category.type";

interface PageProps {
  searchParams: Promise<{
    search?: string;
    categoryId?: string;
    page?: string;
    size?: string;
    sortBy?: string;
    order?: string;
    status?: string;
  }>;
}

export const metadata = {
  title: "Quản lý sản phẩm | Admin",
  description: "Quản lý sản phẩm, thuộc tính EAV và biến thể SKU.",
};

export default async function ProductsPage({ searchParams }: PageProps) {
  const params = await searchParams;

  const search = params.search || undefined;
  const categoryId = params.categoryId ? parseInt(params.categoryId) : undefined;
  const page = params.page ? parseInt(params.page) : 1;
  const size = params.size ? parseInt(params.size) : 10;
  const sortBy = params.sortBy || "name";
  const order = (params.order as "asc" | "desc") || "desc";
  const status = params.status || "all";

  let initialData;
  try {
    initialData = await getProducts({
      search,
      categoryId,
      page,
      size,
      sortBy,
      order,
      status: status === "all" ? undefined : (status as "draft" | "published" | "all"),
    });
  } catch (error) {
    console.error("Lỗi khi tải danh sách sản phẩm:", error);
    initialData = {
      content: [],
      totalElements: 0,
      totalPages: 0,
      size,
      number: 0,
      numberOfElements: 0,
      first: true,
      last: true,
      empty: true,
    };
  }

  let categories: CategoryResponse[] = [];
  try {
    const res = await getCategories({ size: 100 });
    categories = res.content;
  } catch (error) {
    console.error("Lỗi khi tải danh mục cho bộ lọc:", error);
  }

  return (
    <div className="space-y-2 animate-in fade-in duration-300 mx-auto">
      <ProductsClient initialData={initialData} categories={categories} />
    </div>
  );
}
