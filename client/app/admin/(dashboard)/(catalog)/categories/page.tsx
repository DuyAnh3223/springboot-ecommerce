import { getCategories } from "@/features/(catalog)/categories/services/category.service";
import { CategoriesClient } from "@/features/(catalog)/categories/components/CategoriesClient";

interface PageProps {
  searchParams: Promise<{
    keyword?: string;
    isActive?: string;
    parentId?: string;
    page?: string;
    size?: string;
    sortBy?: string;
    order?: string;
  }>;
}

export const metadata = {
  title: "Quản lý danh mục | Admin",
  description: "Thêm, sửa, xóa và tìm kiếm danh mục sản phẩm.",
};

export default async function CategoriesPage({ searchParams }: PageProps) {
  const params = await searchParams;

  const keyword = params.keyword || undefined;
  const isActive =
    params.isActive === "true"
      ? true
      : params.isActive === "false"
        ? false
        : undefined;
  const parentId = params.parentId ? parseInt(params.parentId) : undefined;
  const page = params.page ? parseInt(params.page) : 1;
  const size = params.size ? parseInt(params.size) : 10;
  const sortBy = params.sortBy || "name";
  const order = (params.order as "asc" | "desc") || "asc";

  let initialData;
  try {
    initialData = await getCategories({
      keyword,
      isActive,
      parentId,
      page,
      size,
      sortBy,
      order,
    });
  } catch (error) {
    console.error("Lỗi khi tải danh sách danh mục:", error);
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

  return (
    <div className="space-y-2 animate-in fade-in duration-300 mx-auto">
      <CategoriesClient initialData={initialData} />
    </div>
  );
}