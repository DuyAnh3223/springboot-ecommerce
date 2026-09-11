import test from "node:test";
import assert from "node:assert/strict";
import { canOpenPayment, paymentMessage } from "../features/payments/payment.utils.ts";
import { buildCreateCheckoutOrderRequest, getOrCreateIdempotencyAttempt } from "../features/customer/checkout/utils/checkout.utils.ts";
import type { OrderPayments } from "../features/payments/payment.type.ts";

function payment(overrides: Partial<OrderPayments> = {}): OrderPayments {
  return { orderCode: "ORD1", orderStatus: "PENDING", paymentMethod: "ONLINE", paymentStatus: "UNPAID", retryAllowed: false,
    attempts: [{ id: 1, provider: "MOMO", status: "PENDING", amount: 10000, paidAt: null, providerReference: null,
      initiationState: "UNKNOWN", applicationStatus: "NONE", reviewReason: null, paymentDeadline: null }], ...overrides };
}
test("unknown outcome does not offer another charge", () => {
  assert.equal(canOpenPayment(payment()), false);
  assert.match(paymentMessage(payment()), /không thanh toán thêm/);
});
test("late successful charge keeps cancellation and review visible", () => {
  const p = payment({ orderStatus: "CANCELLED", paymentStatus: "PAID" });
  p.attempts[0].applicationStatus = "REVIEW_REQUIRED";
  assert.equal(canOpenPayment(p), false); assert.match(paymentMessage(p), /kiểm tra/);
});
test("retry requires backend authorization and is blocked during review", () => {
  const p = payment({ retryAllowed: true }); p.attempts[0].status = "FAILED";
  assert.equal(canOpenPayment(p), true);
  p.attempts[0].reviewReason = "MISMATCH"; assert.equal(canOpenPayment(p), false);
});
test("changing provider changes checkout idempotency payload", () => {
  const review = { items: [], subtotal: 10000, eligibleSubtotal: 10000, shippingFee: 0, discountAmount: 0, totalAmount: 10000, voucher: null, canPlaceOrder: true };
  const values = { addressMode: "EXISTING" as const, addressId: "address", paymentProvider: "MOMO" as const };
  const momo = buildCreateCheckoutOrderRequest(review, values);
  const vnpay = buildCreateCheckoutOrderRequest(review, { ...values, paymentProvider: "VNPAY" });
  assert.equal(momo.paymentMethod, "ONLINE"); assert.equal(momo.paymentProvider, "MOMO");
  const first = getOrCreateIdempotencyAttempt(null, momo, () => "first");
  assert.equal(getOrCreateIdempotencyAttempt(first, vnpay, () => "second").idempotencyKey, "second");
});
