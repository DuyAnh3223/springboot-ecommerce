"use client"

import { useEffect, useState, useTransition } from "react"
import { useRouter, usePathname, useSearchParams } from "next/navigation"
import { PageResponse, UserResponse } from "../user.type"
import { deleteUserAction } from "../actions"
import { CustomerEditDialog } from "./CustomerEditDialog"
import { Card, CardContent } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Badge } from "@/components/ui/badge"
import {
  Search,
  ChevronLeft,
  ChevronRight,
  Edit2,
  Trash2,
  ArrowUpDown,
  UserX,
  UserCheck,
  Loader2,
  Users,
  Calendar,
  Lock,
  Unlock,
} from "lucide-react"

interface CustomersClientProps {
  initialData: PageResponse<UserResponse>
}

export function CustomersClient({ initialData }: CustomersClientProps) {
  const router = useRouter()
  const pathname = usePathname()
  const searchParams = useSearchParams()
  const [isPending, startTransition] = useTransition()

  // Local state for search to allow debouncing
  const [searchTerm, setSearchTerm] = useState(searchParams.get("search") || "")
  const [selectedUser, setSelectedUser] = useState<UserResponse | null>(null)
  const [isEditOpen, setIsEditOpen] = useState(false)
  
  // Deactivate user state
  const [userToDeactivate, setUserToDeactivate] = useState<UserResponse | null>(null)
  const [isDeactivateOpen, setIsDeactivateOpen] = useState(false)

  // Current query state
  const currentPage = parseInt(searchParams.get("page") || "1")
  const currentSize = parseInt(searchParams.get("size") || "20")
  const currentIsActive = searchParams.get("isActive")
  const currentSortBy = searchParams.get("sortBy") || "createdAt"
  const currentOrder = searchParams.get("order") || "desc"

  // Debounced search
  useEffect(() => {
    const delayDebounceFn = setTimeout(() => {
      const currentSearch = searchParams.get("search") || ""
      if (searchTerm !== currentSearch) {
        updateQueryParams({ search: searchTerm, page: 1 })
      }
    }, 500)

    return () => clearTimeout(delayDebounceFn)
  }, [searchTerm])

  // Reset local search term when URL param changes externally
  useEffect(() => {
    setSearchTerm(searchParams.get("search") || "")
  }, [searchParams])

  const updateQueryParams = (newParams: Record<string, string | number | boolean | null | undefined>) => {
    const current = new URLSearchParams(Array.from(searchParams.entries()))
    
    Object.entries(newParams).forEach(([key, value]) => {
      if (value === null || value === undefined || value === "") {
        current.delete(key)
      } else {
        current.set(key, String(value))
      }
    })

    startTransition(() => {
      router.push(`${pathname}?${current.toString()}`)
    })
  }

  const handleSort = (field: string) => {
    let nextOrder: "asc" | "desc" = "asc"
    if (currentSortBy === field && currentOrder === "asc") {
      nextOrder = "desc"
    }
    updateQueryParams({ sortBy: field, order: nextOrder, page: 1 })
  }

  const handleDeactivate = async () => {
    if (!userToDeactivate) return
    const result = await deleteUserAction(userToDeactivate.id)
    if (result.error) {
      console.error("Lỗi khi khóa tài khoản:", result.error)
      return
    }
    setIsDeactivateOpen(false)
    setUserToDeactivate(null)
    router.refresh()
  }

  const formatDate = (dateString: string) => {
    try {
      const date = new Date(dateString)
      return date.toLocaleDateString("vi-VN", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
      })
    } catch (e) {
      return dateString
    }
  }

  return (
    <div className="space-y-4">
      {/* Search & Filter Controls */}
      <Card className="border-none shadow-sm bg-white p-4">
        <div className="flex flex-col md:flex-row gap-4 items-center justify-between">
          <div className="relative w-full md:max-w-md">
            <span className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400">
              <Search className="size-4.5" />
            </span>
            <Input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Tìm kiếm theo tên, email, số điện thoại..."
              className="pl-9 h-10 border-slate-200 focus-visible:ring-shop_orange/20"
            />
          </div>

          <div className="flex flex-wrap items-center gap-3 w-full md:w-auto">
            {/* Status Filter */}
            <div className="flex items-center gap-2">
              <span className="text-sm font-medium text-slate-500 whitespace-nowrap">Trạng thái:</span>
              <select
                value={currentIsActive || ""}
                onChange={(e) => updateQueryParams({ isActive: e.target.value, page: 1 })}
                className="h-10 px-3 rounded-lg border border-slate-200 bg-white text-sm focus:outline-none focus:ring-2 focus:ring-slate-100 font-medium text-slate-700 cursor-pointer"
              >
                <option value="">Tất cả</option>
                <option value="true">Hoạt động</option>
                <option value="false">Bị khóa</option>
              </select>
            </div>

            {/* Page Size Selector */}
            <div className="flex items-center gap-2">
              <span className="text-sm font-medium text-slate-500 whitespace-nowrap">Hiển thị:</span>
              <select
                value={currentSize}
                onChange={(e) => updateQueryParams({ size: e.target.value, page: 1 })}
                className="h-10 px-3 rounded-lg border border-slate-200 bg-white text-sm focus:outline-none focus:ring-2 focus:ring-slate-100 font-medium text-slate-700 cursor-pointer"
              >
                <option value="10">10 / trang</option>
                <option value="20">20 / trang</option>
                <option value="50">50 / trang</option>
              </select>
            </div>
          </div>
        </div>
      </Card>

      {/* Main Customers Table Card */}
      <Card className="border-none shadow-sm bg-white overflow-hidden">
        <div className="relative">
          {isPending && (
            <div className="absolute inset-0 bg-white/50 backdrop-blur-xs flex items-center justify-center z-10 transition-opacity">
              <div className="flex items-center gap-2 bg-slate-950 text-white px-4 py-2 rounded-full shadow-lg">
                <Loader2 className="size-4 animate-spin" />
                <span className="text-xs font-semibold">Đang cập nhật...</span>
              </div>
            </div>
          )}

          {initialData.content.length === 0 ? (
            <CardContent className="flex flex-col items-center justify-center py-20 text-center">
              <div className="p-4 bg-slate-50 rounded-full text-slate-400 mb-4 border border-slate-100">
                <Users className="size-12" />
              </div>
              <h3 className="text-lg font-bold text-slate-700">Không tìm thấy khách hàng</h3>
              <p className="text-sm text-slate-400 mt-1 max-w-sm">
                Hãy thử thay đổi từ khóa tìm kiếm hoặc cài đặt bộ lọc trạng thái khác.
              </p>
            </CardContent>
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader className="bg-slate-50/70 border-b border-slate-100">
                  <TableRow>
                    <TableHead 
                      className="font-semibold text-slate-600 cursor-pointer select-none hover:bg-slate-100/50 py-3.5"
                      onClick={() => handleSort("username")}
                    >
                      <div className="flex items-center gap-1.5">
                        Tài khoản
                        <ArrowUpDown className="size-3.5 text-slate-400" />
                      </div>
                    </TableHead>
                     <TableHead 
                      className="font-semibold text-slate-600 cursor-pointer select-none hover:bg-slate-100/50 py-3.5"
                      onClick={() => handleSort("firstName")}
                    >
                      <div className="flex items-center gap-1.5">
                        Họ và Tên
                        <ArrowUpDown className="size-3.5 text-slate-400" />
                      </div>
                    </TableHead>
                    <TableHead 
                      className="font-semibold text-slate-600 cursor-pointer select-none hover:bg-slate-100/50"
                      onClick={() => handleSort("email")}
                    >
                      <div className="flex items-center gap-1.5">
                        Email
                        <ArrowUpDown className="size-3.5 text-slate-400" />
                      </div>
                    </TableHead>
                    <TableHead className="font-semibold text-slate-600">Số điện thoại</TableHead>
                    <TableHead className="font-semibold text-slate-600">Vai trò</TableHead>
                    <TableHead className="font-semibold text-slate-600 text-center">Trạng thái</TableHead>
                    <TableHead 
                      className="font-semibold text-slate-600 cursor-pointer select-none hover:bg-slate-100/50 text-right"
                      onClick={() => handleSort("createdAt")}
                    >
                      <div className="flex items-center gap-1.5 justify-end">
                        Ngày tạo
                        <ArrowUpDown className="size-3.5 text-slate-400" />
                      </div>
                    </TableHead>
                    <TableHead className="font-semibold text-slate-600 text-center">Thao tác</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {initialData.content.map((user) => (
                    <TableRow key={user.id} className="hover:bg-slate-50/50 border-b border-slate-100/80">
                      <TableCell className="font-semibold text-slate-800">{user.username}</TableCell>
                      <TableCell className="text-slate-750 ">{user.lastName} {user.firstName}</TableCell>
                      <TableCell className="text-slate-700 font-medium">{user.email}</TableCell>
                      <TableCell className="text-slate-500 font-mono">{user.phone || "-"}</TableCell>
                      <TableCell>
                        <div className="flex flex-wrap gap-1">
                          {user.roles?.map((role) => (
                            <Badge
                              key={role.id}
                              variant="outline"
                              className={`text-[10px] font-semibold tracking-wider px-1.5 py-0.5 border ${
                                role.name === "ADMIN"
                                  ? "bg-rose-50 text-rose-600 border-rose-200"
                                  : "bg-blue-50 text-blue-600 border-blue-200"
                              }`}
                            >
                              {role.name}
                            </Badge>
                          ))}
                        </div>
                      </TableCell>
                      <TableCell className="text-center">
                        {user.active ? (
                          <Badge className="bg-emerald-50 hover:bg-emerald-50 text-emerald-600 border border-emerald-200 text-xs font-semibold px-2 py-0.5 shadow-none">
                            Đang hoạt động
                          </Badge>
                        ) : (
                          <Badge className="bg-slate-50 hover:bg-slate-50 text-slate-400 border border-slate-200 text-xs font-semibold px-2 py-0.5 shadow-none">
                            Đã khóa
                          </Badge>
                        )}
                      </TableCell>
                      <TableCell className="text-right text-slate-500 font-medium">
                        <div className="flex items-center justify-end gap-1.5 text-xs">
                          <Calendar className="size-3.5 text-slate-350" />
                          {formatDate(user.createdAt)}
                        </div>
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center justify-center gap-1.5">
                          <Button
                            variant="ghost"
                            size="icon-sm"
                            className="text-slate-500 hover:text-slate-800 hover:bg-slate-100 cursor-pointer"
                            onClick={() => {
                              setSelectedUser(user)
                              setIsEditOpen(true)
                            }}
                          >
                            <Edit2 className="size-4" />
                          </Button>
                          {user.active ? (
                            <Button
                              variant="ghost"
                              size="icon-sm"
                              className="text-amber-500 hover:text-amber-700 hover:bg-amber-50 cursor-pointer"
                              onClick={() => {
                                setUserToDeactivate(user)
                                setIsDeactivateOpen(true)
                              }}
                            >
                              <Lock className="size-4" />
                            </Button>
                          ) : (
                            <Button
                              variant="ghost"
                              size="icon-sm"
                              disabled
                              className="text-slate-300"
                            >
                              <Unlock className="size-4" />
                            </Button>
                          )}
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </div>

        {/* Pagination Toolbar */}
        {initialData.totalPages > 1 && (
          <div className="flex items-center justify-between px-6 py-4 bg-slate-50/50 border-t border-slate-100">
            <div className="text-sm text-slate-500 font-medium">
              Hiển thị <span className="font-bold text-slate-700">{initialData.numberOfElements}</span> trên tổng số{" "}
              <span className="font-bold text-slate-700">{initialData.totalElements}</span> khách hàng.
            </div>
            <div className="flex items-center gap-1">
              <Button
                variant="outline"
                size="icon"
                disabled={currentPage <= 1 || isPending}
                className="size-8.5 cursor-pointer border-slate-200 text-slate-600"
                onClick={() => updateQueryParams({ page: currentPage - 1 })}
              >
                <ChevronLeft className="size-4" />
              </Button>
              
              {/* Pagination list */}
              {Array.from({ length: initialData.totalPages }, (_, i) => i + 1)
                .filter(p => Math.abs(p - currentPage) <= 2 || p === 1 || p === initialData.totalPages)
                .map((p, index, array) => {
                  const showEllipsis = index > 0 && p - array[index - 1] > 1;
                  return (
                    <div key={p} className="flex items-center">
                      {showEllipsis && <span className="px-1.5 text-slate-400 font-medium">...</span>}
                      <Button
                        variant={p === currentPage ? "default" : "outline"}
                        size="sm"
                        disabled={isPending}
                        className={`size-8.5 text-xs font-semibold cursor-pointer ${
                          p === currentPage
                            ? "bg-slate-900 text-white shadow-sm"
                            : "border-slate-200 text-slate-600 hover:text-slate-800"
                        }`}
                        onClick={() => updateQueryParams({ page: p })}
                      >
                        {p}
                      </Button>
                    </div>
                  );
                })}

              <Button
                variant="outline"
                size="icon"
                disabled={currentPage >= initialData.totalPages || isPending}
                className="size-8.5 cursor-pointer border-slate-200 text-slate-600"
                onClick={() => updateQueryParams({ page: currentPage + 1 })}
              >
                <ChevronRight className="size-4" />
              </Button>
            </div>
          </div>
        )}
      </Card>

      {/* Edit Customer Dialog */}
      <CustomerEditDialog
        user={selectedUser}
        open={isEditOpen}
        onOpenChange={setIsEditOpen}
        onSuccess={() => {
          router.refresh()
        }}
      />

      {/* Confirm Deactivate Dialog */}
      <Dialog open={isDeactivateOpen} onOpenChange={setIsDeactivateOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="text-amber-600 flex items-center gap-2">
              <Lock className="size-5" /> Khóa tài khoản khách hàng
            </DialogTitle>
            <DialogDescription className="pt-2">
              Bạn có chắc chắn muốn khóa tài khoản của{" "}
              <span className="font-bold text-slate-800">
                {userToDeactivate?.username}
              </span>{" "}
              ({userToDeactivate?.lastName} {userToDeactivate?.firstName}) không? Người dùng này sẽ không thể đăng nhập vào hệ thống nữa.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter className="mt-4">
            <Button
              type="button"
              variant="outline"
              onClick={() => setIsDeactivateOpen(false)}
              className="cursor-pointer"
            >
              Hủy bỏ
            </Button>
            <Button
              type="button"
              onClick={handleDeactivate}
              className="bg-amber-600 hover:bg-amber-700 text-white cursor-pointer"
            >
              Đồng ý khóa
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
