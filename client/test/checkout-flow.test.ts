import assert from "node:assert/strict";
import test from "node:test";
import type { CheckoutResponse } from "../features/orders/order.type";
import {
  buildCheckoutUrl,
  buildCreateCheckoutOrderRequest,
  buildSignInCallbackUrl,
  getCreateFailureResolution,
  getCheckoutErrorMessage,
  getOrCreateIdempotencyAttempt,
  getReviewIssueMessages,
  normalizeSelectedSkuIds,
  normalizeVoucherCode,
} from "../features/customer/checkout/utils/checkout.utils.ts";

const review: CheckoutResponse = {
  items: [
    {
      skuId: 17,
      skuCode: "SKU-17",
      productName: "Keyboard",
      imageUrl: null,
      quantity: 2,
      unitPrice: 100000,
      lineTotal: 200000,
      availableStock: 5,
      issueCode: null,
    },
  ],
  subtotal: 200000,
  eligibleSubtotal: 200000,
  shippingFee: 30000,
  discountAmount: 20000,
  totalAmount: 210000,
  voucher: { code: "summer", applicable: true, issueCode: null },
  canPlaceOrder: true,
};

test("normalizes, deduplicates, and sorts selected SKU IDs", () => {
  assert.deepEqual(normalizeSelectedSkuIds(["42,17", "bad,0,17", "-3"]), [17, 42]);
});

test("builds a stable checkout URL and encoded sign-in callback", () => {
  const checkoutUrl = buildCheckoutUrl([42, 17, 17]);
  assert.equal(checkoutUrl, "/checkout?skuIds=17,42");
  assert.equal(
    buildSignInCallbackUrl(checkoutUrl),
    "/sign-in?callbackUrl=%2Fcheckout%3FskuIds%3D17%2C42",
  );
});

test("maps form values to the reviewed snapshot create contract", () => {
  const existingAddressRequest = buildCreateCheckoutOrderRequest(review, {
    addressMode: "EXISTING",
    addressId: "address-1",
    newAddress: {
      recipientName: "Unused",
      phone: "0000000000",
      province: "Unused",
      ward: "Unused",
      street: "Unused",
      saveAddress: false,
    },
  });

  assert.deepEqual(existingAddressRequest, {
    reviewedCheckout: {
      items: [{ skuId: 17, quantity: 2, unitPrice: 100000, lineTotal: 200000 }],
      subtotal: 200000,
      eligibleSubtotal: 200000,
      shippingFee: 30000,
      discountAmount: 20000,
      totalAmount: 210000,
      voucher: { code: "summer", applicable: true },
      canPlaceOrder: true,
    },
    addressId: "address-1",
    newUserAddress: null,
    paymentMethod: "COD",
  });

  const newAddressRequest = buildCreateCheckoutOrderRequest(review, {
    addressMode: "NEW",
    newAddress: {
      recipientName: "  Nguyen Van A ",
      phone: " 0900000000 ",
      province: " HCM ",
      ward: " Ward 1 ",
      street: " 1 Main Street ",
      saveAddress: true,
    },
  });
  assert.equal(newAddressRequest.addressId, null);
  assert.deepEqual(newAddressRequest.newUserAddress, {
    recipientName: "Nguyen Van A",
    phone: "0900000000",
    province: "HCM",
    ward: "Ward 1",
    street: "1 Main Street",
    saveAddress: true,
  });
});

test("reuses an idempotency key for the same payload and rotates it for a new payload", () => {
  let keyNumber = 0;
  const nextKey = () => `key-${++keyNumber}`;
  const payload = buildCreateCheckoutOrderRequest(review, {
    addressMode: "EXISTING",
    addressId: "address-1",
    newAddress: {
      recipientName: "Unused",
      phone: "0000000000",
      province: "Unused",
      ward: "Unused",
      street: "Unused",
      saveAddress: false,
    },
  });

  const first = getOrCreateIdempotencyAttempt(null, payload, nextKey);
  const retry = getOrCreateIdempotencyAttempt(first, payload, nextKey);
  const changed = getOrCreateIdempotencyAttempt(
    retry,
    { ...payload, paymentMethod: "COD", reviewedCheckout: { ...payload.reviewedCheckout, totalAmount: 220000 } },
    nextKey,
  );

  assert.equal(first.idempotencyKey, "key-1");
  assert.equal(retry.idempotencyKey, "key-1");
  assert.equal(changed.idempotencyKey, "key-2");
});

test("keeps voucher normalization and review issues customer-facing", () => {
  assert.equal(normalizeVoucherCode("  summer "), "SUMMER");
  assert.equal(normalizeVoucherCode("   "), undefined);

  assert.deepEqual(
    getReviewIssueMessages({
      ...review,
      canPlaceOrder: false,
      items: [{ ...review.items[0], issueCode: "INSUFFICIENT_STOCK" }],
      voucher: { code: "SUMMER", applicable: false, issueCode: "VOUCHER_EXPIRED" },
    }),
    ["Số lượng tồn kho hiện không đủ.", "Voucher đã hết hạn sử dụng."],
  );
  assert.equal(getCheckoutErrorMessage(1068), "Thông tin checkout đã thay đổi. Vui lòng xem lại trước khi đặt hàng.");
  assert.equal(
    getCreateFailureResolution({ code: 1068, message: "changed", latestReview: review }),
    "RECONFIRM_LATEST_REVIEW",
  );
  assert.equal(
    getCreateFailureResolution({ code: 1067, message: "reused" }),
    "REFRESH_BEFORE_NEW_ATTEMPT",
  );
  assert.equal(
    getCreateFailureResolution({ status: 503, message: "busy" }),
    "RETRY_SAME_ATTEMPT",
  );
});
