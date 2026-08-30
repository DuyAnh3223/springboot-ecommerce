import test from "node:test";
import assert from "node:assert/strict";
import {
  ADMIN_ORDER_STATUS_META,
  buildAdminOrderUrl,
  getAllowedStatusActions,
  normalizeAdminNote,
  parseAdminOrderQuery,
  serializeAdminDate,
  validateAdminDateRange,
} from "../features/admin/orders/utils/admin-order.utils.ts";
import { adminOrderStatusSchema } from "../features/admin/orders/schemas/admin-order.schema.ts";
import { getAdminOrderErrorMessage, shouldRefreshAdminOrderAfterError } from "../features/admin/orders/utils/admin-order-error.utils.ts";

test("uses exactly the five backend statuses and Vietnamese labels", () => {
  assert.deepEqual(Object.keys(ADMIN_ORDER_STATUS_META), ["PENDING", "CONFIRMED", "SHIPPING", "DELIVERED", "CANCELLED"]);
  assert.equal(ADMIN_ORDER_STATUS_META.SHIPPING.label, "Đang giao");
});

test("parses safe URL filters and approved page sizes", () => {
  const query = parseAdminOrderQuery(new URLSearchParams("search=%20OC-1%20&status=SHIPPED&page=-2&size=99"));
  assert.deepEqual(query, { search: "OC-1", status: undefined, fromDate: undefined, toDate: undefined, page: 0, size: 20 });
});

test("serializes local date boundaries and rejects reversed ranges", () => {
  assert.equal(serializeAdminDate("2026-08-01"), "2026-08-01T00:00:00.000Z");
  assert.equal(serializeAdminDate("2026-08-01", true), "2026-08-01T23:59:59.999Z");
  assert.equal(validateAdminDateRange("2026-08-02", "2026-08-01"), "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
});

test("builds canonical filter and pagination URLs", () => {
  assert.equal(buildAdminOrderUrl({ search: " OC-1 ", status: "PENDING", page: 2, size: 50 }), "/admin/orders?search=OC-1&status=PENDING&page=2&size=50");
  assert.equal(buildAdminOrderUrl({ page: 0, size: 20 }), "/admin/orders");
});

test("shows only backend-authorized transition targets", () => {
  assert.deepEqual(getAllowedStatusActions(["CONFIRMED", "CANCELLED"]), [
    { status: "CONFIRMED", label: "Đã xác nhận" },
    { status: "CANCELLED", label: "Đã hủy" },
  ]);
});

test("trims notes and enforces cancellation reason", () => {
  assert.equal(normalizeAdminNote("  lý do  "), "lý do");
  assert.equal(adminOrderStatusSchema.safeParse({ status: "CANCELLED", note: " " }).success, false);
  assert.equal(adminOrderStatusSchema.safeParse({ status: "CONFIRMED", note: " ok " }).data?.note, "ok");
  assert.equal(adminOrderStatusSchema.safeParse({ status: "CONFIRMED", note: "x".repeat(501) }).success, false);
});

test("maps admin failures without exposing backend text", () => {
  const conflict = { message: "raw backend", code: 1069, status: 409 };
  assert.match(getAdminOrderErrorMessage(conflict), /thay đổi/);
  assert.equal(shouldRefreshAdminOrderAfterError(conflict), true);
  assert.match(getAdminOrderErrorMessage({ status: 403 }), /quyền/);
});
