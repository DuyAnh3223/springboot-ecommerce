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
} from "../actions";
import { useProductWizardStore } from "../stores/product-wizard.store";
import { formatAttributesForSubmit } from "../utils/format-attributes";
import { ProductResponse } from "../product.type";
import { SkuGalleryItem } from "../components/SkuGalleryDialog";
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

  const processAndSaveSkus = async (productId: number, skuFields: any[]) => {
    if (skuFields.length === 0) return true;

    const pendingUploadedKeys = new Set<string>();

    try {
      // 1. Process files & build gallery payloads per SKU
      const processedSkus = [];
      for (const field of skuFields) {
        const galleryItems: SkuGalleryItem[] = field.galleryItems || [];
        const imagesPayload = [];
        const uploadedKeysForThisSku: string[] = [];

        for (const item of galleryItems) {
          let fileKey = item.url;
          if (item.file) {
            const formData = new FormData();
            formData.append("file", item.file);
            formData.append("folder", "products");

            const uploadRes = await uploadFileAction(formData);
            if (uploadRes.error || !uploadRes.data) {
              throw new Error(uploadRes.error || `Tải ảnh cho SKU ${field.sku} thất bại.`);
            }
            fileKey = uploadRes.data.fileKey;
            pendingUploadedKeys.add(fileKey);
            uploadedKeysForThisSku.push(fileKey);
          }

          imagesPayload.push({
            id: item.id,
            url: fileKey,
            sortOrder: item.sortOrder,
            primary: item.isPrimary,
          });
        }

        processedSkus.push({
          ...field,
          imagesPayload,
          uploadedKeysForThisSku,
        });
      }

      // 2. Separate into NEW SKUs vs EXISTING SKUs
      const isExistingSku = (s: any) =>
        (typeof s.skuId === "number" && s.skuId > 0) ||
        (typeof s.id === "number" && s.id > 0);

      const newSkus = processedSkus.filter((s) => !isExistingSku(s));
      const existingSkus = processedSkus.filter((s) => isExistingSku(s));

      // 3. Bulk Create New SKUs
      if (newSkus.length > 0) {
        const bulkPayload = newSkus.map((s) => ({
          productId,
          sku: s.sku,
          price: s.price,
          stock: s.stock,
          attributes: s.attributes,
          images: s.imagesPayload,
        }));

        const resBulk = await createSkusBulkAction(productId, bulkPayload);
        if (resBulk.error) {
          throw new Error(resBulk.error);
        }

        // On success, remove uploaded keys of new SKUs from pending
        newSkus.forEach((s) => {
          s.uploadedKeysForThisSku.forEach((k: string) => pendingUploadedKeys.delete(k));
        });
      }

      // 4. Update Existing SKUs individually via PATCH (only if changed)
      for (const existingSku of existingSkus) {
        const targetSkuId = typeof existingSku.skuId === "number" ? existingSku.skuId : (existingSku.id as number);
        const originalSku = product?.skus?.find((s) => s.id === targetSkuId);

        const isSkuCodeChanged = originalSku ? existingSku.sku !== originalSku.sku : true;
        const isPriceChanged = originalSku ? Number(existingSku.price) !== Number(originalSku.price) : true;
        const isStockChanged = originalSku ? Number(existingSku.stock) !== Number(originalSku.stock) : true;
        const isAttrsChanged = originalSku ? !isAttributesEqual(existingSku.attributes, originalSku.attributes) : true;
        const isGalleryDirty = Boolean(existingSku.isGalleryDirty);

        const hasChanges = isSkuCodeChanged || isPriceChanged || isStockChanged || isAttrsChanged || isGalleryDirty;

        if (!hasChanges) {
          continue;
        }

        const updatePayload: any = {
          sku: existingSku.sku,
          price: existingSku.price,
          stock: existingSku.stock,
          attributes: existingSku.attributes,
        };

        if (isGalleryDirty) {
          updatePayload.images = existingSku.imagesPayload;
        }

        const resPatch = await updateSkuAction(targetSkuId, updatePayload);
        if (resPatch.error) {
          throw new Error(resPatch.error);
        }

        // On success, remove uploaded keys of this SKU from pending
        existingSku.uploadedKeysForThisSku.forEach((k: string) => pendingUploadedKeys.delete(k));
      }

      return true;
    } catch (err: any) {
      // Cleanup orphan uploaded S3 keys on failure
      for (const key of Array.from(pendingUploadedKeys)) {
        await deleteFileAction(key);
      }
      wizard.setError(err.message || "Lỗi khi lưu các biến thể SKU.");
      return false;
    }
  };

  const handleSaveProductDraft = async (skuFields: any[]) => {
    wizard.setError(null);
    setIsSavingSkus(true);
    try {
      if (skuFields.length > 0) {
        const invalidSku = skuFields.find((s) => !s.sku || s.price < 0 || s.stock < 0);
        if (invalidSku) {
          wizard.setError("Tất cả biến thể SKU đều phải có mã định danh, giá bán và tồn kho không âm.");
          scrollToSection(3);
          return;
        }
      }

      const savedProd = await saveProductDraftSilent();
      if (!savedProd) return;
      const currentId = savedProd.id;

      const ok = await processAndSaveSkus(currentId, skuFields);
      if (!ok) return;

      showSuccessBanner("Đã lưu nháp sản phẩm và SKU thành công!");
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

  const handlePublishProduct = async (skuFields: any[]) => {
    wizard.setError(null);
    setIsPublishing(true);
    try {
      if (skuFields.length === 0) {
        wizard.setError("Sản phẩm bắt buộc phải có ít nhất 1 SKU biến thể để xuất bản.");
        scrollToSection(3);
        return;
      }

      const invalidSku = skuFields.find((s) => !s.sku || s.price <= 0 || s.stock < 0);
      if (invalidSku) {
        wizard.setError("Mọi SKU đều phải có mã, giá bán lớn hơn 0 và tồn kho không âm trước khi xuất bản.");
        scrollToSection(3);
        return;
      }

      const savedProd = await saveProductDraftSilent();
      if (!savedProd) return;
      const currentId = savedProd.id;

      const ok = await processAndSaveSkus(currentId, skuFields);
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
