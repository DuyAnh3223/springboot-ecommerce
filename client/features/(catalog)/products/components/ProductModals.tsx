import React from "react";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Trash2, Globe } from "lucide-react";
import { useProductDialogStore } from "../stores/product-dialog.store";
import { useProductMutations } from "../hooks/useProductMutations";

export function ProductModals() {
  const { open, target, close } = useProductDialogStore();
  const { deleteProduct, publishProduct, unpublishProduct } = useProductMutations();

  return (
    <>
      {/* Delete confirmation dialog */}
      <Dialog open={open === "delete"} onOpenChange={(val) => !val && close()}>
        <DialogContent className="sm:max-w-md bg-white border-none shadow-lg">
          <DialogHeader>
            <DialogTitle className="text-slate-800 flex items-center gap-2">
              <Trash2 className="size-5 text-red-500" /> Xác nhận xóa sản phẩm
            </DialogTitle>
            <DialogDescription className="text-slate-500 text-sm mt-1">
              Bạn có chắc chắn muốn xóa sản phẩm <strong>{target?.name}</strong>? Hành động này sẽ đồng thời soft-delete tất cả biến thể SKU đi kèm và không thể hoàn tác.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter className="mt-4 gap-2">
            <Button
              variant="outline"
              onClick={close}
              className="border-slate-200 hover:bg-slate-50 cursor-pointer"
            >
              Hủy bỏ
            </Button>
            <Button onClick={deleteProduct} className="bg-red-500 hover:bg-red-600 text-white cursor-pointer">
              Xóa bỏ
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Publish confirmation dialog */}
      <Dialog open={open === "publish"} onOpenChange={(val) => !val && close()}>
        <DialogContent className="sm:max-w-md bg-white border-none shadow-lg">
          <DialogHeader>
            <DialogTitle className="text-slate-800 flex items-center gap-2">
              <Globe className="size-5 text-emerald-500" /> Xác nhận xuất bản sản phẩm
            </DialogTitle>
            <DialogDescription className="text-slate-500 text-sm mt-1">
              Bạn có chắc chắn muốn xuất bản sản phẩm <strong>{target?.name}</strong> ra ngoài trang bán hàng công khai? Sản phẩm bắt buộc phải có ít nhất một SKU biến thể đang hoạt động.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter className="mt-4 gap-2">
            <Button
              variant="outline"
              onClick={close}
              className="border-slate-200 hover:bg-slate-50 cursor-pointer"
            >
              Hủy bỏ
            </Button>
            <Button onClick={publishProduct} className="bg-emerald-600 hover:bg-emerald-700 text-white cursor-pointer">
              Xuất bản
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Unpublish confirmation dialog */}
      <Dialog open={open === "unpublish"} onOpenChange={(val) => !val && close()}>
        <DialogContent className="sm:max-w-md bg-white border-none shadow-lg">
          <DialogHeader>
            <DialogTitle className="text-slate-800 flex items-center gap-2">
              <Globe className="size-5 text-amber-500" /> Xác nhận tạm ẩn sản phẩm
            </DialogTitle>
            <DialogDescription className="text-slate-500 text-sm mt-1">
              Bạn có chắc chắn muốn tạm ẩn sản phẩm <strong>{target?.name}</strong> khỏi trang bán hàng công khai?
            </DialogDescription>
          </DialogHeader>
          <DialogFooter className="mt-4 gap-2">
            <Button
              variant="outline"
              onClick={close}
              className="border-slate-200 hover:bg-slate-50 cursor-pointer"
            >
              Hủy bỏ
            </Button>
            <Button onClick={unpublishProduct} className="bg-amber-600 hover:bg-amber-700 text-white cursor-pointer">
              Tạm ẩn
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
