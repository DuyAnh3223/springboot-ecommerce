# Product and SKU updates with legacy attributes

## Status

Accepted for implementation from the user's explicit 2026-09-12 requirement:
fix the shared logic so similar products/SKUs can add images and update later,
instead of repairing one product's database row. Human acceptance: UAT PENDING.

## Evidence

Product 114 PATCH returned HTTP 400 / code 1011 before file upload. Its existing
SKU contains `{"color":"Standard"}` although its CPU category has no `color`
definition. Product PATCH and SKU reconciliation both validate unchanged SKU
attributes. Existing image IDs and local files are also lost in form mappings.

## Requirements and acceptance criteria

- R1 / AC1: Metadata and image updates on existing products/SKUs must preserve
  omitted or unchanged attributes, including legacy attributes, without failing
  solely because those unchanged values do not satisfy today's category rules.
  Cover product PATCH, SKU PATCH and existing-SKU reconciliation.
- R2 / AC2: Submitted attribute changes and newly created products/SKUs still
  require current category/type/required-field validation; changed SKU variants
  must still satisfy uniqueness. Product category remains immutable.
- R3 / AC3: First upload, later additions, reorder/primary changes and removal
  work for single and multiple SKUs. Preserve image IDs for retained images and
  local File references until upload; an explicit empty gallery clears images.
- R4 / AC4: Files within the editor's existing 5 MiB per-file limit can cross
  the Next Server Action and backend multipart limits, including form overhead.

## Boundaries

No database/seed repair, automatic removal of legacy values, schema migration,
new endpoint, inventory ownership change or authorization change. Existing
upload validation, ten-image limit, one-primary-image rule and transactional
image deletion remain applicable. Attribute maps are validated as a whole when
changed; this does not allow adding new invalid attributes.

## Verification

Use service unit tests with real mappers/attribute validation for the compatibility
branches and pure frontend tests for gallery transformations. Existing product,
image and storage suites provide surrounding checks. Database transactions and
S3 deletion implementation are unchanged; real upload/browser acceptance needs
authenticated UAT. No claim of live AWS acceptance from mocked tests.

## Manual acceptance

1. Reload a legacy product, add its first image and save successfully.
2. Reload, add a second image, reorder and change primary, save and reload;
   retained images must continue loading.
3. Clear the gallery, save and reload; verify it is empty.
4. Repeat single-SKU and multi-SKU editors and a file between 1 and 5 MiB.
5. Change attributes to an invalid value and confirm validation still rejects it.
