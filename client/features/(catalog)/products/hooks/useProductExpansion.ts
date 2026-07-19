import { useState } from "react";
import { ProductResponse } from "../product.type";
import { getProductAction } from "../actions";

export function useProductExpansion() {
  const [expandedIds, setExpandedIds] = useState<number[]>([]);
  const [loadedDetails, setLoadedDetails] = useState<Record<number, ProductResponse>>({});
  const [loadingDetails, setLoadingDetails] = useState<Record<number, boolean>>({});

  const isExpanded = (id: number) => expandedIds.includes(id);
  const isLoading = (id: number) => !!loadingDetails[id];
  const getDetail = (id: number) => loadedDetails[id];

  const toggleExpand = async (productId: number) => {
    const isCurrentlyExpanded = isExpanded(productId);
    if (isCurrentlyExpanded) {
      setExpandedIds((prev) => prev.filter((id) => id !== productId));
    } else {
      setExpandedIds((prev) => [...prev, productId]);
      if (!loadedDetails[productId]) {
        setLoadingDetails((prev) => ({ ...prev, [productId]: true }));
        try {
          const res = await getProductAction(productId);
          if (res.success && res.product) {
            setLoadedDetails((prev) => ({ ...prev, [productId]: res.product }));
          } else {
            console.error("Error loading product details:", res.error);
          }
        } catch (error) {
          console.error("Error loading product details:", error);
        } finally {
          setLoadingDetails((prev) => ({ ...prev, [productId]: false }));
        }
      }
    }
  };

  return {
    expandedIds,
    isExpanded,
    isLoading,
    getDetail,
    toggleExpand,
  };
}
