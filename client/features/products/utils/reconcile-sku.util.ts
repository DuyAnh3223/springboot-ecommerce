import { SkuDraft } from "../types/sku.draft.type";

export function getCanonicalVariantKey(attributes?: Record<string, unknown>): string {
  if (!attributes || Object.keys(attributes).length === 0) {
    return "__DEFAULT__";
  }

  return Object.keys(attributes)
    .sort()
    .map((key) => `${key.trim().toLowerCase()}:${String(attributes[key]).trim().toLowerCase()}`)
    .join("|");
}

export function generateDefaultSkuCode(productSlug: string, attributes?: Record<string, unknown>): string {
  const baseSlug = (productSlug || "SKU").toUpperCase().replace(/[^A-Z0-9]/g, "");
  if (!attributes || Object.keys(attributes).length === 0) {
    return baseSlug;
  }

  const attrParts = Object.keys(attributes)
    .sort()
    .map((k) => String(attributes[k]).toUpperCase().replace(/[^A-Z0-9]/g, ""))
    .filter(Boolean);

  return [baseSlug, ...attrParts].join("-");
}

export interface ReconcileResult {
  reconciledSkus: SkuDraft[];
  removedSkuIds: number[];
}

export function reconcileSkuDrafts(
  existingSkus: SkuDraft[],
  newCombinations: Record<string, unknown>[],
  productSlug: string = "PRODUCT"
): ReconcileResult {
  const existingMap = new Map<string, SkuDraft>();
  const unmatchedExistingIds = new Set<number>();

  for (const sku of existingSkus) {
    const key = getCanonicalVariantKey(sku.attributes);
    if (!existingMap.has(key)) {
      existingMap.set(key, sku);
    }
    if (sku.id) {
      unmatchedExistingIds.add(sku.id);
    }
  }

  const reconciledSkus: SkuDraft[] = [];

  for (const comboAttrs of newCombinations) {
    const comboKey = getCanonicalVariantKey(comboAttrs);
    const matchedExisting = existingMap.get(comboKey);

    if (matchedExisting) {
      if (matchedExisting.id) {
        unmatchedExistingIds.delete(matchedExisting.id);
      }
      reconciledSkus.push({
        ...matchedExisting,
        attributes: comboAttrs,
      });
    } else {
      reconciledSkus.push({
        sku: generateDefaultSkuCode(productSlug, comboAttrs),
        price: 0,
        stock: 0,
        weightGram: 0,
        currency: "VND",
        attributes: comboAttrs,
        images: [],
        isNew: true,
      });
    }
  }

  return {
    reconciledSkus,
    removedSkuIds: Array.from(unmatchedExistingIds),
  };
}
