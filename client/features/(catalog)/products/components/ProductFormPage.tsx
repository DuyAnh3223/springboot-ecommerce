"use client";

import React, { useEffect, useRef } from "react";
import { useRouter } from "next/navigation";
import { ProductResponse } from "../product.type";
import { CategoryResponse } from "@/features/(catalog)/categories/category.type";
import { getAttributesAction } from "@/features/(catalog)/attributes/actions";
import { useProductWizardStore } from "../stores/product-wizard.store";
import { useProductBasicInfoForm } from "../hooks/useProductBasicInfoForm";
import { useSkuMatrix } from "../hooks/useSkuMatrix";
import { useProductFormActions } from "../hooks/useProductFormActions";

import { StepBasicInfo } from "./form-wizard/StepBasicInfo";
import { StepAttributesForm } from "./form-wizard/StepAttributesForm";
import { StepSkuMatrix } from "./form-wizard/StepSkuMatrix";
import { StepReviewPublish } from "./form-wizard/StepReviewPublish";
import { FormStepper } from "./form-wizard/FormStepper";
import { FormActionFooter } from "./form-wizard/FormActionFooter";

import { Package, CheckCircle, ArrowLeft, Info } from "lucide-react";

interface ProductFormPageProps {
  product?: ProductResponse | null;
  categories: CategoryResponse[];
}

