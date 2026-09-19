import type { ProductImage, ProductImageRequest } from "@/features/skus/sku.type";
import type { SkuImageDraft } from "@/features/products/types/sku.draft.type";
import type { SkuGalleryItem } from "../components/SkuGalleryDialog";

export function skuImagesToDraft(images: ProductImage[] = []): SkuImageDraft[] {
  return images.map((image) => ({
    id: image.id,
    url: image.url,
    previewUrl: image.accessUrl || image.url,
    isPrimary: Boolean(image.primary),
    sortOrder: image.sortOrder,
  }));
}

export function toSkuGalleryItems(images: SkuImageDraft[] = []): SkuGalleryItem[] {
  return images.map((image, index) => ({
    id: image.id,
    url: image.url,
    file: image.file,
    previewUrl: image.previewUrl || image.url,
    isPrimary: Boolean(image.isPrimary),
    sortOrder: image.sortOrder ?? index,
  }));
}

export function fromSkuGalleryItems(items: SkuGalleryItem[]): SkuImageDraft[] {
  return items.map((item) => ({
    id: item.id,
    url: item.url || item.previewUrl,
    previewUrl: item.previewUrl,
    file: item.file,
    isPrimary: item.isPrimary,
    sortOrder: item.sortOrder,
  }));
}

export async function prepareSkuImages(
  images: SkuImageDraft[] = [],
  upload: (file: File) => Promise<string>,
): Promise<ProductImageRequest[]> {
  const requests: ProductImageRequest[] = [];
  for (const image of images) {
    const url = image.file ? await upload(image.file) : image.url;
    requests.push({
      id: image.id,
      url,
      sortOrder: image.sortOrder ?? 0,
      primary: Boolean(image.isPrimary),
    });
  }
  return requests;
}
