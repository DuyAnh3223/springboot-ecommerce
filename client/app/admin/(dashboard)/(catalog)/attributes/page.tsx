import { getGlobalAttributes } from "@/features/(catalog)/attributes/services/attribute.service";
import AttributesClient from "@/features/(catalog)/attributes/components/AttributesClient";

interface PageProps {
  searchParams: Promise<{
    keyword?: string;
    page?: string;
    size?: string;
    sortBy?: string;
    order?: string;
  }>;
}

export const metadata = {
  title: "Quản lý thuộc tính ",
  description: "Định nghĩa và quản lý các thuộc tính dùng chung trong hệ thống.",
};

export default async function AttributesPage({ searchParams }: PageProps) {
  const params = await searchParams;

  const keyword = params.keyword || undefined;
  const page = params.page ? parseInt(params.page) : 1;
  const size = params.size ? parseInt(params.size) : 10;
  const sortBy = params.sortBy || "name";
  const order = (params.order as "asc" | "desc") || "asc";

  let initialData;
  try {
    initialData = await getGlobalAttributes({
      keyword,
      page,
      size,
      sortBy,
      order,
    });
  } catch (error) {
    console.error("Lỗi khi tải danh sách thuộc tính:", error);
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
      <AttributesClient initialData={initialData} />
    </div>
  );
}
