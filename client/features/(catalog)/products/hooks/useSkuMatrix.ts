import { useState, useEffect, useRef } from "react";
import { useForm, useFieldArray } from "react-hook-form";
import { CategoryAttributeResponse } from "@/features/(catalog)/attributes/attribute.type";
import { ProductResponse } from "../product.type";

interface UseSkuMatrixParams {
  categoryAttributes: CategoryAttributeResponse[];
  selectedVariants: Record<string, string[]>;
  productSlug: string;
  productName: string;
  product?: ProductResponse | null;
  loadingAttributes: boolean;
  showSuccessBanner: (msg: string) => void;
}

export function useSkuMatrix({
  categoryAttributes,
  selectedVariants,
  productSlug,
  productName,
  product,
  loadingAttributes,
  showSuccessBanner,
}: UseSkuMatrixParams) {
  const { control, setValue: setSkuValue } = useForm<{
    skus: Array<{
      attributes: Record<string, any>;
      sku: string;
      price: number;
      stock: number;
      imageUrl: string;
    }>;
  }>({
    defaultValues: { skus: [] },
  });

  const { fields: skuFields, replace: replaceSkus, update: updateSkuField } = useFieldArray({
    control,
    name: "skus",
  });

  const [bulkPrice, setBulkPrice] = useState("");
  const [bulkStock, setBulkStock] = useState("");
  const [bulkImage, setBulkImage] = useState("");

  const skuFieldsRef = useRef(skuFields);
  useEffect(() => {
    skuFieldsRef.current = skuFields;
  }, [skuFields]);

  // Reactive sync for automatic SKU preview generation
  useEffect(() => {
    if (loadingAttributes) return;
    if (categoryAttributes.length === 0) return;

    const hasVariants = categoryAttributes.some((ca) => ca.isVariantDefining);

    if (!hasVariants) {
      const defaultSkuStr = `${productSlug || productName || "prod"}-default`
        .toLowerCase()
        .replace(/[^a-z0-9-]/g, "")
        .replace(/-+/g, "-");
      
      const existingDefault = skuFieldsRef.current.find((s) => Object.keys(s.attributes).length === 0);
      if (existingDefault) {
        if (existingDefault.sku !== defaultSkuStr) {
          const index = skuFieldsRef.current.indexOf(existingDefault);
          updateSkuField(index, {
            ...existingDefault,
            sku: defaultSkuStr,
          });
        }
      } else {
        replaceSkus([
          {
            attributes: {},
            sku: defaultSkuStr,
            price: 100000,
            stock: 100,
            imageUrl: "",
          },
        ]);
      }
    } else {
      const variantDefs = categoryAttributes.filter((ca) => ca.isVariantDefining);
      const isAnyVariantEmpty = variantDefs.some(
        (ca) => !selectedVariants[ca.code] || selectedVariants[ca.code].length === 0
      );

      if (isAnyVariantEmpty) {
        if (product && product.productSkus && product.productSkus.length > 0 && skuFieldsRef.current.length > 0) {
          return;
        }
        if (skuFieldsRef.current.length > 0) {
          replaceSkus([]);
        }
        return;
      }

      const generateCombinations = (input: Record<string, string[]>): Array<Record<string, string>> => {
        const keys = Object.keys(input).filter((k) => input[k] && input[k].length > 0);
        if (keys.length === 0) return [];
        
        let results: Array<Record<string, string>> = [{}];
        for (const key of keys) {
          const nextResults: Array<Record<string, string>> = [];
          const values = input[key];
          for (const res of results) {
            for (const val of values) {
              nextResults.push({
                ...res,
                [key]: val,
              });
            }
          }
          results = nextResults;
        }
        return results;
      };

      const combinations = generateCombinations(selectedVariants);
      
      const nextSkus = combinations.map((comb) => {
        const existing = skuFieldsRef.current.find((s) => {
          const sAttrs = s.attributes || {};
          return variantDefs.every((ca) => String(sAttrs[ca.code]) === String(comb[ca.code]));
        });

        const comboSuffix = Object.values(comb)
          .map((v) => String(v).toLowerCase().replace(/[^a-z0-9]/g, "-").replace(/-+/g, "-"))
          .join("-");
        const skuCode = `${productSlug || productName || "prod"}-${comboSuffix}`;

        if (existing) {
          return {
            ...existing,
            sku: skuCode,
            attributes: comb,
          };
        }

        return {
          attributes: comb,
          sku: skuCode,
          price: 100000,
          stock: 50,
          imageUrl: "",
        };
      });

      const isDifferent =
        nextSkus.length !== skuFieldsRef.current.length ||
        nextSkus.some((ns, idx) => {
          const os = skuFieldsRef.current[idx];
          if (!os) return true;
          if (ns.sku !== os.sku) return true;
          return variantDefs.some((ca) => ns.attributes[ca.code] !== os.attributes[ca.code]);
        });

      if (isDifferent) {
        replaceSkus(nextSkus);
      }
    }
  }, [
    selectedVariants,
    productSlug,
    productName,
    categoryAttributes,
    loadingAttributes,
    updateSkuField,
    replaceSkus,
    product,
  ]);

  const handleBulkApply = () => {
    if (!bulkPrice && !bulkStock && !bulkImage) return;
    const priceNum = parseInt(bulkPrice);
    const stockNum = parseInt(bulkStock);

    skuFields.forEach((field, index) => {
      const updated = { ...field };
      if (bulkPrice && !isNaN(priceNum)) updated.price = priceNum;
      if (bulkStock && !isNaN(stockNum)) updated.stock = stockNum;
      if (bulkImage) updated.imageUrl = bulkImage;
      updateSkuField(index, updated);
    });

    showSuccessBanner("Đã áp dụng thông số hàng loạt cho toàn bộ SKU.");
    setBulkPrice("");
    setBulkStock("");
    setBulkImage("");
  };

  return {
    control,
    setSkuValue,
    skuFields,
    replaceSkus,
    updateSkuField,
    bulkPrice,
    setBulkPrice,
    bulkStock,
    setBulkStock,
    bulkImage,
    setBulkImage,
    handleBulkApply,
  };
}
