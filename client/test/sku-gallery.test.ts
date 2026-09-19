import test from "node:test";
import assert from "node:assert/strict";
import nextConfig from "../next.config.ts";
import {
  fromSkuGalleryItems,
  prepareSkuImages,
  skuImagesToDraft,
  toSkuGalleryItems,
} from "../features/admin/catalog/products/utils/sku-gallery.utils.ts";

const storedImage = {
  id: 701,
  url: "products/retained.jpg",
  accessUrl: "https://cdn.example/products/retained.jpg",
  primary: true,
  sortOrder: 0,
  altText: null,
};

test("AC3 retains stored image identity through reload, gallery edits and save", async () => {
  const gallery = toSkuGalleryItems(skuImagesToDraft([storedImage]));
  assert.equal(gallery[0].id, storedImage.id);
  const requests = await prepareSkuImages(fromSkuGalleryItems(gallery), async () => {
    assert.fail("An existing image must not be uploaded again");
  });
  assert.deepEqual(requests, [{
    id: 701,
    url: "products/retained.jpg",
    primary: true,
    sortOrder: 0,
  }]);
});

test("AC3 keeps a newly selected file after another gallery update and uploads it once", async () => {
  const file = new File(["image bytes"], "new.jpg", { type: "image/jpeg" });
  let draft = fromSkuGalleryItems([{
    file,
    previewUrl: "blob:new-image",
    isPrimary: true,
    sortOrder: 0,
  }]);
  const reopened = toSkuGalleryItems(draft);
  draft = fromSkuGalleryItems(reopened.map((item) => ({ ...item, sortOrder: 1 })));
  const uploaded: File[] = [];
  const requests = await prepareSkuImages(draft, async (selected) => {
    uploaded.push(selected);
    return "products/new.jpg";
  });
  assert.deepEqual(uploaded, [file]);
  assert.equal(requests?.[0].url, "products/new.jpg");
  assert.equal(requests?.[0].sortOrder, 1);
});

test("AC3 preserves retained image IDs when adding and changing the primary image", async () => {
  const file = new File(["new image"], "second.jpg", { type: "image/jpeg" });
  const existing = toSkuGalleryItems(skuImagesToDraft([storedImage]));
  const draft = fromSkuGalleryItems([
    { file, previewUrl: "blob:second", isPrimary: true, sortOrder: 0 },
    { ...existing[0], isPrimary: false, sortOrder: 1 },
  ]);
  const requests = await prepareSkuImages(draft, async () => "products/second.jpg");
  assert.equal(requests?.[0].id, undefined);
  assert.equal(requests?.[0].primary, true);
  assert.equal(requests?.[1].id, 701);
  assert.equal(requests?.[1].primary, false);
});

test("AC3 sends an explicit empty array when every gallery image is removed", async () => {
  const requests = await prepareSkuImages(fromSkuGalleryItems([]), async () => {
    assert.fail("Clearing a gallery must not upload files");
  });
  assert.equal(JSON.stringify({ images: requests }), '{"images":[]}');
});

test("AC4 accepts the editor's 5 MiB file plus multipart overhead at the Server Action boundary", () => {
  const fileLimit = 5 * 1024 * 1024;
  const requestLimit = nextConfig.experimental?.serverActions?.bodySizeLimit ?? 1024 * 1024;
  assert.equal(typeof requestLimit, "number");
  assert.ok(Number(requestLimit) >= fileLimit + 20 * 1024);
});
