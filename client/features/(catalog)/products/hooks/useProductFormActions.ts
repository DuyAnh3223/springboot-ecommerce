import { useState } from "react";
import { useRouter } from "next/navigation";
import { getAttributesAction } from "@/features/(catalog)/attributes/actions";
import { uploadFileAction, deleteFileAction } from "@/shared/actions/file.action";
import {
  createProductAction,
  updateProductAction,
  createSkusBulkAction,
  updateSkuAction,
  publishProductAction,
  reconcileSkusAction,
} from "../actions";
import { useProductWizardStore } from "../stores/product-wizard.store";
import { formatAttributesForSubmit } from "../utils/format-attributes";
import { ProductResponse } from "../product.type";
import { SkuGalleryItem } from "../components/SkuGalleryDialog";
import { SkuDraft, ProductReconcilePayload } from "../types/sku.draft.type";
function canonicalStringify(obj: any): string {
  if (obj === null || typeof obj !== "object") {
    return JSON.stringify(obj);
  }
  if (Array.isArray(obj)) {
    return "[" + obj.map(canonicalStringify).join(",") + "]";
  }
  const keys = Object.keys(obj).sort();
  const sortedPairs = keys.map(
    (k) => `${JSON.stringify(k)}:${canonicalStringify(obj[k])}`
  );
  return "{" + sortedPairs.join(",") + "}";
}

function isAttributesEqual(
  a: Record<string, any> = {},
  b: Record<string, any> = {}
): boolean {
  return canonicalStringify(a || {}) === canonicalStringify(b || {});
}

interface UseProductFormActionsParams {
  product?: ProductResponse | null;
  sectionRefs: {
    step1: React.RefObject<HTMLDivElement | null>;
    step2: React.RefObject<HTMLDivElement | null>;
    step3: React.RefObject<HTMLDivElement | null>;
    step4: React.RefObject<HTMLDivElement | null>;
  };
  productName: string;
  selectedCategoryId: number;
  productSlug: string;
  productDescription?: string;
  setValue: any;
}

