"use client";

import { useSearchParams, useRouter, usePathname } from "next/navigation";
import { useCallback, useMemo } from "react";
import { CatalogFilterParams } from "@/features/customer/catalog/types/catalog.types";

export function useCatalogFilters() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();

  const filters = useMemo<CatalogFilterParams>(() => {
    const search = searchParams.get("search") || undefined;
    const brandId = searchParams.get("brandId") ? Number(searchParams.get("brandId")) : undefined;
    const minPrice = searchParams.get("minPrice") ? Number(searchParams.get("minPrice")) : undefined;
    const maxPrice = searchParams.get("maxPrice") ? Number(searchParams.get("maxPrice")) : undefined;
    const inStock = searchParams.get("inStock") === "true" ? true : undefined;
    const page = searchParams.get("page") ? Number(searchParams.get("page")) : 1;
    const size = searchParams.get("size") ? Number(searchParams.get("size")) : 20;
    const sortBy = searchParams.get("sortBy") || "name";
    const order = (searchParams.get("order") as "asc" | "desc") || "asc";

    const attributes: Record<string, string[]> = {};
    searchParams.forEach((value, key) => {
      if (key.startsWith("attr_")) {
        const attrCode = key.substring(5);
        attributes[attrCode] = value.split(",").filter(Boolean);
      }
    });

    return { search, brandId, minPrice, maxPrice, inStock, page, size, sortBy, order, attributes };
  }, [searchParams]);

  const updateFilters = useCallback(
    (newParams: Partial<CatalogFilterParams>) => {
      const params = new URLSearchParams(searchParams.toString());

      // Reset to page 1 whenever filters change unless explicitly page update
      if (!("page" in newParams)) {
        params.set("page", "1");
      }

      Object.entries(newParams).forEach(([key, val]) => {
        if (key === "attributes" && val && typeof val === "object") {
          // Remove old attr_ keys
          Array.from(params.keys()).forEach((k) => {
            if (k.startsWith("attr_")) params.delete(k);
          });
          Object.entries(val as Record<string, string[]>).forEach(([attrCode, values]) => {
            if (values && values.length > 0) {
              params.set(`attr_${attrCode}`, values.join(","));
            }
          });
        } else if (val === undefined || val === null || val === "" || val === false) {
          params.delete(key);
        } else {
          params.set(key, String(val));
        }
      });

      router.push(`${pathname}?${params.toString()}`, { scroll: false });
    },
    [searchParams, router, pathname]
  );

  const clearAllFilters = useCallback(() => {
    router.push(pathname, { scroll: false });
  }, [router, pathname]);

  return { filters, updateFilters, clearAllFilters };
}
