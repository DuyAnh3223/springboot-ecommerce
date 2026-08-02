import { useEffect, useState, useTransition } from "react";
import { useRouter, usePathname, useSearchParams } from "next/navigation";

export function useProductFilters() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const [isPending, startTransition] = useTransition();

  // Search, filter state
  const [searchTerm, setSearchTerm] = useState(searchParams.get("search") || "");
  const [selectedCategory, setSelectedCategory] = useState(
    searchParams.get("categoryId") || ""
  );
  const [selectedStatus, setSelectedStatus] = useState(
    searchParams.get("status") || "all"
  );

  // Pagination & Sorting parameters (preserved for future use as per refactor plan)
  const currentPage = parseInt(searchParams.get("page") || "1");
  const currentSize = parseInt(searchParams.get("size") || "10");
  const currentSortBy = searchParams.get("sortBy") || "name";
  const currentOrder = searchParams.get("order") || "desc";

  // Debounced search trigger
  useEffect(() => {
    const timer = setTimeout(() => {
      const urlSearch = searchParams.get("search") || "";
      if (searchTerm !== urlSearch) {
        updateQueryParams({ search: searchTerm, page: 1 });
      }
    }, 500);
    return () => clearTimeout(timer);
  }, [searchTerm]);

  useEffect(() => {
    setSearchTerm(searchParams.get("search") || "");
  }, [searchParams]);

  const updateQueryParams = (
    newParams: Record<string, string | number | boolean | null | undefined>
  ) => {
    const current = new URLSearchParams(Array.from(searchParams.entries()));
    Object.entries(newParams).forEach(([key, value]) => {
      if (value === null || value === undefined || value === "") {
        current.delete(key);
      } else {
        current.set(key, String(value));
      }
    });
    startTransition(() => {
      router.push(`${pathname}?${current.toString()}`);
    });
  };

  return {
    searchTerm,
    setSearchTerm,
    selectedCategory,
    setSelectedCategory,
    selectedStatus,
    setSelectedStatus,
    isPending,
    updateQueryParams,
    currentPage,
    currentSize,
    currentSortBy,
    currentOrder,
  };
}
