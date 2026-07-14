import React from "react"
import { getUsers } from "@/features/users/services/user.service"
import { CustomersClient } from "@/features/users/components/CustomersClient"
import { Users } from "lucide-react"

interface PageProps {
  searchParams: Promise<{
    search?: string;
    isActive?: string;
    page?: string;
    size?: string;
    sortBy?: string;
    order?: string;
  }>;
}

export default async function CustomersPage({ searchParams }: PageProps) {
  const params = await searchParams

  const search = params.search || undefined
  const isActive =
    params.isActive === "true"
      ? true
      : params.isActive === "false"
      ? false
      : undefined
  const page = params.page ? parseInt(params.page) : 1
  const size = params.size ? parseInt(params.size) : 20
  const sortBy = params.sortBy || "createdAt"
  const order = (params.order as "asc" | "desc") || "desc"

  // Fetch paginated, filtered and sorted users from the backend
  let initialData
  try {
    initialData = await getUsers({
      search,
      isActive,
      page,
      size,
      sortBy,
      order,
    })
  } catch (error) {
    console.error("Lỗi khi tải danh sách khách hàng:", error)
    // Fallback empty data if backend call fails to prevent page crashing
    initialData = {
      content: [],
      totalElements: 0,
      totalPages: 0,
      size: size,
      number: 0,
      numberOfElements: 0,
      first: true,
      last: true,
      empty: true,
    }
  }

  return (
    <div className="space-y-2 animate-in fade-in duration-300  mx-auto ">
      {/* Interactive client component */}
      <CustomersClient initialData={initialData} />
    </div>
  )
}
