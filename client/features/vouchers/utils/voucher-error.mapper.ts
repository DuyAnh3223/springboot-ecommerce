export function mapVoucherError(error: unknown, fallbackMessage: string): string {
  if (!error || typeof error !== "object") {
    return fallbackMessage;
  }

  const err = error as {
    response?: {
      data?: {
        code?: number | string;
        message?: string;
      };
    };
  };

  const code = err.response?.data?.code;
  const message = err.response?.data?.message;

  // Check numeric code or symbolic code string
  const codeNum = typeof code === "number" ? code : parseInt(String(code || ""), 10);
  const msgStr = String(message || "");

  if (codeNum === 1020 || msgStr.includes("VOUCHER_DATE_INVALID")) {
    return "Thời gian không hợp lệ (ngày kết thúc phải sau ngày bắt đầu và ngày bắt đầu tạo mới phải ở tương lai).";
  }
  if (codeNum === 1021 || msgStr.includes("VOUCHER_VALUE_INVALID")) {
    return "Giá trị giảm giá không hợp lệ (phần trăm giảm phải nhỏ hơn 100% và lớn hơn 0).";
  }
  if (codeNum === 1022 || msgStr.includes("VOUCHER_NOT_FOUND")) {
    return "Mã giảm giá không tồn tại trên hệ thống.";
  }
  if (codeNum === 1023 || msgStr.includes("VOUCHER_EXISTED")) {
    return "Mã giảm giá này đã tồn tại trong hệ thống. Vui lòng chọn mã khác.";
  }
  if (codeNum === 1024 || msgStr.includes("VOUCHER_EXPIRED")) {
    return "Mã giảm giá đã hết hạn hoặc chưa đến thời gian có hiệu lực.";
  }
  if (codeNum === 1025 || msgStr.includes("VOUCHER_ARE_OUT")) {
    return "Mã giảm giá đã hết tổng số lượt sử dụng.";
  }
  if (codeNum === 1026 || msgStr.includes("VOUCHER_MIN_ORDER_VALUE_INVALID")) {
    return "Giá trị đơn hàng chưa đạt mức tối thiểu để áp dụng mã giảm giá này.";
  }
  if (codeNum === 1038 || msgStr.includes("VOUCHER_PER_USER_LIMIT_REACHED")) {
    return "Tài khoản của bạn đã dùng hết lượt cho phép của mã giảm giá này.";
  }
  if (codeNum === 1063 || msgStr.includes("VOUCHER_SCOPE_INVALID")) {
    return "Phạm vi áp dụng không hợp lệ (phải chọn ít nhất 1 SKU khi chọn áp dụng cho sản phẩm cụ thể).";
  }
  if (codeNum === 1064 || msgStr.includes("VOUCHER_MAX_DISCOUNT_INVALID")) {
    return "Trần giảm giá tối đa không hợp lệ (không áp dụng cho loại giảm số tiền cố định).";
  }
  if (codeNum === 1065 || msgStr.includes("VOUCHER_CODE_IMMUTABLE")) {
    return "Mã voucher là thuộc tính bất biến, không được phép thay đổi sau khi đã tạo.";
  }

  return fallbackMessage;
}
