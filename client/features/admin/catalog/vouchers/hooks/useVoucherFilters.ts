import { useCallback, useEffect, useState, useTransition } from "react";
import { useRouter, usePathname, useSearchParams } from "next/navigation";

export function useVoucherFilters() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const [isPending, startTransition] = useTransition();

  const [searchTerm, setSearchTerm] = useState(searchParams.get("search") || "");
  const selectedStatus = searchParams.get("status") || "all";

  const currentPage = parseInt(searchParams.get("page") || "1");
  const currentSize = parseInt(searchParams.get("size") || "10");
  const currentSortBy = searchParams.get("sortBy") || "id";
  const currentOrder = searchParams.get("order") || "desc";

  const updateQueryParams = useCallback(
    (newParams: Record<string, string | number | boolean | null | undefined>) => {
      const current = new URLSearchParams(Array.from(searchParams.entries()));
      Object.entries(newParams).forEach(([key, value]) => {
        if (value === null || value === undefined || value === "" || value === "all") {
          current.delete(key);
        } else {
          current.set(key, String(value));
        }
      });
      startTransition(() => {
        router.push(`${pathname}?${current.toString()}`);
      });
    },
    [searchParams, pathname, router, startTransition]
  );

  useEffect(() => {
    const timer = setTimeout(() => {
      const currentSearch = searchParams.get("search") || "";
      if (searchTerm !== currentSearch) {
        updateQueryParams({ search: searchTerm, page: 1 });
      }
    }, 500);
    return () => clearTimeout(timer);
  }, [searchTerm, searchParams, updateQueryParams]);

  const handleStatusChange = (status: string) => {
    if (status === "disabled") {
      updateQueryParams({ status: "disabled", active: "false", page: 1 });
    } else if (status === "active") {
      updateQueryParams({ status: "active", active: "true", page: 1 });
    } else if (status === "expired") {
      updateQueryParams({ status: "expired", active: "true", page: 1 });
    } else {
      updateQueryParams({ status: null, active: null, page: 1 });
    }
  };

  return {
    searchTerm,
    setSearchTerm,
    selectedStatus,
    handleStatusChange,
    isPending,
    updateQueryParams,
    currentPage,
    currentSize,
    currentSortBy,
    currentOrder,
  };
}