export function ProductFormPage({ product, categories }: ProductFormPageProps) {
  const router = useRouter();
  const wizard = useProductWizardStore();

  const {
    activeStep,
    savedProductId,
    categoryAttributes,
    loadingAttributes,
    error,
    nonVariantValues,
    selectedVariants,
    newCustomTag,
    setActiveStep,
    setSavedProductId,
    setCategoryAttributes,
    setLoadingAttributes,
    setError,
    setNonVariantValues,
    setSelectedVariants,
    setNewCustomTag,
    resetWizard,
  } = wizard;

  // Wizard Section Refs for smooth scrolling
  const sectionRefs = {
    step1: useRef<HTMLDivElement>(null),
    step2: useRef<HTMLDivElement>(null),
    step3: useRef<HTMLDivElement>(null),
    step4: useRef<HTMLDivElement>(null),
  };

  // 1. Basic Info Form Hook
  const basicInfo = useProductBasicInfoForm(product);

  const {
    register,
    errors,
    setValue,
    isEdit,
    productName,
    productSlug,
    productThumbnail,
    productDescription,
    selectedCategoryId,
  } = basicInfo;

  // 2. Form Actions Hook
  const actions = useProductFormActions({
    product,
    sectionRefs,
    productName,
    selectedCategoryId,
    productSlug,
    productThumbnail,
    productDescription,
    setValue,
  });

  const {
    isSavingSkus,
    isPublishing,
    successToast,
    scrollToSection,
    handleCategorySelect,
    handleSaveProductDraft,
    handlePublishProduct,
  } = actions;

  // 3. Sku Matrix Hook
  const matrix = useSkuMatrix({
    categoryAttributes,
    selectedVariants,
    productSlug,
    productName,
    product,
    loadingAttributes,
    showSuccessBanner: actions.showSuccessBanner,
  });

  const {
    skuFields,
    updateSkuField,
    bulkPrice,
    setBulkPrice,
    bulkStock,
    setBulkStock,
    bulkImage,
    setBulkImage,
    handleBulkApply,
  } = matrix;

  // Reset wizard state and load initial product/skus on mount
  useEffect(() => {
    resetWizard();
    if (product) {
      setSavedProductId(product.id);
      basicInfo.reset({
        name: product.name,
        slug: product.slug,
        thumbnail: product.thumbnail ?? "",
        categoryId: product.category?.id ?? 0,
        description: product.description ?? "",
      });
      setNonVariantValues(product.attributes || {});
      if (product.category?.id) {
        setLoadingAttributes(true);
        getAttributesAction(product.category.id)
          .then((res) => {
            if (res.data) {
              const attrs = res.data;
              setCategoryAttributes(attrs);

              const vInit: Record<string, string[]> = {};
              const variantCodes = attrs
                .filter((ca) => ca.isVariantDefining)
                .map((ca) => ca.code);

              if (product.productSkus && product.productSkus.length > 0) {
                product.productSkus.forEach((sku) => {
                  const sAttrs = sku.attributes || {};
                  Object.entries(sAttrs).forEach(([key, val]) => {
                    if (variantCodes.includes(key)) {
                      if (!vInit[key]) vInit[key] = [];
                      if (val && !vInit[key].includes(String(val))) {
                        vInit[key].push(String(val));
                      }
                    }
                  });
                });
                setSelectedVariants(vInit);

                matrix.replaceSkus(
                  product.productSkus.map((s) => ({
                    attributes: s.attributes || {},
                    sku: s.sku,
                    price: s.price,
                    stock: s.stock,
                    imageUrl: s.imageUrl || "",
                  }))
                );
              } else {
                attrs.forEach((a) => {
                  if (a.isVariantDefining) {
                    vInit[a.code] = [];
                  }
                });
                setSelectedVariants(vInit);
              }
            }
          })
          .finally(() => setLoadingAttributes(false));
      }
    }
  }, [product, basicInfo.reset, resetWizard, setSavedProductId, setCategoryAttributes, setLoadingAttributes, setSelectedVariants, setNonVariantValues]);

  // Scroll spy highlights steps based on scroll location
  useEffect(() => {
    const handleScroll = () => {
      const scrollPos = window.scrollY + 200;
      const step1Top = sectionRefs.step1.current?.offsetTop || 0;
      const step2Top = sectionRefs.step2.current?.offsetTop || 0;
      const step3Top = sectionRefs.step3.current?.offsetTop || 0;
      const step4Top = sectionRefs.step4.current?.offsetTop || 0;

      if (scrollPos >= step4Top) {
        setActiveStep(4);
      } else if (scrollPos >= step3Top) {
        setActiveStep(3);
      } else if (scrollPos >= step2Top) {
        setActiveStep(2);
      } else {
        setActiveStep(1);
      }
    };

    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, [setActiveStep]);

  const selectedCategoryName = React.useMemo(() => {
    const cat = categories.find((c) => c.id === selectedCategoryId);
    return cat ? cat.name : "Chưa chọn danh mục";
  }, [selectedCategoryId, categories]);

  return (
    <div className="space-y-6 max-w-5xl mx-auto pb-28 relative">
      {/* Success Banner popup */}
      {successToast && (
        <div className="fixed bottom-6 right-6 z-50 bg-emerald-600 text-white font-medium text-sm py-3 px-5 rounded-xl shadow-lg border border-emerald-500 flex items-center gap-2 animate-in slide-in-from-bottom-5">
          <CheckCircle className="size-4.5 shrink-0" />
          <span>{successToast}</span>
        </div>
      )}

      {/* Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 border-b border-slate-100 pb-5">
        <div>
          <div
            className="flex items-center gap-2 text-sm text-slate-500 font-semibold cursor-pointer mb-1 hover:text-slate-800"
            onClick={() => router.push("/admin/products")}
          >
            <ArrowLeft className="size-4" /> Quay lại danh sách
          </div>
          <h1 className="text-2xl font-bold text-slate-855 flex items-center gap-2.5">
            <Package className="size-6.5 text-shop_dark_green" />
            {isEdit ? "Chỉnh sửa sản phẩm & SKU" : "Thêm mới sản phẩm"}
          </h1>
          <p className="text-sm text-slate-455 mt-1">
            Giao diện tạo lập sản phẩm hợp nhất. Điền thông tin, gán thuộc tính và lập bảng ma trận SKU.
          </p>
        </div>
      </div>

      {/* Sticky Progress Stepper */}
      <FormStepper activeStep={activeStep} onStepClick={scrollToSection} />

      {/* Error notification popup */}
      {error && (
        <div className="fixed top-24 right-6 z-50 bg-rose-600 text-white font-medium text-sm py-3 px-5 rounded-xl shadow-lg border border-rose-500 flex items-center gap-2 animate-in slide-in-from-top-5 max-w-md">
          <Info className="size-4.5 shrink-0 text-white" />
          <div className="flex-1">{error}</div>
          <button
            type="button"
            onClick={() => setError(null)}
            className="ml-3 text-rose-100 hover:text-white font-bold text-xs focus:outline-none cursor-pointer"
          >
            Đóng
          </button>
        </div>
      )}

      {/* Wizard Sections */}
      <div className="space-y-12">
        {/* Step 1 Section */}
        <div ref={sectionRefs.step1} className="scroll-mt-36">
          <StepBasicInfo
            register={register}
            errors={errors}
            isEdit={isEdit}
            categories={categories}
            selectedCategoryId={selectedCategoryId}
            setValue={setValue}
            setError={setError}
            productThumbnail={productThumbnail || ""}
            productDescription={productDescription || ""}
            onCategorySelect={handleCategorySelect}
          />
        </div>

        {/* Step 2 Section */}
        <div ref={sectionRefs.step2} className="scroll-mt-36">
          <StepAttributesForm
            selectedCategoryName={selectedCategoryName}
            categoryAttributes={categoryAttributes}
            loadingAttributes={loadingAttributes}
            nonVariantValues={nonVariantValues}
            setNonVariantValues={setNonVariantValues}
            selectedVariants={selectedVariants}
            setSelectedVariants={setSelectedVariants}
            newCustomTag={newCustomTag}
            setNewCustomTag={setNewCustomTag}
            scrollToSection={scrollToSection}
          />
        </div>

        {/* Step 3 Section */}
        <div ref={sectionRefs.step3} className="scroll-mt-36">
          <StepSkuMatrix
            skuFields={skuFields}
            updateSkuField={updateSkuField}
            bulkPrice={bulkPrice}
            setBulkPrice={setBulkPrice}
            bulkStock={bulkStock}
            setBulkStock={setBulkStock}
            bulkImage={bulkImage}
            setBulkImage={setBulkImage}
            handleBulkApply={handleBulkApply}
          />
        </div>

        {/* Step 4 Section */}
        <div ref={sectionRefs.step4} className="scroll-mt-36">
          <StepReviewPublish
            productName={productName}
            productSlug={productSlug}
            selectedCategoryName={selectedCategoryName}
            productThumbnail={productThumbnail || ""}
            productDescription={productDescription || ""}
            categoryAttributes={categoryAttributes}
            nonVariantValues={nonVariantValues}
            skuFields={skuFields}
          />
        </div>
      </div>

      {/* Action Footer navigation control */}
      <FormActionFooter
        isSavingSkus={isSavingSkus}
        isPublishing={isPublishing}
        onSaveDraft={() => handleSaveProductDraft(skuFields)}
        onPublish={() => handlePublishProduct(skuFields)}
      />
    </div>
  );
}
