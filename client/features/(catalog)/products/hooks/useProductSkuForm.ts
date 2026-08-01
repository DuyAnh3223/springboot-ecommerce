import { useState, useEffect, useRef, useMemo } from "react";
import { ProductResponse } from "../product.type";
import { CategoryAttributeResponse } from "@/features/(catalog)/attributes/attribute.type";
import { SellingMode, SkuDraft } from "../types/sku.draft.type";
import { reconcileSkuDrafts, generateDefaultSkuCode } from "../utils/reconcile-sku.util";
import { getAttributesAction } from "@/features/(catalog)/attributes/actions";

function cartesianProduct(arrays: string[][]): string[][] {
  return arrays.reduce<string[][]>(
    (acc, curr) => acc.flatMap((x) => curr.map((y) => [...x, y])),
    [[]]
  );
}

interface UseProductSkuFormParams {
  product?: ProductResponse | null;
  productSlug: string;
  categoryAttributes: CategoryAttributeResponse[];
  selectedVariants: Record<string, string[]>;
  setSelectedVariants: (v: Record<string, string[]>) => void;
  resetWizard: () => void;
  setSavedProductId: (id: number) => void;
  setNonVariantValues: (v: Record<string, unknown>) => void;
  setCategoryAttributes: (attrs: CategoryAttributeResponse[]) => void;
  setLoadingAttributes: (loading: boolean) => void;
  basicInfoReset: (values: Record<string, unknown>) => void;
}

export function useProductSkuForm({
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
  basicInfoReset,
}: UseProductSkuFormParams) {
  const [sellingMode, setSellingMode] = useState<SellingMode>("single");
  const [singleSku, setSingleSku] = useState<SkuDraft>({
    sku: "",
    price: 0,
    stock: 0,
    weightGram: 0,
    currency: "VND",
    attributes: {},
    images: [],
  });

  const [skus, setSkus] = useState<SkuDraft[]>([]);
  const [removedSkuIds, setRemovedSkuIds] = useState<number[]>([]);
  const [existingSkus, setExistingSkus] = useState<SkuDraft[]>([]);

  const variantDefs = useMemo(() => {
    return categoryAttributes.filter((ca) => Boolean(ca.isVariantDefining));
  }, [categoryAttributes]);

  const hasVariantAttributes = variantDefs.length > 0;

  useEffect(() => {
    resetWizard();
    if (product) {
      setSavedProductId(product.id);
      basicInfoReset({
        name: product.name,
        slug: product.slug,
        categoryId: product.category?.id ?? 0,
        description: product.description ?? "",
      });
      setNonVariantValues(product.attributes || {});

      if (product.skus && product.skus.length > 0) {
        const initialDrafts: SkuDraft[] = product.skus.map((s) => ({
          id: s.id,
          sku: s.sku,
          price: Number(s.price),
          stock: s.stock,
          weightGram: s.weightGram ?? 0,
          currency: s.currency ?? "VND",
          attributes: s.attributes || {},
          images: (s.images || []).map((img) => ({
            url: img.url,
            isPrimary: Boolean(img.primary),
            sortOrder: img.sortOrder,
          })),
        }));

        setExistingSkus(initialDrafts);

        const isMulti = product.skus.length > 1 || Object.keys(product.skus[0]?.attributes || {}).length > 0;
        setSellingMode(isMulti ? "multi" : "single");

        if (isMulti) {
          setSkus(initialDrafts);

          const initialSelectedVariants: Record<string, string[]> = {};
          initialDrafts.forEach((draft) => {
            if (draft.attributes) {
              Object.entries(draft.attributes).forEach(([attrCode, val]) => {
                if (val !== undefined && val !== null) {
                  const strVal = String(val);
                  if (!initialSelectedVariants[attrCode]) {
                    initialSelectedVariants[attrCode] = [];
                  }
                  if (!initialSelectedVariants[attrCode].includes(strVal)) {
                    initialSelectedVariants[attrCode].push(strVal);
                  }
                }
              });
            }
          });
          setSelectedVariants(initialSelectedVariants);
        } else {
          setSingleSku(initialDrafts[0] || {
            sku: product.slug.toUpperCase(),
            price: 0,
            stock: 0,
            weightGram: 0,
            currency: "VND",
            attributes: {},
            images: [],
          });
        }
      }

      if (product.category?.id) {
        setLoadingAttributes(true);
        getAttributesAction(product.category.id)
          .then((res) => {
            if (res.data) {
              setCategoryAttributes(res.data);
            }
          })
          .finally(() => setLoadingAttributes(false));
      }
    } else {
      setSingleSku({
        sku: "",
        price: 0,
        stock: 0,
        weightGram: 0,
        currency: "VND",
        attributes: {},
        images: [],
      });
    }
  }, [product, basicInfoReset, resetWizard, setSavedProductId, setCategoryAttributes, setLoadingAttributes, setNonVariantValues, setSelectedVariants]);

  const handleVariantSelectionsChange = (newSelections: Record<string, string[]>) => {
    setSelectedVariants(newSelections);

    const activeVarDefs = variantDefs.filter((ca) => (newSelections[ca.code] || []).length > 0);
    if (activeVarDefs.length === 0) {
      setSkus([]);
      setRemovedSkuIds(existingSkus.map((s) => s.id!).filter(Boolean));
      return;
    }

    const varCodes = activeVarDefs.map((ca) => ca.code);
    const valueArrays = varCodes.map((code) => newSelections[code]);
    const cartesian = cartesianProduct(valueArrays);

    const newCombinations = cartesian.map((comboVals) => {
      const comboAttrs: Record<string, unknown> = {};
      varCodes.forEach((code, i) => {
        comboAttrs[code] = comboVals[i];
      });
      return comboAttrs;
    });

    const result = reconcileSkuDrafts(existingSkus, newCombinations, productSlug);
    setSkus(result.reconciledSkus);
    setRemovedSkuIds(result.removedSkuIds);
  };

  const handleRegenerateSkuCodes = () => {
    const regenerated = skus.map((s) => ({
      ...s,
      sku: generateDefaultSkuCode(productSlug, s.attributes),
    }));
    setSkus(regenerated);
  };

  const activeSkusForSubmit = useMemo(() => {
    if (sellingMode === "single") {
      const finalSku = singleSku.sku || (productSlug ? productSlug.toUpperCase() : "SKU-001");
      return [{ ...singleSku, sku: finalSku, attributes: {} }];
    }
    return skus;
  }, [sellingMode, singleSku, skus, productSlug]);

  const activeRemovedIdsForSubmit = useMemo(() => {
    if (sellingMode === "single") {
      return existingSkus
        .filter((s) => s.id && s.id !== singleSku.id)
        .map((s) => s.id!);
    }
    return removedSkuIds;
  }, [sellingMode, singleSku, existingSkus, removedSkuIds]);

  return {
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
  };
}
