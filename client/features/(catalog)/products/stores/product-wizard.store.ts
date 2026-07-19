import { create } from "zustand";
import { CategoryAttributeResponse } from "@/features/(catalog)/attributes/attribute.type";

interface ProductWizardState {
  activeStep: number;
  savedProductId: number | null;
  categoryAttributes: CategoryAttributeResponse[];
  loadingAttributes: boolean;
  error: string | null;
  nonVariantValues: Record<string, any>;
  selectedVariants: Record<string, string[]>;
  newCustomTag: Record<string, string>;
  setActiveStep: (step: number) => void;
  setSavedProductId: (id: number | null) => void;
  setCategoryAttributes: (attrs: CategoryAttributeResponse[]) => void;
  setLoadingAttributes: (loading: boolean) => void;
  setError: (err: string | null) => void;
  setNonVariantValues: (v: Record<string, any>) => void;
  setSelectedVariants: (v: Record<string, string[]>) => void;
  setNewCustomTag: (v: Record<string, string>) => void;
  resetWizard: () => void;
}

export const useProductWizardStore = create<ProductWizardState>((set) => ({
  activeStep: 1,
  savedProductId: null,
  categoryAttributes: [],
  loadingAttributes: false,
  error: null,
  nonVariantValues: {},
  selectedVariants: {},
  newCustomTag: {},
  setActiveStep: (step) => set({ activeStep: step }),
  setSavedProductId: (id) => set({ savedProductId: id }),
  setCategoryAttributes: (attrs) => set({ categoryAttributes: attrs }),
  setLoadingAttributes: (loading) => set({ loadingAttributes: loading }),
  setError: (err) => set({ error: err }),
  setNonVariantValues: (v) => set({ nonVariantValues: v }),
  setSelectedVariants: (v) => set({ selectedVariants: v }),
  setNewCustomTag: (v) => set({ newCustomTag: v }),
  resetWizard: () =>
    set({
      activeStep: 1,
      savedProductId: null,
      categoryAttributes: [],
      loadingAttributes: false,
      error: null,
      nonVariantValues: {},
      selectedVariants: {},
      newCustomTag: {},
    }),
}));
