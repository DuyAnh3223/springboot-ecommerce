import React, { useEffect, useState } from "react";
import { useForm, useWatch, Resolver } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import {
  createVoucherSchema,
  updateVoucherSchema,
  voucherRawObject,
  VoucherFormInput,
} from "@/features/vouchers/schemas/voucher.schema";
import {
  VoucherCreateRequest,
  VoucherUpdateRequest,
} from "@/features/vouchers/voucher.type";
import { useVoucherDialogStore } from "../stores/voucher-dialog.store";
import { createVoucherAction } from "@/features/vouchers/actions/create-voucher.action";
import { updateVoucherAction } from "@/features/vouchers/actions/update-voucher.action";
import { useAsyncAction } from "@/shared/hooks/useAsyncAction";
import { SkuSelectorModal } from "./SkuSelectorModal";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Loader2, PackageCheck } from "lucide-react";

const formResolver: Resolver<VoucherFormInput> = zodResolver(voucherRawObject);

const toInputDatetime = (isoStr?: string | null) => {
  if (!isoStr) return "";
  const date = new Date(isoStr);
  if (Number.isNaN(date.getTime())) return "";
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(
    date.getHours(),
  )}:${pad(date.getMinutes())}`;
};

const getFutureStartDate = () =>
  toInputDatetime(new Date(Date.now() + 5 * 60 * 1000).toISOString());

const getFutureEndDate = () =>
  toInputDatetime(new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString());

export function VoucherFormDialog() {
  const router = useRouter();
  const { open, target, close } = useVoucherDialogStore();
  const isOpen = open === "create" || open === "edit";
  const isEdit = open === "edit";
  const { isLoading, error, run } = useAsyncAction();

  const [isSkuModalOpen, setIsSkuModalOpen] = useState(false);

  const {
    register,
    handleSubmit,
    control,
    setValue,
    reset,
    setError,
    formState: { errors },
  } = useForm<VoucherFormInput>({
    resolver: formResolver,
    defaultValues: {
      name: "",
      description: "",
      type: "PERCENTAGE",
      value: 10,
      maxDiscountAmount: null,
      code: "",
      startDate: getFutureStartDate(),
      endDate: getFutureEndDate(),
      maxUses: null,
      maxPerUser: 1,
      minOrderValue: null,
      isActive: true,
      applyScope: "ALL",
      productSkuIds: [],
    },
  });

  const watchType = useWatch({ control, name: "type" });
  const watchApplyScope = useWatch({ control, name: "applyScope" });
  const watchProductSkuIds = useWatch({ control, name: "productSkuIds" }) || [];

  // Automatically clear maxDiscountAmount when type is FIXED_AMOUNT
  useEffect(() => {
    if (watchType === "FIXED_AMOUNT") {
      setValue("maxDiscountAmount", null, { shouldValidate: true });
    }
  }, [watchType, setValue]);

  // Reset form when dialog opens
  useEffect(() => {
    if (isOpen) {
      if (isEdit && target) {
        reset({
          name: target.name,
          description: target.description || "",
          type: target.type,
          value: target.value,
          maxDiscountAmount: target.maxDiscountAmount ?? null,
          code: target.code,
          startDate: toInputDatetime(target.startDate),
          endDate: toInputDatetime(target.endDate),
          maxUses: target.maxUses ?? null,
          maxPerUser: target.maxPerUser ?? null,
          minOrderValue: target.minOrderValue ?? null,
          isActive: target.isActive,
          applyScope: target.applyScope,
          productSkuIds: target.productSkus?.map((s) => s.id) || [],
        });
      } else {
        reset({
          name: "",
          description: "",
          type: "PERCENTAGE",
          value: 10,
          maxDiscountAmount: null,
          code: "",
          startDate: getFutureStartDate(),
          endDate: getFutureEndDate(),
          maxUses: null,
          maxPerUser: 1,
          minOrderValue: null,
          isActive: true,
          applyScope: "ALL",
          productSkuIds: [],
        });
      }
    }
  }, [isOpen, isEdit, target, reset]);

  const normalizeOptionalNumber = (value: unknown) => {
    if (value === "" || value === null || value === undefined) return null;
    const numberValue = Number(value);
    return Number.isNaN(numberValue) ? null : numberValue;
  };

  const onSubmit = (values: VoucherFormInput) => {
    run(async () => {
      const validation = isEdit
        ? updateVoucherSchema.safeParse(values)
        : createVoucherSchema.safeParse(values);

      if (!validation.success) {
        const firstIssue = validation.error.issues[0];
        const field = firstIssue?.path[0];
        if (typeof field === "string" && field in values) {
          setError(field as keyof VoucherFormInput, {
            type: "manual",
            message: firstIssue.message,
          });
        }
        return;
      }

      const payloadBase = {
        name: values.name.trim(),
        description: values.description || null,
        type: values.type,
        value: Number(values.value),
        maxDiscountAmount:
          values.type === "FIXED_AMOUNT"
            ? null
            : normalizeOptionalNumber(values.maxDiscountAmount),
        startDate: new Date(values.startDate).toISOString(),
        endDate: new Date(values.endDate).toISOString(),
        maxUses: normalizeOptionalNumber(values.maxUses),
        maxPerUser: normalizeOptionalNumber(values.maxPerUser),
        minOrderValue: normalizeOptionalNumber(values.minOrderValue),
        isActive: values.isActive,
        applyScope: values.applyScope,
        productSkuIds: values.applyScope === "SPECIFIC" ? values.productSkuIds || [] : [],
      };

      if (isEdit && target) {
        const res = await updateVoucherAction(target.code, payloadBase satisfies VoucherUpdateRequest);
        if (res.error) throw new Error(res.error);
      } else {
        const createPayload: VoucherCreateRequest = {
          ...payloadBase,
          code: values.code.trim().toUpperCase(),
        };
        const res = await createVoucherAction(createPayload);
        if (res.error) throw new Error(res.error);
      }

      router.refresh();
      close();
    });
  };

  return (
    <>
      <Dialog open={isOpen} onOpenChange={(openState) => !openState && close()}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>
              {isEdit ? `Chỉnh sửa mã giảm giá: ${target?.code}` : "Tạo mã giảm giá mới"}
            </DialogTitle>
          </DialogHeader>

          {error && (
            <div className="p-3 text-sm text-red-700 bg-red-50 border border-red-200 rounded-md">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 text-slate-700">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <Label htmlFor="code">Mã Voucher (Code) *</Label>
                <Input
                  id="code"
                  disabled={isEdit}
                  placeholder="VD: SUMMER2026"
                  className="font-mono uppercase mt-1"
                  {...register("code")}
                />
                {errors.code && (
                  <p className="text-xs text-red-500 mt-1">{String(errors.code?.message)}</p>
                )}
              </div>

              <div>
                <Label htmlFor="name">Tên hiển thị *</Label>
                <Input
                  id="name"
                  placeholder="VD: Giảm 20% đón Hè"
                  className="mt-1"
                  {...register("name")}
                />
                {errors.name && (
                  <p className="text-xs text-red-500 mt-1">{String(errors.name?.message)}</p>
                )}
              </div>
            </div>

            <div>
              <Label htmlFor="description">Mô tả</Label>
              <Textarea
                id="description"
                placeholder="Mô tả chi tiết điều kiện áp dụng..."
                className="mt-1 resize-none"
                rows={2}
                {...register("description")}
              />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <Label>Loại giảm giá *</Label>
                <select
                  className="w-full h-10 border border-slate-200 rounded-md px-3 text-sm mt-1 bg-white"
                  {...register("type")}
                >
                  <option value="PERCENTAGE">Phần trăm (%)</option>
                  <option value="FIXED_AMOUNT">Số tiền cố định (VNĐ)</option>
                </select>
              </div>

              <div>
                <Label htmlFor="value">Giá trị giảm *</Label>
                <Input
                  id="value"
                  type="number"
                  step="any"
                  className="mt-1"
                  {...register("value")}
                />
                {errors.value && (
                  <p className="text-xs text-red-500 mt-1">{String(errors.value?.message)}</p>
                )}
              </div>

              <div>
                <Label htmlFor="maxDiscountAmount">Trần giảm tối đa (VNĐ)</Label>
                <Input
                  id="maxDiscountAmount"
                  type="number"
                  disabled={watchType === "FIXED_AMOUNT"}
                  placeholder={watchType === "FIXED_AMOUNT" ? "Không áp dụng" : "VD: 100000"}
                  className="mt-1"
                  {...register("maxDiscountAmount")}
                />
                {errors.maxDiscountAmount && (
                  <p className="text-xs text-red-500 mt-1">{String(errors.maxDiscountAmount?.message)}</p>
                )}
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <Label htmlFor="startDate">Ngày bắt đầu *</Label>
                <Input id="startDate" type="datetime-local" className="mt-1" {...register("startDate")} />
                {errors.startDate && (
                  <p className="text-xs text-red-500 mt-1">{String(errors.startDate?.message)}</p>
                )}
              </div>

              <div>
                <Label htmlFor="endDate">Ngày kết thúc *</Label>
                <Input id="endDate" type="datetime-local" className="mt-1" {...register("endDate")} />
                {errors.endDate && (
                  <p className="text-xs text-red-500 mt-1">{String(errors.endDate?.message)}</p>
                )}
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <Label htmlFor="maxUses">Tổng lượt dùng tối đa</Label>
                <Input id="maxUses" type="number" placeholder="Để trống = Không giới hạn" className="mt-1" {...register("maxUses")} />
                {errors.maxUses && (
                  <p className="text-xs text-red-500 mt-1">{String(errors.maxUses?.message)}</p>
                )}
              </div>

              <div>
                <Label htmlFor="maxPerUser">Lượt dùng / Khách hàng</Label>
                <Input id="maxPerUser" type="number" className="mt-1" {...register("maxPerUser")} />
                {errors.maxPerUser && (
                  <p className="text-xs text-red-500 mt-1">{String(errors.maxPerUser?.message)}</p>
                )}
              </div>

              <div>
                <Label htmlFor="minOrderValue">Đơn tối thiểu (VNĐ)</Label>
                <Input id="minOrderValue" type="number" placeholder="0" className="mt-1" {...register("minOrderValue")} />
                {errors.minOrderValue && (
                  <p className="text-xs text-red-500 mt-1">{String(errors.minOrderValue?.message)}</p>
                )}
              </div>
            </div>

            <div>
              <Label>Phạm vi áp dụng *</Label>
              <div className="flex gap-4 mt-2">
                <label className="flex items-center gap-2 cursor-pointer text-sm">
                  <input type="radio" value="ALL" {...register("applyScope")} /> Tất cả sản phẩm
                </label>
                <label className="flex items-center gap-2 cursor-pointer text-sm">
                  <input type="radio" value="SPECIFIC" {...register("applyScope")} /> Sản phẩm cụ thể (SKU)
                </label>
              </div>
            </div>

            {watchApplyScope === "SPECIFIC" && (
              <div className="border border-slate-200 rounded-lg p-3 bg-slate-50 space-y-2">
                <div className="flex items-center justify-between">
                  <Label className="font-semibold text-sm">Danh sách SKU sản phẩm áp dụng</Label>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => setIsSkuModalOpen(true)}
                    className="gap-2 bg-white text-blue-600 border-blue-200 hover:bg-blue-50"
                  >
                    <PackageCheck className="size-4" />
                    Chọn sản phẩm SKU áp dụng (Đã chọn: {watchProductSkuIds.length})
                  </Button>
                </div>
                {errors.productSkuIds && (
                  <p className="text-xs text-red-500">{String(errors.productSkuIds?.message)}</p>
                )}
              </div>
            )}

            <DialogFooter className="pt-2">
              <Button type="button" variant="outline" onClick={close}>
                Hủy
              </Button>
              <Button type="submit" disabled={isLoading} className="bg-blue-600 hover:bg-blue-700 text-white">
                {isLoading && <Loader2 className="size-4 animate-spin mr-2" />}
                {isEdit ? "Cập nhật Voucher" : "Tạo Voucher mới"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {isSkuModalOpen && (
        <SkuSelectorModal
          open
          onClose={() => setIsSkuModalOpen(false)}
          initialSelectedSkuIds={watchProductSkuIds}
          onConfirm={(selectedIds) => setValue("productSkuIds", selectedIds, { shouldValidate: true })}
        />
      )}
    </>
  );
}
