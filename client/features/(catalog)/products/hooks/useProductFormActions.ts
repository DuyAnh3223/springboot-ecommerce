import { useState } from "react";
import { useRouter } from "next/navigation";
import { getAttributesAction } from "@/features/(catalog)/attributes/actions";
import {
  createProductAction,
  updateProductAction,
  createSkusBulkAction,
  publishProductAction,
} from "../actions";
import { useProductWizardStore } from "../stores/product-wizard.store";
import { formatAttributesForSubmit } from "../utils/format-attributes";
import { ProductResponse } from "../product.type";

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
  productThumbnail?: string;
  productDescription?: string;
  setValue: any;
}

export function useProductFormActions({
  product,
  sectionRefs,
  productName,
  selectedCategoryId,
  productSlug,
  productThumbnail,
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
      thumbnail: productThumbnail || null,
      description: productDescription || null,
      categoryId: selectedCategoryId,
      attributes: formattedAttributes,
      isDraft: true,
      isPublished: false,
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

  const handleSaveProductDraft = async (skuFields: any[]) => {
    wizard.setError(null);
    setIsSavingSkus(true);
    try {
      const savedProd = await saveProductDraftSilent();
      if (!savedProd) return;
      const currentId = savedProd.id;

      if (skuFields.length > 0) {
        const invalidSku = skuFields.find((s) => !s.sku || s.price < 0 || s.stock < 0);
        if (invalidSku) {
          wizard.setError("Tất cả biến thể SKU đều phải có mã định danh, giá bán và tồn kho không âm.");
          scrollToSection(3);
          return;
        }

        const skusPayload = skuFields.map((s) => ({
          productId: currentId,
          sku: s.sku,
          price: s.price,
          stock: s.stock,
          imageUrl: s.imageUrl || null,
          attributes: s.attributes,
        }));

        const resSku = await createSkusBulkAction(currentId, skusPayload);
        if (resSku.error) {
          wizard.setError(resSku.error);
          return;
        }
      }

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
      const savedProd = await saveProductDraftSilent();
      if (!savedProd) return;
      const currentId = savedProd.id;

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

      const skusPayload = skuFields.map((s) => ({
        productId: currentId,
        sku: s.sku,
        price: s.price,
        stock: s.stock,
        imageUrl: s.imageUrl || null,
        attributes: s.attributes,
      }));

      const resSku = await createSkusBulkAction(currentId, skusPayload);
      if (resSku.error) {
        wizard.setError(resSku.error);
        return;
      }

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
