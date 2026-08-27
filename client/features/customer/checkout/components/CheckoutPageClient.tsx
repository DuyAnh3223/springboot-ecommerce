"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import type { FormEvent } from "react";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Check, Loader2, MapPin, Tag, Truck, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { formatCurrency } from "@/shared/utils";
import type { AddressResponse } from "@/features/users/address.type";
import type { CheckoutResponse } from "@/features/orders/order.type";
import { useCartHydration } from "@/features/customer/cart/components/CartInitializer";
import { useAsyncAction } from "@/shared/hooks/useAsyncAction";
import {
  createCheckoutOrderAction,
  reviewCheckoutAction,
} from "../actions/checkout.actions";
import { checkoutFormSchema } from "../schemas/checkout.schema";
import type {
  CheckoutActionResult,
  CheckoutFormValues,
  IdempotencyAttempt,
} from "../types/checkout.types";
import {
  buildCreateCheckoutOrderRequest,
  getCreateFailureResolution,
  getOrCreateIdempotencyAttempt,
  getReviewIssueMessages,
  normalizeVoucherCode,
} from "../utils/checkout.utils";

interface CheckoutPageClientProps {
  selectedSkuIds: number[];
  initialReview: CheckoutResponse | null;
  initialReviewError?: string | null;
  addresses: AddressResponse[];
  addressError?: string | null;
}

const DEFAULT_NEW_ADDRESS: CheckoutFormValues["newAddress"] = {
  recipientName: "",
  phone: "",
  province: "",
  ward: "",
  street: "",
  saveAddress: false,
};

function getDefaultAddress(addresses: AddressResponse[]): AddressResponse | undefined {
  return addresses.find((address) => address.isDefault) || addresses[0];
}

function formatAddress(address: AddressResponse): string {
  return [address.street, address.ward, address.province].filter(Boolean).join(", ");
}

function ReviewLoading({ message = "Đang cập nhật thông tin checkout..." }: { message?: string }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-8 text-center shadow-sm">
      <Loader2 className="mx-auto h-8 w-8 animate-spin text-shop_light_green" />
      <p className="mt-3 text-sm text-slate-600">{message}</p>
    </div>
  );
}

function ReviewSummary({ review }: { review: CheckoutResponse }) {
  const issues = getReviewIssueMessages(review);

  return (
    <section className="space-y-4 rounded-2xl border border-slate-100 bg-white p-5 shadow-sm md:p-6">
      <div className="flex items-start justify-between gap-4 border-b border-slate-100 pb-4">
        <div>
          <p className="text-xs font-bold uppercase tracking-[0.18em] text-shop_light_green">
            Bước 1
          </p>
          <h2 className="mt-1 text-xl font-black text-slate-900">Kiểm tra đơn hàng</h2>
        </div>
        <Truck className="h-6 w-6 shrink-0 text-shop_light_green" />
      </div>

      <div className="space-y-3">
        {review.items.map((item) => (
          <div
            key={item.skuId}
            className="flex gap-3 rounded-xl border border-slate-100 bg-slate-50/60 p-3"
          >
            <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-lg bg-white text-xs font-bold text-slate-400">
              SKU
            </div>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-bold text-slate-800">{item.productName}</p>
              <p className="mt-1 text-xs text-slate-500">
                {item.skuCode} · {item.quantity} sản phẩm · {formatCurrency(item.unitPrice)}
              </p>
              {item.issueCode && (
                <p className="mt-1 text-xs font-semibold text-rose-600">
                  Sản phẩm này cần được kiểm tra lại.
                </p>
              )}
            </div>
            <p className="shrink-0 text-sm font-black text-slate-900">
              {formatCurrency(item.lineTotal)}
            </p>
          </div>
        ))}
      </div>

      {review.voucher?.code && (
        <div
          className={`flex items-center gap-2 rounded-xl border p-3 text-sm ${
            review.voucher.applicable
              ? "border-emerald-200 bg-emerald-50 text-emerald-800"
              : "border-rose-200 bg-rose-50 text-rose-700"
          }`}
        >
          {review.voucher.applicable ? <Check className="h-4 w-4" /> : <X className="h-4 w-4" />}
          <span>
            Voucher <strong>{review.voucher.code}</strong>{" "}
            {review.voucher.applicable ? "đã được áp dụng." : "chưa đủ điều kiện áp dụng."}
          </span>
        </div>
      )}

      {issues.length > 0 && (
        <div className="rounded-xl border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900">
          <p className="font-bold">Chưa thể đặt hàng</p>
          <ul className="mt-1 list-disc space-y-1 pl-5">
            {issues.map((issue) => (
              <li key={issue}>{issue}</li>
            ))}
          </ul>
        </div>
      )}

      <dl className="space-y-3 border-t border-slate-100 pt-4 text-sm">
        <div className="flex justify-between gap-4 text-slate-600">
          <dt>Tạm tính</dt>
          <dd className="font-semibold text-slate-800">{formatCurrency(review.subtotal)}</dd>
        </div>
        <div className="flex justify-between gap-4 text-slate-600">
          <dt>Phí vận chuyển</dt>
          <dd className="font-semibold text-slate-800">{formatCurrency(review.shippingFee)}</dd>
        </div>
        {review.discountAmount > 0 && (
          <div className="flex justify-between gap-4 text-emerald-700">
            <dt>Giảm giá</dt>
            <dd className="font-semibold">-{formatCurrency(review.discountAmount)}</dd>
          </div>
        )}
        <div className="flex justify-between gap-4 border-t border-slate-100 pt-3 text-base font-black text-slate-900">
          <dt>Tổng thanh toán</dt>
          <dd className="text-xl text-shop_dark_green">{formatCurrency(review.totalAmount)}</dd>
        </div>
      </dl>
    </section>
  );
}

