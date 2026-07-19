import { useRouter } from "next/navigation";
import { useProductDialogStore } from "../stores/product-dialog.store";
import { deleteProductAction, publishProductAction, unpublishProductAction } from "../actions";

export function useProductMutations() {
  const router = useRouter();
  const { target, close } = useProductDialogStore();

  const deleteProduct = async () => {
    if (!target) return;
    const res = await deleteProductAction(target.id);
    if (!res.error) {
      close();
      router.refresh();
    } else {
      alert(res.error);
    }
  };

  const publishProduct = async () => {
    if (!target) return;
    const res = await publishProductAction(target.id);
    if (!res.error) {
      close();
      router.refresh();
    } else {
      alert(res.error);
    }
  };

  const unpublishProduct = async () => {
    if (!target) return;
    const res = await unpublishProductAction(target.id);
    if (!res.error) {
      close();
      router.refresh();
    } else {
      alert(res.error);
    }
  };

  return { deleteProduct, publishProduct, unpublishProduct };
}
