"use client";

import Image from "next/image";
import { useState } from "react";
import { CheckCircle2, Minus, PackageCheck, Plus, ShoppingCart, Star } from "lucide-react";
import { useCart } from "@/features/customer/cart/hooks/useCart";
import type { CatalogProductDetail, CatalogProductImage } from "../../types/catalog.types";
import {
  buildCartItemDetails,
  clampProductQuantity,
  getInitialVariantSelection,
  getVariantOptions,
  resolveSelectedSku,
  selectNormalizedVariantValue,
} from "../../utils/product-detail.utils";

interface ProductDetailClientProps {
  product: CatalogProductDetail;
}

function formatMoney(value: number | null, currency = "VND") {
  if (value === null) return "Liên hệ";
  return new Intl.NumberFormat("vi-VN", { style: "currency", currency }).format(value);
}

function getGallery(product: CatalogProductDetail, selectedSkuId: number | null): CatalogProductImage[] {
  const sku = product.skus.find((item) => item.id === selectedSkuId);
  if (sku?.images.length) return sku.images;
  const fallback = sku?.primaryImageUrl || product.primaryImageUrl;
  return fallback
    ? [{ id: -1, url: fallback, altText: product.name, sortOrder: 0, primary: true }]
    : [];
}

export function ProductDetailClient({ product }: ProductDetailClientProps) {
  const { handleAddToCart, error, setError, isLoading } = useCart();
  const [selection, setSelection] = useState(() => getInitialVariantSelection(product));
  const [quantity, setQuantity] = useState(1);
  const [selectedImageUrl, setSelectedImageUrl] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);

  const selectedSku = resolveSelectedSku(product, selection);
  const gallery = getGallery(product, selectedSku?.id ?? null);
  const activeImage = gallery.some((image) => image.url === selectedImageUrl)
    ? selectedImageUrl
    : gallery[0]?.url || null;
  const validQuantity = clampProductQuantity(quantity, selectedSku);
  const busy = submitting || isLoading;
  const hasVariants = product.skus.length > 1 && product.variantDefinitions.length > 0;
  const canAdd = Boolean(selectedSku && selectedSku.stock > 0 && validQuantity > 0 && !busy);

  const displayPrice = selectedSku?.price ?? product.priceMin;
  const priceRange =
    !selectedSku && product.priceMin !== null && product.priceMax !== null && product.priceMax > product.priceMin;

  const handleVariantChange = (code: string, normalizedValue: string) => {
    setSelection((current) =>
      selectNormalizedVariantValue(product.variantDefinitions, current, code, normalizedValue),
    );
    setQuantity(1);
    setSelectedImageUrl(null);
    setNotice(null);
    setError(null);
  };

  const handleAdd = async () => {
    if (!selectedSku || !canAdd) return;

    setSubmitting(true);
    setNotice(null);
    setError(null);
    try {
      const added = await handleAddToCart(
        selectedSku.id,
        validQuantity,
        buildCartItemDetails(product, selectedSku),
      );
      if (added) setNotice(`Đã thêm ${validQuantity} sản phẩm vào giỏ hàng.`);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section className="grid gap-8 rounded-3xl border border-slate-200 bg-white p-4 shadow-sm sm:p-6 lg:grid-cols-2 lg:p-8">
      <div className="space-y-4">
        <div className="relative aspect-square overflow-hidden rounded-2xl border border-slate-200 bg-slate-50">
          {activeImage ? (
            <Image
              src={activeImage}
              alt={gallery.find((image) => image.url === activeImage)?.altText || product.name}
              fill
              priority
              sizes="(max-width: 1024px) 100vw, 50vw"
              className="object-contain p-4"
            />
          ) : (
            <div className="flex h-full items-center justify-center text-sm font-semibold text-slate-400">
              Chưa có hình ảnh
            </div>
          )}
        </div>

        {gallery.length > 1 && (
          <div className="grid grid-cols-5 gap-2" aria-label="Hình ảnh sản phẩm">
            {gallery.map((image) => (
              <button
                key={image.id}
                type="button"
                onClick={() => setSelectedImageUrl(image.url)}
                aria-label={`Xem ${image.altText || product.name}`}
                aria-pressed={activeImage === image.url}
                className={`relative aspect-square overflow-hidden rounded-xl border bg-white transition focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-600 ${
                  activeImage === image.url ? "border-emerald-600 ring-1 ring-emerald-600" : "border-slate-200"
                }`}
              >
                <Image src={image.url} alt="" fill sizes="120px" className="object-contain p-1" />
              </button>
            ))}
          </div>
        )}
      </div>

      <div className="flex flex-col">
        {product.brand && (
          <span className="mb-2 text-xs font-extrabold uppercase tracking-[0.18em] text-emerald-700">
            {product.brand.name}
          </span>
        )}
        <h1 className="text-2xl font-black leading-tight text-slate-950 sm:text-3xl">{product.name}</h1>

        <div className="mt-3 flex min-h-6 items-center gap-2 text-sm">
          {product.rating !== null && product.reviewCount > 0 ? (
            <>
              <Star className="h-4 w-4 fill-amber-400 text-amber-400" aria-hidden="true" />
              <span className="font-bold text-slate-800">{product.rating.toFixed(1)}</span>
              <span className="text-slate-500">({product.reviewCount} đánh giá)</span>
            </>
          ) : (
            <span className="text-slate-500">Chưa có đánh giá</span>
          )}
        </div>

        <div className="mt-5 rounded-2xl border border-emerald-100 bg-emerald-50/70 p-4">
          <div className="text-xs font-semibold uppercase tracking-wide text-slate-500">Giá sản phẩm</div>
          <div className="mt-1 text-2xl font-black text-emerald-800">
            {formatMoney(displayPrice, selectedSku?.currency)}
            {priceRange && <span className="text-lg"> – {formatMoney(product.priceMax)}</span>}
          </div>
          <div className="mt-2 flex items-center gap-2 text-sm font-semibold">
            <PackageCheck className="h-4 w-4 text-emerald-700" aria-hidden="true" />
            {!product.skus.length
              ? "Tạm hết hàng"
              : selectedSku
                ? selectedSku.stock > 0
                  ? `Còn ${selectedSku.stock} sản phẩm`
                  : "Hết hàng"
                : "Vui lòng chọn phiên bản"}
          </div>
          {selectedSku && <div className="mt-1 text-xs text-slate-500">Mã SKU: {selectedSku.sku}</div>}
        </div>

        {hasVariants && (
          <div className="mt-6 space-y-5">
            {product.variantDefinitions
              .slice()
              .sort((left, right) => left.sortOrder - right.sortOrder)
              .map((definition, index) => {
                const options = getVariantOptions(product, selection, index);
                return (
                  <fieldset key={definition.code}>
                    <legend className="mb-2 text-sm font-bold text-slate-800">{definition.name}</legend>
                    <div className="flex flex-wrap gap-2">
                      {options.map((option) => {
                        const selected = selection[definition.code] === option.value;
                        return (
                          <button
                            key={option.value}
                            type="button"
                            disabled={!option.available}
                            aria-pressed={selected}
                            onClick={() => handleVariantChange(definition.code, option.value)}
                            className={`rounded-xl border px-3 py-2 text-sm font-semibold transition focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-600 disabled:cursor-not-allowed disabled:opacity-35 ${
                              selected
                                ? "border-emerald-600 bg-emerald-50 text-emerald-800"
                                : "border-slate-300 bg-white text-slate-700 hover:border-emerald-500"
                            }`}
                          >
                            {option.label}
                            {option.outOfStock && <span className="ml-1 text-xs text-rose-600">· Hết hàng</span>}
                          </button>
                        );
                      })}
                    </div>
                  </fieldset>
                );
              })}
          </div>
        )}

        {product.skus.length > 1 && product.variantDefinitions.length === 0 && (
          <p className="mt-5 rounded-xl bg-amber-50 p-3 text-sm text-amber-800">
            Sản phẩm chưa có cấu hình biến thể hợp lệ để đặt mua.
          </p>
        )}

        {selectedSku && selectedSku.stock > 0 && (
          <div className="mt-6">
            <div className="mb-2 text-sm font-bold text-slate-800">Số lượng</div>
            <div className="inline-flex items-center rounded-xl border border-slate-300 bg-white">
              <button
                type="button"
                aria-label="Giảm số lượng"
                onClick={() => setQuantity((current) => clampProductQuantity(current - 1, selectedSku))}
                disabled={validQuantity <= 1 || busy}
                className="p-3 text-slate-600 hover:text-emerald-700 disabled:opacity-30"
              >
                <Minus className="h-4 w-4" />
              </button>
              <output aria-live="polite" className="min-w-12 text-center text-sm font-extrabold">
                {validQuantity}
              </output>
              <button
                type="button"
                aria-label="Tăng số lượng"
                onClick={() => setQuantity((current) => clampProductQuantity(current + 1, selectedSku))}
                disabled={validQuantity >= selectedSku.stock || busy}
                className="p-3 text-slate-600 hover:text-emerald-700 disabled:opacity-30"
              >
                <Plus className="h-4 w-4" />
              </button>
            </div>
          </div>
        )}

        <button
          type="button"
          onClick={handleAdd}
          disabled={!canAdd}
          className="mt-6 inline-flex w-full items-center justify-center gap-2 rounded-xl bg-emerald-600 px-5 py-3.5 text-sm font-extrabold text-white shadow-sm transition hover:bg-emerald-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-600 disabled:cursor-not-allowed disabled:bg-slate-300"
        >
          <ShoppingCart className="h-5 w-5" aria-hidden="true" />
          {busy ? "Đang thêm..." : selectedSku?.stock === 0 ? "Phiên bản đã hết hàng" : "Thêm vào giỏ hàng"}
        </button>

        <div aria-live="polite" className="mt-3 min-h-6 text-sm">
          {notice && (
            <p className="flex items-center gap-2 font-semibold text-emerald-700">
              <CheckCircle2 className="h-4 w-4" aria-hidden="true" /> {notice}
            </p>
          )}
          {error && <p className="font-semibold text-rose-600">{error}</p>}
        </div>

        {product.description && <p className="mt-4 text-sm leading-7 text-slate-600">{product.description}</p>}
      </div>
    </section>
  );
}
