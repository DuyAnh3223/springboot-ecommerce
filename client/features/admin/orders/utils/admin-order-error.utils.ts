type AdminOrderFailure = { status?: number; code?: number };

function isAdminOrderFailure(error: unknown): error is AdminOrderFailure {
  return typeof error === "object" && error !== null && ("status" in error || "code" in error);
}

export function getAdminOrderErrorMessage(error: unknown): string {
  if (!isAdminOrderFailure(error)) return "Không thể tải dữ liệu đơn hàng. Vui lòng thử lại.";
  if (error.status === 403 || error.code === 1003) return "Bạn không có quyền truy cập quản lý đơn hàng.";
  if (error.status === 404 || error.code === 1034) return "Không tìm thấy đơn hàng này.";
  if (error.status === 409 || error.code === 1069) return "Đơn hàng đã thay đổi. Dữ liệu mới nhất đã được tải lại.";
  if (error.status === 400) return "Thông tin cập nhật chưa hợp lệ.";
  return "Hệ thống đơn hàng tạm thời không khả dụng. Vui lòng thử lại sau.";
}

export const shouldRefreshAdminOrderAfterError = (error: unknown): boolean =>
  isAdminOrderFailure(error) && (error.status === 409 || error.code === 1069);
