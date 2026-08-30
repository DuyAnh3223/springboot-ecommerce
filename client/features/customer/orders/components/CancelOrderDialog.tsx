"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { zodResolver } from "@hookform/resolvers/zod";
import { AlertTriangle, Loader2 } from "lucide-react";
import { useForm, useWatch } from "react-hook-form";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { cancelOrderAction } from "@/features/orders/actions/cancel-order.action";
import type { OrderStatus } from "@/features/orders/order.type";
import { useAsyncAction } from "@/shared/hooks";
import {
  cancelOrderSchema,
  type CancelOrderFormValues,
} from "../schemas/cancel-order.schema";
import { canCancelOrder } from "../utils/order.utils";

interface CancelOrderDialogProps {
  orderCode: string;
  allowedTransitions: OrderStatus[];
}

export function CancelOrderDialog({
  orderCode,
  allowedTransitions,
}: CancelOrderDialogProps) {
  const router = useRouter();
  const [isOpen, setIsOpen] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);
  const [confirmation, setConfirmation] = useState<string | null>(null);
  const { isLoading, run } = useAsyncAction();
  const {
    control,
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<CancelOrderFormValues>({
    resolver: zodResolver(cancelOrderSchema),
    defaultValues: { reason: "" },
  });
  const reason = useWatch({ control, name: "reason" }) ?? "";
  const isCancellationAllowed = canCancelOrder(allowedTransitions);

  const closeDialog = () => {
    if (isLoading) return;
    setIsOpen(false);
    setServerError(null);
    reset({ reason: "" });
  };

  const onSubmit = handleSubmit(async (values) => {
    setServerError(null);
    const result = await run(() => cancelOrderAction(orderCode, values));
    if (!result) {
      setServerError("Không thể gửi yêu cầu hủy đơn lúc này. Vui lòng thử lại.");
      return;
    }

    if (!result.success) {
      setServerError(result.error.message);
      if (result.error.refresh) router.refresh();
      return;
    }

    setConfirmation(result.message);
    setIsOpen(false);
    reset({ reason: "" });
    router.refresh();
  });

  if (confirmation) {
    return (
      <p
        role="status"
        className="max-w-sm rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm font-semibold text-emerald-800"
      >
        {confirmation}
      </p>
    );
  }

  if (!isCancellationAllowed) {
    return serverError ? (
      <p role="alert" className="max-w-sm text-sm font-semibold text-amber-700">
        {serverError}
      </p>
    ) : null;
  }

  return (
    <Dialog
      open={isOpen}
      onOpenChange={(open) => {
        if (open) {
          setServerError(null);
          setIsOpen(true);
        } else {
          closeDialog();
        }
      }}
    >
      <Button
        type="button"
        variant="outline"
        onClick={() => setIsOpen(true)}
        className="border-rose-200 text-rose-700 hover:bg-rose-50 hover:text-rose-800"
      >
        Hủy đơn hàng
      </Button>

      <DialogContent
        showCloseButton={false}
        className="max-w-lg border border-slate-100 bg-white p-6 shadow-xl"
      >
        <DialogHeader>
          <div className="mb-1 flex h-11 w-11 items-center justify-center rounded-full bg-amber-50 text-amber-700">
            <AlertTriangle className="h-5 w-5" />
          </div>
          <DialogTitle className="text-xl font-black text-slate-900">
            Xác nhận hủy đơn hàng
          </DialogTitle>
          <DialogDescription className="text-sm leading-6 text-slate-600">
            Hệ thống chỉ cho phép hủy khi trạng thái mới nhất vẫn phù hợp. Tồn kho và voucher sẽ do backend xử lý an toàn.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={onSubmit} className="space-y-4">
          <div className="space-y-2">
            <div className="flex items-center justify-between gap-3">
              <Label htmlFor="cancel-reason">Lý do hủy đơn hàng</Label>
              <span className="text-xs text-slate-400">{reason.trim().length}/500</span>
            </div>
            <Textarea
              id="cancel-reason"
              rows={5}
              maxLength={500}
              aria-invalid={Boolean(errors.reason)}
              aria-describedby={errors.reason ? "cancel-reason-error" : undefined}
              placeholder="Ví dụ: Tôi đặt nhầm sản phẩm"
              {...register("reason")}
            />
            {errors.reason?.message && (
              <p id="cancel-reason-error" className="text-xs font-semibold text-rose-600">
                {errors.reason.message}
              </p>
            )}
          </div>

          {serverError && (
            <p role="alert" className="rounded-lg border border-rose-200 bg-rose-50 p-3 text-sm text-rose-800">
              {serverError}
            </p>
          )}

          <DialogFooter className="mt-6">
            <Button type="button" variant="outline" disabled={isLoading} onClick={closeDialog}>
              Giữ lại đơn hàng
            </Button>
            <Button
              type="submit"
              disabled={isLoading}
              className="bg-rose-600 text-white hover:bg-rose-700"
            >
              {isLoading ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" /> Đang hủy đơn...
                </>
              ) : (
                "Xác nhận hủy"
              )}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
