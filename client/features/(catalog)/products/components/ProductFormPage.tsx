"use client";

import React, { useRef, useMemo } from "react";
import { useRouter } from "next/navigation";
import { ProductResponse } from "../product.type";
import { CategoryResponse } from "@/features/(catalog)/categories/category.type";
import { useProductWizardStore } from "../stores/product-wizard.store";
import { useProductBasicInfoForm } from "../hooks/useProductBasicInfoForm";
import { useProductFormActions } from "../hooks/useProductFormActions";
import { useProductSkuForm } from "../hooks/useProductSkuForm";

import { StepBasicInfo } from "./form-wizard/StepBasicInfo";
import { StepAttributesForm } from "./form-wizard/StepAttributesForm";
import { SellingModeSelector } from "./form-wizard/SellingModeSelector";
import { SingleSkuEditor } from "./form-wizard/SingleSkuEditor";
import { VariantOptionsEditor } from "./form-wizard/VariantOptionsEditor";
import { VariantSkuMatrix } from "./form-wizard/VariantSkuMatrix";
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
    categoryAttributes,
    loadingAttributes,
    error,
    nonVariantValues,
    selectedVariants,
    setSavedProductId,
    setCategoryAttributes,
    setLoadingAttributes,
    setError,
    setNonVariantValues,
    setSelectedVariants,
    resetWizard,
  } = wizard;

  const sectionRefs = {
    step1: useRef<HTMLDivElement>(null),
    step2: useRef<HTMLDivElement>(null),
    step3: useRef<HTMLDivElement>(null),
    step4: useRef<HTMLDivElement>(null),
  };

  const basicInfo = useProductBasicInfoForm(product);
  const { register, errors, setValue, isEdit, productName, productSlug, productDescription, selectedCategoryId } = basicInfo;

  const skuForm = useProductSkuForm({
    product,
    productSlug,
    categoryAttributes,
    selectedVariants,
    setSelectedVariants,
    resetWizard,
    setSavedProductId,
    setNonVariantValues,
    setCategoryAttributes,
    setLoadingAttributes,
    basicInfoReset: basicInfo.reset,
  });

  const {
    sellingMode,
    setSellingMode,
    singleSku,
    setSingleSku,
    skus,
    setSkus,
    variantDefs,
    hasVariantAttributes,
    handleVariantSelectionsChange,
    handleRegenerateSkuCodes,
    activeSkusForSubmit,
    activeRemovedIdsForSubmit,
  } = skuForm;

  const actions = useProductFormActions({
    product,
    sectionRefs,
    productName,
    selectedCategoryId,
    productSlug,
    productDescription,
    setValue,
  });

  const { isSavingSkus, isPublishing, successToast, scrollToSection, handleCategorySelect, handleSaveProductDraft, handlePublishProduct } = actions;

  const selectedCategoryName = useMemo(() => {
    const cat = categories.find((c) => c.id === selectedCategoryId);
    return cat ? cat.name : "Chưa chọn danh mục";
  }, [selectedCategoryId, categories]);

  return (
    <div className="space-y-6 max-w-5xl mx-auto pb-28 relative">
      {successToast && (
        <div className="fixed bottom-6 right-6 z-50 bg-emerald-600 text-white font-medium text-sm py-3 px-5 rounded-xl shadow-lg border border-emerald-500 flex items-center gap-2">
          <CheckCircle className="w-4.5 h-4.5 shrink-0" />
          <span>{successToast}</span>
        </div>
      )}

      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 border-b border-slate-100 pb-5">
        <div>
          <div
            className="flex items-center gap-2 text-sm text-slate-500 font-semibold cursor-pointer mb-1 hover:text-slate-800"
            onClick={() => router.push("/admin/products")}
          >
            <ArrowLeft className="w-4 h-4" /> Quay lại danh sách
          </div>
          <h1 className="text-2xl font-bold text-slate-900 flex items-center gap-2.5">
            <Package className="w-6 h-6 text-rose-600" />
            {isEdit ? "Chỉnh sửa sản phẩm & SKU" : "Thêm mới sản phẩm"}
          </h1>
          <p className="text-sm text-slate-500 mt-1">
            Quản lý thông tin chung, thuộc tính kỹ thuật và thiết lập giá bán/tồn kho SKU cho sản phẩm.
          </p>
        </div>
      </div>

      <FormStepper activeStep={activeStep} onStepClick={scrollToSection} />

      {error && (
        <div className="fixed top-24 right-6 z-50 bg-rose-600 text-white font-medium text-sm py-3 px-5 rounded-xl shadow-lg flex items-center gap-2 max-w-md">
          <Info className="w-4.5 h-4.5 shrink-0" />
          <div className="flex-1">{error}</div>
          <button type="button" onClick={() => setError(null)} className="ml-3 font-bold text-xs">
            Đóng
          </button>
        </div>
      )}

      <div className="space-y-12">
        <div ref={sectionRefs.step1} className="scroll-mt-36">
          <StepBasicInfo
            register={register}
            errors={errors}
            isEdit={isEdit}
            categories={categories}
            selectedCategoryId={selectedCategoryId}
            setValue={setValue}
            setError={setError}
            productDescription={productDescription || ""}
            onCategorySelect={handleCategorySelect}
          />
        </div>

        <div ref={sectionRefs.step2} className="scroll-mt-36">
          <StepAttributesForm
            selectedCategoryName={selectedCategoryName}
            categoryAttributes={categoryAttributes}
            loadingAttributes={loadingAttributes}
            nonVariantValues={nonVariantValues}
            setNonVariantValues={setNonVariantValues}
          />
        </div>

        <div ref={sectionRefs.step3} className="scroll-mt-36 space-y-6">
          <SellingModeSelector
            mode={sellingMode}
            hasVariantAttributes={hasVariantAttributes}
            onModeChange={setSellingMode}
            hasExistingData={skus.length > 0 || Boolean(singleSku.sku)}
          />

          {sellingMode === "single" ? (
            <SingleSkuEditor
              skuDraft={singleSku}
              onChange={setSingleSku}
              productSlug={productSlug}
            />
          ) : (
            <div className="space-y-6">
              <VariantOptionsEditor
                variantDefs={variantDefs}
                variantSelections={selectedVariants}
                onChange={handleVariantSelectionsChange}
              />
              <VariantSkuMatrix
                skus={skus}
                onChange={setSkus}
                productSlug={productSlug}
                onRegenerateCodes={handleRegenerateSkuCodes}
              />
            </div>
          )}
        </div>

        <div ref={sectionRefs.step4} className="scroll-mt-36">
          <StepReviewPublish
            productName={productName}
            productSlug={productSlug}
            selectedCategoryName={selectedCategoryName}
            productDescription={productDescription || ""}
            categoryAttributes={categoryAttributes}
            nonVariantValues={nonVariantValues}
            skuFields={activeSkusForSubmit}
          />
        </div>
      </div>

      <FormActionFooter
        isSavingSkus={isSavingSkus}
        isPublishing={isPublishing}
        onSaveDraft={() => handleSaveProductDraft(activeSkusForSubmit, activeRemovedIdsForSubmit)}
        onPublish={() => handlePublishProduct(activeSkusForSubmit, activeRemovedIdsForSubmit)}
      />
    </div>
  );
}
