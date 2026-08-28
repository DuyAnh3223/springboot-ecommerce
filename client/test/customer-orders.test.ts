import assert from "node:assert/strict";
import test from "node:test";
import { cancelOrderSchema } from "../features/customer/orders/schemas/cancel-order.schema.ts";
import {
  getOrderErrorMessage,
  shouldRefreshOrderAfterError,
} from "../features/orders/order-error.ts";
import {
  ORDER_STATUS_META,
  buildOrderListUrl,
  canCancelOrder,
  getHistoryTargetStatus,
  getPaymentMethodLabel,
  getPaymentStatusLabel,
  parseOrderListQuery,
} from "../features/customer/orders/utils/order.utils.ts";

test("maps exactly the five backend order statuses to Vietnamese metadata", () => {
  assert.deepEqual(Object.keys(ORDER_STATUS_META), [
    "PENDING",
    "CONFIRMED",
    "SHIPPING",
    "DELIVERED",
    "CANCELLED",
  ]);
  assert.equal(ORDER_STATUS_META.SHIPPING.label, "Đang giao hàng");
  assert.equal("SHIPPED" in ORDER_STATUS_META, false);
  assert.equal("REFUNDED" in ORDER_STATUS_META, false);
});

test("parses safe URL query values and converts UI page to backend page", () => {
  assert.deepEqual(
    parseOrderListQuery({ status: "SHIPPING", page: "2", size: "20" }),
    { status: "SHIPPING", page: 2, apiPage: 1, size: 20 },
  );
  assert.deepEqual(
    parseOrderListQuery({ status: "SHIPPED", page: "-4", size: "999" }),
    { status: undefined, page: 1, apiPage: 0, size: 10 },
  );
});

test("builds a canonical order history URL for filters and pagination", () => {
  assert.equal(
    buildOrderListUrl({ status: "CANCELLED", page: 3, size: 20 }),
    "/profile/orders?status=CANCELLED&page=3&size=20",
  );
  assert.equal(
    buildOrderListUrl({ status: undefined, page: 1, size: 10 }),
    "/profile/orders",
  );
});

test("shows cancellation only from backend allowedTransitions", () => {
  assert.equal(canCancelOrder(["CANCELLED"]), true);
  assert.equal(canCancelOrder([]), false);
  assert.equal(canCancelOrder(["CONFIRMED", "SHIPPING"]), false);
});

test("validates and trims cancellation reason using the backend 500-char limit", () => {
  const valid = cancelOrderSchema.safeParse({ reason: "  Tôi đặt nhầm sản phẩm  " });
  assert.equal(valid.success, true);
  if (valid.success) assert.equal(valid.data.reason, "Tôi đặt nhầm sản phẩm");

  assert.equal(cancelOrderSchema.safeParse({ reason: "   " }).success, false);
  assert.equal(cancelOrderSchema.safeParse({ reason: "x".repeat(501) }).success, false);
});

test("uses canonical history target with a legacy status fallback", () => {
  assert.equal(getHistoryTargetStatus({ toStatus: "CONFIRMED", status: "PENDING" }), "CONFIRMED");
  assert.equal(getHistoryTargetStatus({ toStatus: null, status: "PENDING" }), "PENDING");
});

test("maps payment and backend failures to Vietnamese guidance", () => {
  assert.equal(getPaymentMethodLabel("COD"), "Thanh toán khi nhận hàng (COD)");
  assert.equal(getPaymentStatusLabel("UNPAID"), "Chưa thanh toán");
  assert.equal(getPaymentStatusLabel("PAID"), "Đã thanh toán");
  assert.equal(getPaymentStatusLabel("CANCELLED"), "Đã hủy thanh toán");
  assert.equal(
    getOrderErrorMessage({ code: 1069, status: 409 }),
    "Trạng thái đơn hàng vừa thay đổi. Vui lòng kiểm tra lại thông tin mới nhất.",
  );
  assert.equal(
    getOrderErrorMessage({ code: 1034, status: 404 }),
    "Không tìm thấy đơn hàng hoặc bạn không có quyền xem đơn hàng này.",
  );
  assert.equal(shouldRefreshOrderAfterError({ code: 1069, status: 409 }), true);
  assert.equal(shouldRefreshOrderAfterError({ status: 400 }), false);
});
