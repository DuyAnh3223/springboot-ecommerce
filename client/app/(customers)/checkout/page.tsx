import Link from "next/link";
import { getPaymentProviders } from "@/features/payments/services/payment.service";
import { redirect } from "next/navigation";
import { getUserSession } from "@/features/auth/actions";
import { getAddresses } from "@/features/users/services/address.service";
import { reviewCheckoutAction } from "@/features/customer/checkout/actions/checkout.actions";
import { CheckoutPageClient } from "@/features/customer/checkout/components/CheckoutPageClient";
import {
  buildCheckoutUrl,
  buildSignInCallbackUrl,
  normalizeSelectedSkuIds,
  normalizeVoucherCode,
} from "@/features/customer/checkout/utils/checkout.utils";

interface CheckoutPageProps {
  searchParams: Promise<{ skuIds?: string | string[]; voucherCode?: string | string[] }>;
}

export const metadata = {
  title: "Thanh toán | ABTechZone",
  description: "Kiểm tra đơn hàng và đặt hàng COD tại ABTechZone.",
};

function EmptySelection() {
  return (
    <div className="rounded-2xl border border-amber-200 bg-amber-50 p-8 text-center">
      <h1 className="text-xl font-black text-amber-950">Chưa có sản phẩm để thanh toán</h1>
      <p className="mt-2 text-sm text-amber-800">
        Vui lòng quay lại giỏ hàng và chọn ít nhất một sản phẩm.
      </p>
      <Link
        href="/cart"
        className="mt-5 inline-flex h-10 items-center justify-center rounded-lg bg-amber-700 px-5 text-sm font-bold text-white hover:bg-amber-800"
      >
        Quay lại giỏ hàng
      </Link>
    </div>
  );
}

export default async function CheckoutPage({ searchParams }: CheckoutPageProps) {
  const params = await searchParams;
  const selectedSkuIds = normalizeSelectedSkuIds(params.skuIds);
  const rawSkuIds = Array.isArray(params.skuIds) ? params.skuIds.join(",") : params.skuIds || "";
  const rawVoucherCode = Array.isArray(params.voucherCode)
    ? params.voucherCode[0]
    : params.voucherCode;
  const voucherCode = normalizeVoucherCode(rawVoucherCode);

  if (selectedSkuIds.length === 0) {
    return (
      <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
        <EmptySelection />
      </div>
    );
  }

  if (rawSkuIds !== selectedSkuIds.join(",")) {
    redirect(buildCheckoutUrl(selectedSkuIds, voucherCode));
  }

  const checkoutUrl = buildCheckoutUrl(selectedSkuIds, voucherCode);
  const session = await getUserSession();
  if (!session) {
    redirect(buildSignInCallbackUrl(checkoutUrl));
  }

  const [addressResult, paymentProviders] = await Promise.all([
    getAddresses({ size: 50, sortBy: "id", order: "desc" }).catch(() => null),
    getPaymentProviders().catch(() => []),
  ]);

  const addresses = addressResult?.content || [];
  const defaultAddress = addresses.find((address) => address.isDefault) || addresses[0];
  const reviewResult = await reviewCheckoutAction({
    selectedSkuIds,
    voucherCode,
    addressId: defaultAddress?.id,
  });

  if (!reviewResult.success && reviewResult.error.status === 401) {
    redirect(buildSignInCallbackUrl(checkoutUrl));
  }

  const addressError = addressResult
    ? null
    : "Không thể tải danh sách địa chỉ đã lưu.";

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="mb-6">
        <p className="text-sm font-semibold text-shop_light_green">ABTechZone</p>
        <h1 className="mt-1 text-3xl font-black text-slate-900">Thanh toán đơn hàng</h1>
        <p className="mt-2 text-sm text-slate-500">
          Kiểm tra thông tin trước khi đặt hàng. Giá và điều kiện đặt hàng được xác nhận từ hệ thống.
        </p>
      </div>

      <CheckoutPageClient
        paymentProviders={paymentProviders}
        selectedSkuIds={selectedSkuIds}
        initialVoucherCode={voucherCode}
        initialReview={reviewResult.success ? reviewResult.data : null}
        initialReviewError={reviewResult.success ? null : reviewResult.error.message}
        addresses={addresses}
        addressError={addressError}
      />
    </div>
  );
}