export function useProductFormActions({
  product,
  sectionRefs,
  productName,
  selectedCategoryId,
  productSlug,
  productDescription,
  setValue,
}: UseProductFormActionsParams) {
  const router = useRouter();
  const wizard = useProductWizardStore();

  const [isSavingSkus, setIsSavingSkus] = useState(false);
  const [isPublishing, setIsPublishing] = useState(false);
  const [successToast, setSuccessToast] = useState<string | null>(null);

  const showSuccessBanner = (msg: string) => {
    setSuccessToast(msg);
    setTimeout(() => setSuccessToast(null), 4000);
  };

  const scrollToSection = (step: number) => {
    const targetRef =
      step === 1
        ? sectionRefs.step1
        : step === 2
        ? sectionRefs.step2
        : step === 3
        ? sectionRefs.step3
        : sectionRefs.step4;

    if (targetRef.current) {
      window.scrollTo({
        top: targetRef.current.offsetTop - 120,
        behavior: "smooth",
      });
      wizard.setActiveStep(step);
    }
  };

  const handleCategorySelect = async (categoryId: number) => {
    setValue("categoryId", categoryId, { shouldValidate: true });

    wizard.setLoadingAttributes(true);
    try {
      const res = await getAttributesAction(categoryId);
      if (res.error) {
        wizard.setError(res.error);
        return;
      }
      const attrs = res.data || [];
      wizard.setCategoryAttributes(attrs);

      const nvInit: Record<string, any> = {};
      const vInit: Record<string, string[]> = {};
      attrs.forEach((a) => {
        if (a.isVariantDefining) {
          vInit[a.code] = [];
        } else {
          nvInit[a.code] = a.isMultiValue ? [] : "";
        }
      });
      wizard.setNonVariantValues(nvInit);
      wizard.setSelectedVariants(vInit);

      setTimeout(() => scrollToSection(2), 200);
    } catch (err) {
      console.error(err);
      wizard.setError("Không thể tải thuộc tính danh mục.");
    } finally {
      wizard.setLoadingAttributes(false);
    }
  };

  const saveProductDraftSilent = async () => {
    wizard.setError(null);
    if (!productName || productName.trim() === "") {
      wizard.setError("Vui lòng nhập tên sản phẩm trước khi lưu.");
      scrollToSection(1);
      return null;
    }
    if (!selectedCategoryId || selectedCategoryId === 0) {
      wizard.setError("Vui lòng chọn danh mục trước khi lưu.");
      scrollToSection(1);
      return null;
    }

    const formattedAttributes = formatAttributesForSubmit(
      wizard.categoryAttributes,
      wizard.nonVariantValues
    );

    const payload = {
      name: productName,
      slug: productSlug,
      description: productDescription || null,
      categoryId: selectedCategoryId,
      attributes: formattedAttributes,
      draft: true,
      published: false,
    };

    const currentId = wizard.savedProductId || product?.id;
    if (currentId) {
      const res = await updateProductAction(currentId, payload);
      if (res.error) {
        wizard.setError(res.error);
        return null;
      }
      return res.product;
    } else {
      const res = await createProductAction(payload);
      if (res.error) {
        wizard.setError(res.error);
        return null;
      }
      if (res.product?.id) {
        wizard.setSavedProductId(res.product.id);
      }
      return res.product;
    }
  };

  const processAndSaveSkus = async (
    productId: number,
    skus: SkuDraft[],
    removedSkuIds: number[]
  ) => {
    if (skus.length === 0 && removedSkuIds.length === 0) return true;

    const pendingUploadedKeys = new Set<string>();

    try {
      const processedSkus = [];
      for (const skuItem of skus) {
        const galleryItems: SkuGalleryItem[] = (skuItem as any).galleryItems || [];
        const imagesPayload: any[] = [];
        const uploadedKeysForThisSku: string[] = [];

        // Upload any local files
        if (skuItem.images) {
          for (const img of skuItem.images) {
            let fileKey = img.url;
            const fileObj = (img as any).file;
            if (fileObj) {
              const formData = new FormData();
              formData.append("file", fileObj);
              formData.append("folder", "products");

              const uploadRes = await uploadFileAction(formData);
              if (uploadRes.error || !uploadRes.data) {
                throw new Error(uploadRes.error || `Tải ảnh cho SKU ${skuItem.sku} thất bại.`);
              }
              fileKey = uploadRes.data.fileKey;
              pendingUploadedKeys.add(fileKey);
              uploadedKeysForThisSku.push(fileKey);
            }

            imagesPayload.push({
              url: fileKey,
              sortOrder: img.sortOrder ?? 0,
              primary: Boolean(img.isPrimary),
            });
          }
        }

        processedSkus.push({
          id: skuItem.id,
          sku: skuItem.sku,
          price: skuItem.price,
          stock: skuItem.stock,
          weightGram: skuItem.weightGram,
          currency: skuItem.currency || "VND",
          attributes: skuItem.attributes || {},
          images: imagesPayload.length > 0 ? imagesPayload : undefined,
          uploadedKeysForThisSku,
        });
      }

      const reconcilePayload: ProductReconcilePayload = {
        skus: processedSkus.map(({ uploadedKeysForThisSku, ...rest }) => rest),
        removedSkuIds,
      };

      const res = await reconcileSkusAction(productId, reconcilePayload);
      if (res.error) {
        throw new Error(res.error);
      }

      return true;
    } catch (err: any) {
      for (const key of Array.from(pendingUploadedKeys)) {
        await deleteFileAction(key);
      }
      wizard.setError(err.message || "Lỗi khi đồng bộ danh sách SKU.");
      return false;
    }
  };

  const handleSaveProductDraft = async (skus: SkuDraft[], removedSkuIds: number[] = []) => {
    wizard.setError(null);
    setIsSavingSkus(true);
    try {
      if (skus.length > 0) {
        const invalidSku = skus.find((s) => !s.sku || s.price < 0 || s.stock < 0);
        if (invalidSku) {
          wizard.setError("Tất cả SKU đều phải có mã định danh, giá bán và tồn kho không âm.");
          scrollToSection(3);
          return;
        }
      }

      const savedProd = await saveProductDraftSilent();
      if (!savedProd) return;
      const currentId = savedProd.id;

      const ok = await processAndSaveSkus(currentId, skus, removedSkuIds);
      if (!ok) return;

      showSuccessBanner("Đã lưu nháp sản phẩm và đồng bộ SKU thành công!");
      setTimeout(() => {
        router.push("/admin/products");
        router.refresh();
      }, 1000);
    } catch (err: any) {
      wizard.setError(err.message || "Lỗi khi lưu bản nháp.");
    } finally {
      setIsSavingSkus(false);
    }
  };

  const handlePublishProduct = async (skus: SkuDraft[], removedSkuIds: number[] = []) => {
    wizard.setError(null);
    setIsPublishing(true);
    try {
      if (skus.length === 0) {
        wizard.setError("Sản phẩm bắt buộc phải có ít nhất 1 SKU để xuất bản.");
        scrollToSection(3);
        return;
      }

      const invalidSku = skus.find((s) => !s.sku || s.price <= 0 || s.stock < 0);
      if (invalidSku) {
        wizard.setError("Mọi SKU đều phải có mã, giá bán lớn hơn 0 và tồn kho không âm trước khi xuất bản.");
        scrollToSection(3);
        return;
      }

      const savedProd = await saveProductDraftSilent();
      if (!savedProd) return;
      const currentId = savedProd.id;

      const ok = await processAndSaveSkus(currentId, skus, removedSkuIds);
      if (!ok) return;

      const resPub = await publishProductAction(currentId);
      if (resPub.error) {
        wizard.setError(resPub.error);
        return;
      }

      showSuccessBanner("Đã lưu & xuất bản sản phẩm thành công!");
      setTimeout(() => {
        router.push("/admin/products");
        router.refresh();
      }, 1000);
    } catch (err: any) {
      wizard.setError(err.message || "Không thể xuất bản sản phẩm.");
    } finally {
      setIsPublishing(false);
    }
  };

  return {
    isSavingSkus,
    isPublishing,
    successToast,
    setSuccessToast,
    showSuccessBanner,
    scrollToSection,
    handleCategorySelect,
    saveProductDraftSilent,
    handleSaveProductDraft,
    handlePublishProduct,
  };
}

