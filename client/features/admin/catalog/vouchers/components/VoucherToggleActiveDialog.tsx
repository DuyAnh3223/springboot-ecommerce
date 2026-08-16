import React from "react";
import { useRouter } from "next/navigation";
import { useVoucherDialogStore } from "../stores/voucher-dialog.store";
import { deleteVoucherAction } from "@/features/vouchers/actions/delete-voucher.action";
import { reactivateVoucherAction } from "@/features/vouchers/actions/reactivate-voucher.action";
import { useAsyncAction } from "@/shared/hooks/useAsyncAction";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Loader2, Power } from "lucide-react";

export function VoucherToggleActiveDialog() {
  const router = useRouter();
  const { open, target, close } = useVoucherDialogStore();
  const isOpen = open === "toggle-active";
  const { isLoading, error, run } = useAsyncAction();

  if (!target) return null;

  const isDeactivating = target.isActive;

  const handleToggle = () => {
    run(async () => {
      if (isDeactivating) {
        const res = await deleteVoucherAction(target.code);
        if (res.error) throw new Error(res.error);
      } else {
        const res = await reactivateVoucherAction(target.code);
        if (res.error) throw new Error(res.error);
      }
      router.refresh();
      close();
    });
  };

  return (
    <Dialog open={isOpen} onOpenChange={(openState) => !openState && close()}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Power className={`size-5 ${isDeactivating ? "text-amber-500" : "text-emerald-500"}`} />
            {isDeactivating ? "Xác nhận Ngừng kích hoạt" : "Xác nhận Kích hoạt lại"}
          </DialogTitle>
        </DialogHeader>

        {error && (
          <div className="p-3 text-sm text-red-700 bg-red-50 border border-red-200 rounded-md">
            {error}
          </div>
        )}

        <div className="py-2 text-sm text-slate-600 space-y-2">
          {isDeactivating ? (
            <p>
              Bạn có chắc chắn muốn ngừng kích hoạt voucher{" "}
              <strong className="font-mono text-blue-700">{target.code}</strong> (
              {target.name}) không? Khách hàng sẽ không thể nhập mã này khi thanh toán.
            </p>
          ) : (
            <p>
              Bạn có chắc chắn muốn kích hoạt lại voucher{" "}
              <strong className="font-mono text-blue-700">{target.code}</strong> (
              {target.name}) không?
            </p>
          )}
        </div>

        <DialogFooter className="gap-2">
          <Button type="button" variant="outline" onClick={close}>
            Hủy
          </Button>
          <Button
            type="button"
            variant={isDeactivating ? "destructive" : "default"}
            onClick={handleToggle}
            disabled={isLoading}
          >
            {isLoading && <Loader2 className="size-4 animate-spin mr-2" />}
            {isDeactivating ? "Ngừng kích hoạt" : "Kích hoạt lại"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
