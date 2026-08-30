import type {
  CatalogAttributeDefinition,
  CatalogAttributeValue,
  CatalogProductDetail,
  CatalogProductSku,
} from "../types/catalog.types.ts";

export type VariantSelection = Record<string, string>;

export interface VariantOption {
  value: string;
  label: string;
  available: boolean;
  outOfStock: boolean;
}

function orderedDefinitions(definitions: CatalogAttributeDefinition[]) {
  return [...definitions].sort((left, right) => left.sortOrder - right.sortOrder);
}

export function normalizeVariantValue(value: CatalogAttributeValue): string {
  return `${typeof value}:${String(value)}`;
}

export function formatVariantValue(value: CatalogAttributeValue, unit?: string | null): string {
  const display = typeof value === "boolean" ? (value ? "Có" : "Không") : String(value);
  return unit ? `${display} ${unit}` : display;
}

function matchesSelection(
  sku: CatalogProductSku,
  definitions: CatalogAttributeDefinition[],
  selection: VariantSelection,
) {
  return definitions.every((definition) => {
    const selected = selection[definition.code];
    if (!selected) return true;
    const actual = sku.attributes[definition.code];
    return actual !== undefined && normalizeVariantValue(actual) === selected;
  });
}

export function getInitialVariantSelection(product: CatalogProductDetail): VariantSelection {
  if (product.skus.length !== 1) return {};

  const sku = product.skus[0];
  return Object.fromEntries(
    orderedDefinitions(product.variantDefinitions).flatMap((definition) => {
      const value = sku.attributes[definition.code];
      return value === undefined ? [] : [[definition.code, normalizeVariantValue(value)]];
    }),
  );
}

export function getVariantOptions(
  product: CatalogProductDetail,
  selection: VariantSelection,
  dimensionIndex: number,
): VariantOption[] {
  const definitions = orderedDefinitions(product.variantDefinitions);
  const definition = definitions[dimensionIndex];
  if (!definition) return [];

  const priorDefinitions = definitions.slice(0, dimensionIndex);
  const values = new Map<string, CatalogAttributeValue>();
  for (const sku of product.skus) {
    const value = sku.attributes[definition.code];
    if (value !== undefined) values.set(normalizeVariantValue(value), value);
  }

  return [...values.entries()]
    .map(([value, rawValue]) => {
      const candidates = product.skus.filter(
        (sku) =>
          matchesSelection(sku, priorDefinitions, selection) &&
          sku.attributes[definition.code] !== undefined &&
          normalizeVariantValue(sku.attributes[definition.code]) === value,
      );
      return {
        value,
        label: formatVariantValue(rawValue, definition.unit),
        available: candidates.length > 0,
        outOfStock: candidates.length > 0 && candidates.every((sku) => sku.stock <= 0),
      };
    })
    .sort((left, right) => left.label.localeCompare(right.label, "vi", { numeric: true }));
}

export function selectVariantValue(
  definitions: CatalogAttributeDefinition[],
  current: VariantSelection,
  code: string,
  rawValue: CatalogAttributeValue,
): VariantSelection {
  const ordered = orderedDefinitions(definitions);
  const changedIndex = ordered.findIndex((definition) => definition.code === code);
  if (changedIndex < 0) return current;

  const next: VariantSelection = {};
  for (const definition of ordered.slice(0, changedIndex)) {
    if (current[definition.code]) next[definition.code] = current[definition.code];
  }
  next[code] = normalizeVariantValue(rawValue);
  return next;
}

export function selectNormalizedVariantValue(
  definitions: CatalogAttributeDefinition[],
  current: VariantSelection,
  code: string,
  normalizedValue: string,
): VariantSelection {
  const ordered = orderedDefinitions(definitions);
  const changedIndex = ordered.findIndex((definition) => definition.code === code);
  if (changedIndex < 0) return current;

  const next: VariantSelection = {};
  for (const definition of ordered.slice(0, changedIndex)) {
    if (current[definition.code]) next[definition.code] = current[definition.code];
  }
  next[code] = normalizedValue;
  return next;
}

export function resolveSelectedSku(
  product: CatalogProductDetail,
  selection: VariantSelection,
): CatalogProductSku | null {
  if (product.skus.length === 1) return product.skus[0];

  const definitions = orderedDefinitions(product.variantDefinitions);
  if (definitions.length === 0 || definitions.some((definition) => !selection[definition.code])) {
    return null;
  }

  const matches = product.skus.filter((sku) => matchesSelection(sku, definitions, selection));
  return matches.length === 1 ? matches[0] : null;
}

export function clampProductQuantity(
  requestedQuantity: number,
  sku: CatalogProductSku | null | undefined,
): number {
  if (!sku || sku.stock <= 0) return 0;
  const quantity = Number.isFinite(requestedQuantity) ? Math.floor(requestedQuantity) : 1;
  return Math.min(Math.max(quantity, 1), sku.stock);
}

export function getProductDetailPath(slug: string): string {
  return `/products/${encodeURIComponent(slug)}`;
}

export function buildCartItemDetails(product: CatalogProductDetail, sku: CatalogProductSku) {
  return {
    productName: product.name,
    imageUrl: sku.primaryImageUrl || sku.images[0]?.url || product.primaryImageUrl || "",
    unitPrice: sku.price,
    skuCode: sku.sku,
  };
}
