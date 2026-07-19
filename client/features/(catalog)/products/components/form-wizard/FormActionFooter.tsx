import React from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Loader2, Save, Globe } from "lucide-react";

interface FormActionFooterProps {
  isSavingSkus: boolean;
  isPublishing: boolean;
  onSaveDraft: () => void;
  onPublish: () => void;
}

export function FormActionFooter({
  isSavingSkus,
  isPublishing,
  onSaveDraft,
  onPublish,
}: FormActionFooterProps) {
  const router = useRouter();

  return (
    <div className="fixed bottom-0 left-0 md:left-48 right-0 z-30 bg-white border-t border-slate-150 shadow-lg py-4 px-6 flex justify-between items-center mt-8">
      <Button
        type="button"
        variant="outline"
        className="border-red-200 text-slate-600 hover:bg-slate-50 cursor-pointer h-10 px-5 text-xs font-bold"
        onClick={() => {
          router.push("/admin/products");
        }}
      >
        Hủy bỏ
      </Button>

      <div className="flex gap-3">
        <Button
          type="button"
          variant="outline"
          disabled={isSavingSkus || isPublishing}
          onClick={onSaveDraft}
          className="border-slate-200 text-slate-700 hover:bg-slate-50 cursor-pointer h-10 px-5 text-xs font-bold gap-1.5"
        >
          {isSavingSkus ? (
            <>
              <Loader2 className="size-4 animate-spin" />
              Đang lưu...
            </>
          ) : (
            <>
              <Save className="size-4" />
              Lưu bản nháp
            </>
          )}
        </Button>
        <Button
          type="button"
          disabled={isSavingSkus || isPublishing}
          onClick={onPublish}
          className="bg-shop_dark_green hover:bg-shop_dark_green/90 text-white cursor-pointer h-10 px-5 text-xs font-bold gap-1.5"
        >
          {isPublishing ? (
            <>
              <Loader2 className="size-4 animate-spin" />
              Đang xuất bản...
            </>
          ) : (
            <>
              <Globe className="size-4" />
              Lưu & Xuất bản
            </>
          )}
        </Button>
      </div>
    </div>
  );
}