export function CheckoutPageClient({
  selectedSkuIds,
  initialReview,
  initialReviewError,
  addresses,
  addressError,
}: CheckoutPageClientProps) {
  const router = useRouter();
  const { isHydrated, guestMergeStatus } = useCartHydration();
  const defaultAddress = getDefaultAddress(addresses);
  const { run: runReview, isLoading: isReviewLoading } = useAsyncAction();
  const { run: runSubmit, isLoading: isSubmitLoading } = useAsyncAction();
  const attemptRef = useRef<IdempotencyAttempt | null>(null);
  const reviewStartedRef = useRef(false);
  const [review, setReview] = useState<CheckoutResponse | null>(initialReview);
  const [reviewError, setReviewError] = useState<string | null>(initialReviewError || null);
  const [voucherError, setVoucherError] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [requiresReconfirmation, setRequiresReconfirmation] = useState(false);

  const form = useForm<CheckoutFormValues>({
    resolver: zodResolver(checkoutFormSchema),
    defaultValues: {
      addressMode: defaultAddress ? "EXISTING" : "NEW",
      addressId: defaultAddress?.id,
      newAddress: DEFAULT_NEW_ADDRESS,
      voucherCode: "",
    },
  });

  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors },
  } = form;
  const addressMode = useWatch({ control: form.control, name: "addressMode" });
  const voucherCode = useWatch({ control: form.control, name: "voucherCode" });

  const refreshReview = useCallback(
    async (code?: string | null): Promise<CheckoutActionResult<CheckoutResponse> | null> => {
      const result = await runReview(() =>
        reviewCheckoutAction({
          selectedSkuIds,
          voucherCode: normalizeVoucherCode(code),
        }),
      );

      if (!result) {
        setReviewError("Không thể kết nối đến hệ thống checkout. Vui lòng thử lại sau.");
        return null;
      }

      if (!result.success) {
        setReviewError(result.error.message);
        return result;
      }

      setReview(result.data);
      setReviewError(null);
      setSubmitError(null);
      setRequiresReconfirmation(false);
      return result;
    },
    [runReview, selectedSkuIds, setRequiresReconfirmation, setReview, setReviewError, setSubmitError],
  );

  useEffect(() => {
    if (
      !isHydrated ||
      guestMergeStatus === "unknown" ||
      guestMergeStatus === "pending" ||
      guestMergeStatus === "failed"
    ) return;
    if (reviewStartedRef.current) return;

    reviewStartedRef.current = true;
    void refreshReview();
  }, [guestMergeStatus, isHydrated, refreshReview]);

  const handleApplyVoucher = async () => {
    const normalized = normalizeVoucherCode(voucherCode);
    if (!normalized) {
      setVoucherError("Vui lòng nhập mã voucher.");
      return;
    }

    setVoucherError(null);
    const result = await refreshReview(normalized);
    if (!result || !result.success) return;

    setValue("voucherCode", normalized, { shouldValidate: true });
    if (!result.data.voucher?.applicable) {
      setVoucherError("Voucher chưa đủ điều kiện áp dụng. Bạn có thể sửa hoặc gỡ mã này.");
    }
  };

  const handleRemoveVoucher = async () => {
    setVoucherError(null);
    const result = await refreshReview();
    if (result?.success) {
      setValue("voucherCode", "", { shouldValidate: true });
    }
  };

  const handleSubmitOrder = async (values: CheckoutFormValues) => {
    if (!review) {
      setSubmitError("Checkout chưa có thông tin mới nhất. Vui lòng thử tải lại.");
      return;
    }
    if (!review.canPlaceOrder) {
      setSubmitError("Vui lòng xử lý các điều kiện checkout trước khi đặt hàng.");
      return;
    }
    if (requiresReconfirmation) {
      setSubmitError("Vui lòng xác nhận thông tin checkout mới nhất trước khi đặt hàng.");
      return;
    }

    const payload = buildCreateCheckoutOrderRequest(review, values);
    const attempt = getOrCreateIdempotencyAttempt(
      attemptRef.current,
      payload,
      () => crypto.randomUUID(),
    );
    attemptRef.current = attempt;
    setSubmitError(null);

    const result = await runSubmit(() =>
      createCheckoutOrderAction(payload, attempt.idempotencyKey),
    );

    if (!result) {
      setSubmitError("Không nhận được phản hồi từ hệ thống. Bạn có thể thử lại với cùng yêu cầu.");
      return;
    }

    if (!result.success) {
      const failureResolution = getCreateFailureResolution(result.error);
      if (failureResolution === "RECONFIRM_LATEST_REVIEW" && result.error.latestReview) {
        setReview(result.error.latestReview);
        setRequiresReconfirmation(true);
        setSubmitError(result.error.message);
        return;
      }

      if (failureResolution === "REFRESH_BEFORE_NEW_ATTEMPT") {
        attemptRef.current = null;
        setSubmitError(result.error.message);
        await refreshReview(normalizeVoucherCode(values.voucherCode));
        return;
      }

      setSubmitError(result.error.message);
      return;
    }

    router.push(`/checkout/success?orderCode=${encodeURIComponent(result.data.orderCode)}`);
  };

  if (!isHydrated || guestMergeStatus === "unknown" || guestMergeStatus === "pending") {
    return <ReviewLoading message="Đang đồng bộ giỏ hàng trước khi mở checkout..." />;
  }

  if (guestMergeStatus === "failed") {
    return (
      <div className="rounded-2xl border border-amber-200 bg-amber-50 p-6 text-center">
        <h2 className="text-lg font-bold text-amber-950">Chưa thể đồng bộ giỏ hàng</h2>
        <p className="mt-2 text-sm text-amber-800">
          Một số sản phẩm địa phương chưa được đồng bộ. Vui lòng quay lại giỏ hàng để thử lại.
        </p>
        <Link
          href="/cart"
          className="mt-4 inline-flex h-9 items-center justify-center rounded-lg bg-amber-700 px-4 text-sm font-medium text-white hover:bg-amber-800"
        >
          Quay lại giỏ hàng
        </Link>
      </div>
    );
  }

  if (reviewError && !review) {
    return (
      <div className="rounded-2xl border border-rose-200 bg-rose-50 p-6 text-center">
        <h2 className="text-lg font-bold text-rose-950">Không thể tải checkout</h2>
        <p className="mt-2 text-sm text-rose-800">{reviewError}</p>
        <Button
          type="button"
          className="mt-4 bg-rose-700 text-white hover:bg-rose-800"
          onClick={() => {
            reviewStartedRef.current = false;
            void refreshReview(voucherCode);
          }}
        >
          Thử lại
        </Button>
      </div>
    );
  }

  if (!review) {
    return <ReviewLoading />;
  }

  const addressModeRegistration = register("addressMode");
  const canSubmit = review.canPlaceOrder && !isReviewLoading && !isSubmitLoading;
  const handleFormSubmit = (event: FormEvent<HTMLFormElement>) => {
    void handleSubmit(handleSubmitOrder)(event);
  };

  return (
    <form onSubmit={handleFormSubmit} className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_380px]">
      <div className="space-y-6">
        <ReviewSummary review={review} />

        <section className="space-y-5 rounded-2xl border border-slate-100 bg-white p-5 shadow-sm md:p-6">
          <div className="border-b border-slate-100 pb-4">
            <p className="text-xs font-bold uppercase tracking-[0.18em] text-shop_light_green">
              Bước 2
            </p>
            <h2 className="mt-1 text-xl font-black text-slate-900">Địa chỉ nhận hàng</h2>
          </div>

          {addressError && (
            <p className="rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-800">
              {addressError} Bạn vẫn có thể nhập địa chỉ mới.
            </p>
          )}

          <fieldset className="space-y-3">
            <legend className="sr-only">Chọn hình thức địa chỉ</legend>
            {addresses.length > 0 && (
              <label className="flex cursor-pointer items-center gap-3 rounded-xl border border-slate-200 p-3 has-[:checked]:border-shop_light_green has-[:checked]:bg-emerald-50/40">
                <input
                  {...addressModeRegistration}
                  type="radio"
                  value="EXISTING"
                  checked={addressMode === "EXISTING"}
                  onChange={(event) => {
                    setValue("addressMode", event.target.value as "EXISTING" | "NEW", {
                      shouldValidate: true,
                    });
                    setValue("addressId", defaultAddress?.id, { shouldValidate: true });
                  }}
                />
                <span className="text-sm font-semibold text-slate-800">Dùng địa chỉ đã lưu</span>
              </label>
            )}
            <label className="flex cursor-pointer items-center gap-3 rounded-xl border border-slate-200 p-3 has-[:checked]:border-shop_light_green has-[:checked]:bg-emerald-50/40">
              <input
                {...addressModeRegistration}
                type="radio"
                value="NEW"
                checked={addressMode === "NEW"}
                onChange={(event) => {
                  setValue("addressMode", event.target.value as "EXISTING" | "NEW", {
                    shouldValidate: true,
                  });
                  setValue("addressId", undefined, { shouldValidate: true });
                }}
              />
              <span className="text-sm font-semibold text-slate-800">Nhập địa chỉ mới</span>
            </label>
          </fieldset>

          {addressMode === "EXISTING" && addresses.length > 0 ? (
            <div className="space-y-2">
              <Label htmlFor="checkout-address">Địa chỉ đã lưu</Label>
              <select
                id="checkout-address"
                {...register("addressId")}
                className="h-11 w-full rounded-lg border border-input bg-white px-3 text-sm outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
              >
                <option value="">Chọn địa chỉ</option>
                {addresses.map((address) => (
                  <option key={address.id} value={address.id}>
                    {address.recipientName} - {formatAddress(address)}
                  </option>
                ))}
              </select>
              {errors.addressId?.message && (
                <p className="text-xs text-destructive">{errors.addressId.message}</p>
              )}
            </div>
          ) : (
            <div className="space-y-4 rounded-xl border border-slate-100 bg-slate-50/60 p-4">
              <div className="grid gap-4 sm:grid-cols-2">
                <div className="space-y-2">
                  <Label htmlFor="recipientName">Tên người nhận</Label>
                  <Input id="recipientName" {...register("newAddress.recipientName")} />
                  {errors.newAddress?.recipientName?.message && (
                    <p className="text-xs text-destructive">{errors.newAddress.recipientName.message}</p>
                  )}
                </div>
                <div className="space-y-2">
                  <Label htmlFor="phone">Số điện thoại</Label>
                  <Input id="phone" inputMode="tel" {...register("newAddress.phone")} />
                  {errors.newAddress?.phone?.message && (
                    <p className="text-xs text-destructive">{errors.newAddress.phone.message}</p>
                  )}
                </div>
              </div>
              <div className="grid gap-4 sm:grid-cols-2">
                <div className="space-y-2">
                  <Label htmlFor="province">Tỉnh/Thành phố</Label>
                  <Input id="province" {...register("newAddress.province")} />
                  {errors.newAddress?.province?.message && (
                    <p className="text-xs text-destructive">{errors.newAddress.province.message}</p>
                  )}
                </div>
                <div className="space-y-2">
                  <Label htmlFor="ward">Phường/Xã</Label>
                  <Input id="ward" {...register("newAddress.ward")} />
                  {errors.newAddress?.ward?.message && (
                    <p className="text-xs text-destructive">{errors.newAddress.ward.message}</p>
                  )}
                </div>
              </div>
              <div className="space-y-2">
                <Label htmlFor="street">Địa chỉ chi tiết</Label>
                <Input id="street" {...register("newAddress.street")} />
                {errors.newAddress?.street?.message && (
                  <p className="text-xs text-destructive">{errors.newAddress.street.message}</p>
                )}
              </div>
              <label className="flex items-center gap-2 text-sm text-slate-700">
                <input type="checkbox" {...register("newAddress.saveAddress")} />
                Lưu địa chỉ này cho lần mua sau
              </label>
            </div>
          )}
        </section>
      </div>

      <aside className="space-y-6 lg:sticky lg:top-24 lg:self-start">
        <section className="space-y-4 rounded-2xl border border-slate-100 bg-white p-5 shadow-sm md:p-6">
          <div className="flex items-center justify-between border-b border-slate-100 pb-4">
            <h2 className="text-lg font-black text-slate-900">Mã ưu đãi</h2>
            <Tag className="h-5 w-5 text-shop_light_green" />
          </div>
          <div className="flex gap-2">
            <Input
              aria-label="Mã voucher"
              placeholder="Nhập mã voucher"
              className="uppercase"
              {...register("voucherCode")}
              onKeyDown={(event) => {
                if (event.key === "Enter") {
                  event.preventDefault();
                  void handleApplyVoucher();
                }
              }}
            />
            <Button
              type="button"
              disabled={isReviewLoading || !voucherCode?.trim()}
              onClick={() => void handleApplyVoucher()}
              className="shrink-0 bg-slate-900 text-white hover:bg-slate-800"
            >
              {isReviewLoading ? <Loader2 className="h-4 w-4 animate-spin" /> : "Áp dụng"}
            </Button>
          </div>
          {review.voucher?.code && (
            <Button
              type="button"
              variant="ghost"
              onClick={() => void handleRemoveVoucher()}
              className="h-auto px-0 text-xs font-semibold text-rose-600 hover:bg-transparent hover:text-rose-700"
            >
              <X className="mr-1 h-3.5 w-3.5" /> Gỡ voucher và cập nhật lại
            </Button>
          )}
          {voucherError && <p className="text-xs font-medium text-rose-600">{voucherError}</p>}
        </section>

        <section className="space-y-4 rounded-2xl border border-slate-100 bg-white p-5 shadow-sm md:p-6">
          <div className="border-b border-slate-100 pb-4">
            <p className="text-xs font-bold uppercase tracking-[0.18em] text-shop_light_green">
              Bước 3
            </p>
            <h2 className="mt-1 text-xl font-black text-slate-900">Thanh toán</h2>
          </div>
          <div className="flex items-start gap-3 rounded-xl border border-shop_light_green bg-emerald-50/40 p-3">
            <input type="radio" checked readOnly aria-label="Thanh toán khi nhận hàng" />
            <div>
              <p className="text-sm font-bold text-slate-800">Thanh toán khi nhận hàng (COD)</p>
              <p className="mt-1 text-xs text-slate-600">Bạn thanh toán khi nhận được sản phẩm.</p>
            </div>
          </div>

          {submitError && (
            <div className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-sm text-rose-800">
              {submitError}
            </div>
          )}

          {requiresReconfirmation && (
            <div className="space-y-3 rounded-xl border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900">
              <p className="font-bold">Checkout vừa thay đổi</p>
              <p>Hãy kiểm tra lại tổng tiền và xác nhận trước khi đặt hàng.</p>
              <Button
                type="button"
                className="w-full bg-amber-700 text-white hover:bg-amber-800"
                onClick={() => {
                  attemptRef.current = null;
                  setRequiresReconfirmation(false);
                  setSubmitError(null);
                }}
              >
                Tôi xác nhận thông tin mới
              </Button>
            </div>
          )}

          <Button
            type="submit"
            disabled={!canSubmit || requiresReconfirmation}
            className="h-12 w-full rounded-xl bg-shop_light_green text-base font-bold text-white hover:bg-shop_dark_green disabled:cursor-not-allowed disabled:opacity-50"
          >
            {isSubmitLoading ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" /> Đang tạo đơn hàng...
              </>
            ) : (
              "Đặt hàng COD"
            )}
          </Button>

          <div className="flex items-start gap-2 text-xs text-slate-500">
            <MapPin className="mt-0.5 h-4 w-4 shrink-0 text-shop_light_green" />
            <span>Thông tin đơn hàng được xác nhận lại an toàn trên hệ thống trước khi tạo đơn.</span>
          </div>
        </section>
      </aside>
    </form>
  );
}
