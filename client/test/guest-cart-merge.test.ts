import assert from "node:assert/strict";
import test from "node:test";

import {
  applyGuestMergeResult,
  getGuestMergeReasonMessage,
} from "../features/customer/cart/utils/merge-guest-cart.ts";
import {
  clearGuestCartMergeId,
  getGuestCartMergeId,
  getOrCreateGuestCartMergeId,
} from "../features/customer/cart/utils/guest-cart.utils.ts";
import type { CartItem } from "../features/customer/cart/types/cart.types";

const localItems: CartItem[] = [
  {
    productSkuId: 17,
    skuCode: "GOOD",
    productName: "Sản phẩm hợp lệ",
    imageUrl: "",
    quantity: 2,
    unitPrice: 100,
  },
  {
    productSkuId: 42,
    skuCode: "OUT",
    productName: "Sản phẩm hết hàng",
    imageUrl: "",
    quantity: 1,
    unitPrice: 200,
  },
];

test("chỉ xóa item MERGED và giữ nguyên quantity item REJECTED", () => {
  const result = applyGuestMergeResult(localItems, [
    {
      skuId: 17,
      requestedQuantity: 2,
      mergedQuantity: 2,
      status: "MERGED",
      reasonCode: null,
    },
    {
      skuId: 42,
      requestedQuantity: 1,
      mergedQuantity: 0,
      status: "REJECTED",
      reasonCode: "INSUFFICIENT_STOCK",
    },
  ]);

  assert.deepEqual(result.retainedItems.map((item) => [item.productSkuId, item.quantity]), [[42, 1]]);
  assert.equal(result.notices[0]?.message, "Số lượng sản phẩm trong kho không đủ.");
  assert.equal(result.allResultsApplied, true);
});

test("thiếu result không được coi là đã áp dụng toàn bộ local cart", () => {
  const result = applyGuestMergeResult(localItems, [
    {
      skuId: 17,
      requestedQuantity: 2,
      mergedQuantity: 2,
      status: "MERGED",
      reasonCode: null,
    },
  ]);

  assert.deepEqual(result.retainedItems.map((item) => item.productSkuId), [42]);
  assert.equal(result.allResultsApplied, false);
});

test("reason code không xác định vẫn hiển thị thông báo tiếng Việt", () => {
  assert.equal(
    getGuestMergeReasonMessage("UNKNOWN"),
    "Sản phẩm chưa được đồng bộ vào tài khoản.",
  );
});

test("retry dùng lại cùng merge ID đã lưu trong localStorage", () => {
  const values = new Map<string, string>();
  const previousWindow = (globalThis as { window?: unknown }).window;
  const previousLocalStorage = (globalThis as { localStorage?: unknown }).localStorage;

  Object.assign(globalThis, {
    window: {},
    localStorage: {
      getItem: (key: string) => values.get(key) ?? null,
      setItem: (key: string, value: string) => values.set(key, value),
      removeItem: (key: string) => values.delete(key),
    },
  });

  try {
    const firstId = getOrCreateGuestCartMergeId();
    assert.equal(getGuestCartMergeId(), firstId);
    assert.equal(getOrCreateGuestCartMergeId(), firstId);
    clearGuestCartMergeId();
    assert.equal(getGuestCartMergeId(), null);
  } finally {
    if (previousWindow === undefined) {
      delete (globalThis as { window?: unknown }).window;
    } else {
      (globalThis as { window?: unknown }).window = previousWindow;
    }
    if (previousLocalStorage === undefined) {
      delete (globalThis as { localStorage?: unknown }).localStorage;
    } else {
      (globalThis as { localStorage?: unknown }).localStorage = previousLocalStorage;
    }
  }
});
