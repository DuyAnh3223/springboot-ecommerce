import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ProductSchema, ProductFormValues } from "../schemas/product.schema";
import { ProductResponse } from "../product.type";
import { slugify } from "@/lib/slugify";

export function useProductBasicInfoForm(product?: ProductResponse | null) {
  const form = useForm<ProductFormValues>({
    resolver: zodResolver(ProductSchema),
    defaultValues: { name: "", slug: "", thumbnail: "", categoryId: 0, description: "" },
  });

  const { register, watch, setValue, formState: { errors }, reset } = form;

  const selectedCategoryId = watch("categoryId");
  const productName = watch("name");
  const productSlug = watch("slug");
  const productThumbnail = watch("thumbnail");
  const productDescription = watch("description");

  const isEdit = !!product;

  // Auto-generate slug from product name in create mode
  useEffect(() => {
    if (!isEdit && productName) {
      const slug = slugify(productName, "-");
      setValue("slug", slug, { shouldValidate: true });
    }
  }, [productName, isEdit, setValue]);

  return {
    form,
    register,
    errors,
    setValue,
    reset,
    isEdit,
    productName,
    productSlug,
    productThumbnail,
    productDescription,
    selectedCategoryId,
  };
}
