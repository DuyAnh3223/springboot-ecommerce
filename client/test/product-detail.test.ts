import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";
import type { CatalogProductDetail } from "../features/customer/catalog/types/catalog.types.ts";
import {
  buildCartItemDetails,
  clampProductQuantity,
  getInitialVariantSelection,
  getProductDetailPath,
  getVariantOptions,
  resolveSelectedSku,
  selectVariantValue,
} from "../features/customer/catalog/utils/product-detail.utils.ts";

const product: CatalogProductDetail = {
  id: 101,
  name: "Bàn phím Gaming",
  slug: "ban-phim-gaming",
  description: "Bàn phím có nhiều phiên bản.",
  primaryImageUrl: "/product.webp",
  rating: null,
  reviewCount: 0,
  category: { id: 7, name: "Bàn phím", slug: "ban-phim" },
  brand: { id: 3, name: "AB", slug: "ab" },
  attributes: { layout: "TKL" },
  specificationDefinitions: [],
  variantDefinitions: [
    { code: "color", name: "Màu sắc", dataType: "ENUM", unit: null, sortOrder: 1 },
    { code: "switch", name: "Switch", dataType: "ENUM", unit: null, sortOrder: 2 },
  ],
  priceMin: 1_000_000,
  priceMax: 1_200_000,
  totalStock: 7,
  skus: [
    {
      id: 11,
      sku: "BLACK-RED",
      price: 1_000_000,
      stock: 5,
      currency: "VND",
      weightGram: 700,
      attributes: { switch: "Red", color: "Black" },
      primaryImageUrl: "/black-red.webp",
      images: [],
    },
    {
      id: 12,
      sku: "BLACK-BLUE",
      price: 1_100_000,
      stock: 0,
      currency: "VND",
      weightGram: 700,
      attributes: { color: "Black", switch: "Blue" },
      primaryImageUrl: "/black-blue.webp",
      images: [],
    },
    {
      id: 13,
      sku: "WHITE-BLUE",
      price: 1_200_000,
      stock: 2,
      currency: "VND",
      weightGram: 700,
      attributes: { color: "White", switch: "Blue" },
      primaryImageUrl: "/white-blue.webp",
      images: [],
    },
  ],
};

test("keeps multi-SKU selection explicit and auto-selects a single SKU", () => {
  assert.deepEqual(getInitialVariantSelection(product), {});

  const singleSkuProduct = { ...product, skus: [product.skus[0]] };
  assert.deepEqual(getInitialVariantSelection(singleSkuProduct), {
    color: "string:Black",
    switch: "string:Red",
  });
  assert.equal(resolveSelectedSku(singleSkuProduct, getInitialVariantSelection(singleSkuProduct))?.id, 11);
});

test("orders dimensions by metadata and derives deterministic option state", () => {
  const colorOptions = getVariantOptions(product, {}, 0);
  assert.deepEqual(
    colorOptions.map(({ label, available, outOfStock }) => ({ label, available, outOfStock })),
    [
      { label: "Black", available: true, outOfStock: false },
      { label: "White", available: true, outOfStock: false },
    ],
  );

  const blackSelection = { color: "string:Black" };
  const switchOptions = getVariantOptions(product, blackSelection, 1);
  assert.deepEqual(
    switchOptions.map(({ label, available, outOfStock }) => ({ label, available, outOfStock })),
    [
      { label: "Blue", available: true, outOfStock: true },
      { label: "Red", available: true, outOfStock: false },
    ],
  );
});

test("changing an earlier dimension clears incompatible later selections", () => {
  const selected = selectVariantValue(
    product.variantDefinitions,
    { color: "string:Black", switch: "string:Red" },
    "color",
    "White",
  );

  assert.deepEqual(selected, { color: "string:White" });
  assert.equal(resolveSelectedSku(product, selected), null);

  const complete = selectVariantValue(product.variantDefinitions, selected, "switch", "Blue");
  assert.equal(resolveSelectedSku(product, complete)?.id, 13);
});

test("distinguishes zero-stock variants and clamps quantity", () => {
  const zeroStockSelection = { color: "string:Black", switch: "string:Blue" };
  const zeroStockSku = resolveSelectedSku(product, zeroStockSelection);
  assert.equal(zeroStockSku?.id, 12);
  assert.equal(clampProductQuantity(2, zeroStockSku), 0);

  const inStockSku = product.skus[0];
  assert.equal(clampProductQuantity(-2, inStockSku), 1);
  assert.equal(clampProductQuantity(2.8, inStockSku), 2);
  assert.equal(clampProductQuantity(99, inStockSku), 5);
});

test("builds the canonical route and exact guest-cart display metadata", () => {
  assert.equal(getProductDetailPath("ban phim/gaming"), "/products/ban%20phim%2Fgaming");
  assert.deepEqual(buildCartItemDetails(product, product.skus[0]), {
    productName: "Bàn phím Gaming",
    imageUrl: "/black-red.webp",
    unitPrice: 1_000_000,
    skuCode: "BLACK-RED",
  });
});

test("keeps homepage and Quick View navigation on the canonical product route", () => {
  const homeCard = readFileSync(
    new URL("../app/(customers)/_components/home/components/HomeProductCard.tsx", import.meta.url),
    "utf8",
  );
  const quickView = readFileSync(
    new URL("../app/(customers)/_components/home/components/ProductQuickView.tsx", import.meta.url),
    "utf8",
  );

  assert.match(homeCard, /href=\{getProductDetailPath\(product\.slug\)\}/);
  assert.match(quickView, /href=\{getProductDetailPath\(product\.slug\)\}/);
  assert.doesNotMatch(quickView, /href=\{`\/category\/\$\{product\.slug\}`\}/);
});
