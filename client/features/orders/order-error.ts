export interface OrderErrorLike {
  code?: number;
  status?: number;
}

export function isOrderNotFound(error: OrderErrorLike): boolean {
  return error.code === 1034 || error.status === 404;
}

export function getOrderErrorMessage(error: OrderErrorLike): string {
  if (isOrderNotFound(error)) {
    return "Không tìm thấy đơn hàng hoặc bạn không có quyền xem đơn hàng này.";
  }

  if (error.code === 1069 || error.status === 409) {
    return "Trạng thái đơn hàng vừa thay đổi. Vui lòng kiểm tra lại thông tin mới nhất.";
  }

  if (error.code === 1006 || error.status === 401) {
    return "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.";
  }

  if (error.status === 400) {
    return "Thông tin yêu cầu chưa hợp lệ. Vui lòng kiểm tra lại.";
  }

  if (error.status === 503) {
    return "Hệ thống đang bận. Vui lòng thử lại sau ít phút.";
  }

  return "Không thể tải thông tin đơn hàng lúc này. Vui lòng thử lại sau.";
}

export function shouldRefreshOrderAfterError(error: OrderErrorLike): boolean {
  return error.code === 1069 || error.status === 409;
}
