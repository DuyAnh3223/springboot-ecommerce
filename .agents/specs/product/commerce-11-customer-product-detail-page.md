# Commerce-11 — Customer Product Detail Page

## Status and authority

**Accepted for implementation** — explicitly accepted by the user on
2026-08-27.

Accepted by / date: user / 2026-08-27.

This document is the governing product contract for Commerce-11 implementation.

## Context

Customer catalog links and the multi-SKU add-to-cart action already navigate to
`/products/{slug}`, but the client has no matching route and currently renders a
Next.js 404. The homepage Quick View also sends its detail CTA to a category URL
using a product slug.

The backend has a mixed-audience `/products` controller, but `/products/**` is
not an anonymous HTTP-security boundary and also contains admin paths. The
customer product page therefore needs a dedicated, audience-safe read contract
under the existing public `/catalog/**` boundary.

## Current behavior and evidence

- `client/app/(customers)/products/[slug]/page.tsx` does not exist.
- `CatalogTable` and the multi-SKU branch of `AddToCartButton` already target
  `/products/${product.slug}`.
- `ProductQuickView` incorrectly targets `/category/${product.slug}`.
- Customer catalog services currently expose only category facets and product
  summaries.
- Backend catalog list behavior already limits results to published products in
  active categories.
- Backend product/SKU models already contain description, product attributes,
  SKU attributes, price, stock and image data needed by the page.
- Baseline on 2026-08-27: client TypeScript passed. Scoped catalog/home ESLint
  reported two pre-existing errors and seven warnings. There is no focused
  product-detail harness yet.

## Goal

Anonymous and authenticated customers can open a canonical product detail URL,
view current public product/SKU information, select a valid active SKU and
quantity, and add that exact SKU to the existing guest or authenticated cart.
The page must not expose unpublished, inactive-category or admin-only data.

## Requirements

### R-C11-01 — Canonical public route and page states

- The canonical customer URL is `/products/[slug]`.
- Anonymous and authenticated customers can open the URL directly, navigate to
  it from another customer page, and refresh it without authentication.
- The route is a Server Component and reads the product through the customer
  catalog service. It does not call the backend directly from a UI component.
- A backend product-not-found response renders the route-level Vietnamese
  not-found UI and is marked noindex by Next.js behavior.
- Transport, unavailable-service and unexpected server failures render a
  recoverable Vietnamese error state; they must not be disguised as product
  not found.
- The page provides a loading state that does not invent product values.

### R-C11-02 — Audience-safe public product contract

- Customer detail is read from `GET /catalog/products/{slug}` using the existing
  API result envelope.
- The endpoint permits anonymous reads but returns a product only when it is
  published, not soft-deleted and belongs to an active category.
- Missing, draft, unpublished, soft-deleted and inactive-category products all
  return the same `PRODUCT_NOT_FOUND`/HTTP 404 behavior without disclosing which
  condition failed.
- Only active SKUs are included. An active SKU with zero stock remains visible
  as out of stock; an inactive SKU is omitted entirely.
- The response uses a customer-owned DTO and excludes draft/published flags,
  internal storage keys, admin workflow fields and inactive SKU data.
- Product and SKU image values are resolved public access URLs. Gallery images
  are ordered by their declared sort order, with the primary image first when
  one exists.
- Customer `priceMin`, `priceMax` and `totalStock` are derived from the active
  SKU set represented by the response. Stock is never negative in the public
  response.

### R-C11-03 — Product information and gallery

- The page shows a Vietnamese breadcrumb, product name, brand when present,
  category, SKU-aware price, stock state, product description and public
  specifications.
- The initial gallery uses the product primary image. After a SKU is selected,
  its non-empty gallery becomes authoritative for the displayed gallery; if the
  selected SKU has no image, the product primary image remains the fallback.
- Missing images render a stable placeholder and meaningful alt text rather
  than a broken image.
- Product attributes render as specifications in the category metadata order
  when metadata is available; remaining public attributes use a deterministic
  key order. Null, blank and internal-only values are omitted.
- A real rating/review count may be shown when present. When there is no review
  data, the page shows no fabricated stars or score and may display
  “Chưa có đánh giá”.
- The layout is usable at mobile, tablet and desktop widths without horizontal
  page overflow.

### R-C11-04 — Deterministic SKU and variant selection

- Variant dimensions come only from active SKU attributes and public
  `variantDefinitions` supplied in category-attribute sort order.
- String, number and boolean attribute values are normalized deterministically
  for matching while their customer-facing labels and units remain readable.
- With exactly one active SKU, that SKU is selected automatically.
- With more than one active SKU, the customer selects the dimensions in the
  server-provided order. Changing a dimension clears incompatible selections in
  later dimensions so the UI cannot retain an impossible combination.
- An option is available when at least one active SKU matches the selections in
  all earlier dimensions plus that option. Combinations with no matching active
  SKU are disabled.
- An option represented only by zero-stock active SKUs remains visible and may
  be selected, but is marked “Hết hàng”. Completing that selection disables the
  add-to-cart action.
- The selected SKU is resolved only when all required dimensions identify
  exactly one active SKU. An incomplete, ambiguous or impossible selection
  never produces a cart request.
- Once resolved, the selected SKU controls the displayed SKU code, price,
  stock, stock state and SKU gallery.
- If the product has no active SKU, the product information remains visible
  with “Tạm hết hàng” and no add-to-cart action.

### R-C11-05 — Quantity and existing cart integration

- Quantity is an integer from 1 through the selected SKU's current displayed
  stock.
- Selecting a different SKU resets quantity to 1 when stock is positive; a
  zero-stock SKU has no valid quantity for submission.
- Decrement/increment and direct input, if provided, enforce the same bounds and
  expose an accessible label.
- Add-to-cart is disabled while selection is incomplete, the SKU is out of
  stock, quantity is invalid or the action is in flight.
- A valid action calls the existing cart capability with the exact selected
  `skuId`, quantity and display metadata needed by the current cart contract.
- Existing guest-cart and authenticated-cart behavior remains authoritative;
  the product page does not require login and does not introduce a second cart
  store or direct cart API call.
- The page does not calculate cart subtotal, shipping, voucher discount or
  checkout total. Cart and checkout server responses remain authoritative if
  price or stock changes after product render.

### R-C11-06 — Customer navigation integration

- Product-name links in customer catalog results use
  `/products/${product.slug}`.
- The existing multi-SKU add-to-cart branch navigates to the same URL so the
  customer can select a variant.
- Homepage product cards expose a clear path to the canonical product URL while
  Quick View may remain as an optional preview.
- The Quick View “Xem Chi Tiết & Mua Hàng” action uses the canonical product URL
  and never treats a product slug as a category slug.
- Customer navigation changes do not import admin UI or change category
  filtering, homepage ranking or cart calculation behavior.

### R-C11-07 — Metadata, accessibility and customer-safe errors

- `generateMetadata` produces a product-specific title, description, canonical
  path and Open Graph image when available. Missing optional data uses a stable
  ABTechZone fallback without fabricated claims.
- The route avoids duplicate detail requests within one server render where the
  framework/service boundary permits request memoization.
- Product data is refreshed on a new page request; the client does not persist
  product price/stock as authority in Zustand or local storage.
- Interactive variant choices expose their selected, unavailable and
  out-of-stock states to keyboard and assistive-technology users.
- Gallery controls, quantity controls and add-to-cart have accessible names,
  visible focus and disabled/loading states.
- New customer-facing copy is Vietnamese. Raw backend exception messages,
  storage keys and stack/internal details are never rendered.

## Acceptance criteria

### AC-C11-01 — Public deep link and refresh

Given a published product in an active category, when an anonymous or
authenticated customer opens `/products/{slug}` directly or refreshes it, then
the detail page renders without requiring login. Maps to R-C11-01 and R-C11-02.

### AC-C11-02 — Safe not-found boundary

Given a missing, draft, unpublished, soft-deleted or inactive-category product,
when its slug is requested, then the API returns the same 404 contract and the
page renders Vietnamese not-found/noindex without exposing product data. Maps
to R-C11-01 and R-C11-02.

### AC-C11-03 — Customer-safe detail response

Given a public product with active and inactive SKUs, when detail is requested,
then the response contains the specified customer fields, active SKUs and
resolved ordered images, while inactive SKUs and admin/internal fields are
absent. Maps to R-C11-02.

### AC-C11-04 — Product presentation without fabricated data

Given a product with optional brand, images, description, specifications and
rating data, when the page renders, then available real data is shown with
stable fallbacks and absent review data does not create a score or review
claim. Maps to R-C11-03 and R-C11-07.

### AC-C11-05 — Single-SKU and unavailable-product behavior

Given exactly one active SKU, when the page loads, then it is auto-selected and
its price/stock/image state is shown. Given no active SKU, product information
remains visible as unavailable and no cart request can be made. Maps to
R-C11-04 and R-C11-05.

### AC-C11-06 — Deterministic multi-SKU selection

Given a multi-SKU product, when the customer chooses variants in order, then
impossible combinations are disabled, changing an earlier choice clears later
incompatible choices, and only a complete unique combination resolves a SKU.
Maps to R-C11-04.

### AC-C11-07 — Out-of-stock variant

Given an active SKU with zero stock, when its combination is shown or selected,
then it is visibly marked “Hết hàng”, remains distinct from an impossible
combination, and add-to-cart stays disabled. Maps to R-C11-04 and R-C11-05.

### AC-C11-08 — Exact cart payload and duplicate protection

Given a selected in-stock SKU and valid quantity, when the customer adds it to
cart, then the existing cart capability receives that exact SKU ID and quantity
once while in flight; invalid quantity, incomplete selection and repeated click
cannot issue a competing request. Maps to R-C11-05.

### AC-C11-09 — Canonical customer navigation

Given the catalog table, multi-SKU cart CTA, homepage card and Quick View,
when the customer asks to see product detail, then each relevant path resolves
to `/products/{productSlug}` and never `/category/{productSlug}`. Maps to
R-C11-06.

### AC-C11-10 — Metadata and accessibility

Given a product page at mobile or desktop width, when rendered and operated by
keyboard, then metadata uses real product values/fallbacks, images have alt
text, controls expose selected/disabled/loading state, focus remains visible
and the page does not overflow horizontally. Maps to R-C11-03 and R-C11-07.

### AC-C11-11 — Failure separation

Given a backend 404 versus an unavailable or unexpected backend failure, when
the detail loader handles the response, then only the 404 becomes product
not-found; other failures show a recoverable Vietnamese error and do not render
raw backend messages. Maps to R-C11-01 and R-C11-07.

### AC-C11-12 — Verification boundary

Focused backend security/detail tests, frontend selection/cart/link tests,
TypeScript, changed-file lint and production build are executed where the
environment permits. Browser and live-backend acceptance remain `UAT PENDING`
until a human signs off. Maps to all requirements.

## Non-goals

- Review submission, review listing or generation of rating/review data.
- “Mua ngay”, wishlist, compare or related/recommended products in v1.
- Voucher, shipping, checkout, payment or order behavior changes.
- Product price/stock reservation or a guarantee that rendered values remain
  unchanged until checkout.
- Admin product CRUD, product publication workflow or admin authorization
  changes.
- Database migrations, product/SKU image upload or catalog/category redesign.
- Refactoring existing unrelated catalog lint debt.

## Edge cases

- Unknown, blank-equivalent, URL-encoded or malformed product slug.
- Published product whose category becomes inactive between list and detail.
- Product with no active SKU, one active SKU, or multiple active SKUs.
- Active SKU with zero stock; inactive SKU with historical image/stock data.
- SKU attributes with different object key order or string/number/boolean values.
- Variant value shared by both in-stock and out-of-stock SKU combinations.
- Missing product image, empty SKU gallery, broken/unresolvable optional image.
- Missing brand, description, product attributes, rating or review count.
- Price/stock changes after render or between selecting a SKU and adding it.
- Repeated add-to-cart click and cart request failure.
- API 404, validation error, timeout, unavailable backend and unexpected error.

## Domain invariants

- Publication, soft-delete state, category activity and SKU activity are
  enforced by the backend, not trusted from client filtering.
- The public detail DTO is audience-owned and cannot expose admin-only fields.
- Only a unique active SKU can be submitted to cart.
- Zero stock means unavailable for cart submission; it is not the same as an
  invalid SKU combination.
- Backend/cart/checkout remain authoritative for current price, stock and all
  order-affecting amounts.
- Customer product data is server-owned response state, not a Zustand cache.
- Customer UI and admin UI do not import each other's components or stores.

## API contract

### Request

```text
GET /catalog/products/{slug}
Authorization: not required
```

`slug` is the canonical product slug from catalog data. The response uses the
existing `ApiResult.result` envelope.

### Success response

Illustrative `result` payload:

```json
{
  "id": 101,
  "name": "AMD Ryzen 5 5500",
  "slug": "amd-ryzen-5-5500",
  "description": "Bộ xử lý AMD Ryzen 5 5500.",
  "primaryImageUrl": "https://cdn.example/products/ryzen-5-5500.webp",
  "rating": 4.8,
  "reviewCount": 12,
  "category": {
    "id": 7,
    "name": "CPU",
    "slug": "cpu"
  },
  "brand": {
    "id": 3,
    "name": "AMD",
    "slug": "amd"
  },
  "attributes": {
    "socket": "AM4",
    "core_count": 6
  },
  "specificationDefinitions": [
    {
      "code": "socket",
      "name": "Socket",
      "unit": null,
      "dataType": "STRING",
      "sortOrder": 10
    },
    {
      "code": "core_count",
      "name": "Số nhân",
      "unit": null,
      "dataType": "NUMBER",
      "sortOrder": 20
    }
  ],
  "variantDefinitions": [
    {
      "code": "color",
      "name": "Màu sắc",
      "unit": null,
      "dataType": "ENUM",
      "sortOrder": 10
    }
  ],
  "priceMin": 3429000,
  "priceMax": 3529000,
  "totalStock": 9,
  "skus": [
    {
      "id": 129,
      "sku": "AMD-R5-5500-BOX",
      "price": 3429000,
      "stock": 9,
      "currency": "VND",
      "weightGram": 450,
      "attributes": {
        "color": "BOX"
      },
      "primaryImageUrl": "https://cdn.example/products/ryzen-box.webp",
      "images": [
        {
          "id": 501,
          "url": "https://cdn.example/products/ryzen-box.webp",
          "altText": "AMD Ryzen 5 5500 bản BOX",
          "sortOrder": 0,
          "primary": true
        }
      ]
    }
  ]
}
```

Contract notes:

- `brand`, `description`, `primaryImageUrl`, `rating` and image `altText` may be
  null when source data is absent.
- `reviewCount` defaults to zero only when that is the stored/domain meaning;
  the UI must not infer a rating from it.
- `attributes`, definition lists and `skus` may be empty but not replaced with
  fabricated values.
- `priceMin`/`priceMax` may be null when there is no active SKU. `totalStock` is
  zero in that state.
- `images[].url` is the resolved customer access URL, not a private object key.

### Errors

- `404 PRODUCT_NOT_FOUND`: missing or any non-public product state; render
  route not-found.
- `400`: malformed request/path contract; show safe Vietnamese guidance or
  not-found according to the normalized customer loader contract.
- `429`: show a bounded retry message; do not render cached values as current.
- `500/503` or network timeout: render a recoverable Vietnamese error state;
  do not convert to 404 or expose the backend message.

## Security and authorization

- `GET /catalog/products/{slug}` is public through the already-public catalog
  boundary. No user ID or access token is required.
- Do not make `/products/**` broadly public because it shares a prefix with
  admin/product-management paths.
- Backend predicates enforce publication, category activity and soft deletion
  before mapping the response.
- Inactive SKUs and admin-only fields are removed server-side.
- Not-found behavior is intentionally indistinguishable across missing and
  non-public product states.
- Product description and attribute values are rendered as text/data; raw HTML
  is not injected unless a future accepted requirement defines sanitization.

## Data and persistence considerations

- No schema or data migration is required.
- Product/SKU/category/image tables remain the source of truth.
- No page response, selected SKU or price/stock snapshot is persisted in a new
  client store or backend table.
- Existing guest/authenticated cart persistence is reused unchanged.
- Any caching must not make a stale PDP response authoritative for cart or
  checkout. A new route request must be able to obtain current backend data.

## Verification strategy

### Unit

- Backend mapping/predicate tests for public product state, active SKU filtering,
  customer aggregates, attribute definitions and ordered resolved images.
- Frontend pure tests for dimension order, value normalization, selection
  cascade, unique SKU resolution, out-of-stock distinction, quantity bounds and
  exact cart payload.

### MVC / component

- Anonymous MVC test for the public endpoint and safe 404/error envelope.
- Customer UI harness for visible selected SKU state, disabled/in-flight cart
  action, metadata fallback and canonical link construction.

### Integration

- Catalog integration test with published/draft/inactive-category products and
  active/inactive/zero-stock SKUs.
- Existing catalog list, guest cart and checkout focused suites remain green.

### Static / build / runtime

- Run changed-file ESLint, TypeScript, focused product-detail harness and
  production build.
- Run GitNexus change detection before commits.
- Verify browser behavior against a live Spring backend separately; automated
  checks do not constitute human acceptance.

## Traceability

| Acceptance criterion | Automated evidence | Manual UAT | Status |
| --- | --- | --- | --- |
| AC-C11-01 | public endpoint MVC + route loader harness | Anonymous direct link and refresh | PLANNED |
| AC-C11-02 | backend public-state integration tests | Open missing/draft/inactive-category slugs | PLANNED |
| AC-C11-03 | DTO mapping and serialization tests | Inspect active/inactive SKU and gallery | PLANNED |
| AC-C11-04 | response/render mapping harness | Inspect missing optional content | PLANNED |
| AC-C11-05 | single/no-active-SKU tests | Add single SKU; inspect unavailable product | PLANNED |
| AC-C11-06 | variant-selection pure tests | Change dimensions across combinations | PLANNED |
| AC-C11-07 | stock-state pure/component tests | Select zero-stock variant | PLANNED |
| AC-C11-08 | cart payload/in-flight harness | Quantity and repeated-click cart action | PLANNED |
| AC-C11-09 | canonical-link harness | Navigate from catalog/home/Quick View | PLANNED |
| AC-C11-10 | metadata mapping + scoped lint/build | Keyboard and responsive inspection | PLANNED |
| AC-C11-11 | loader error mapping tests | Disconnect backend and retry | PLANNED |
| AC-C11-12 | verification command log | Human sign-off | PLANNED |

## Acceptance scenarios

1. An anonymous customer opens and refreshes a published single-SKU product,
   changes quantity and adds the exact SKU to the guest cart.
2. A customer opens a multi-SKU product, changes an earlier dimension, sees
   incompatible later choices clear, selects one valid combination and adds it.
3. A zero-stock active variant is visible as “Hết hàng” but cannot be added; an
   impossible combination is visibly distinct and cannot resolve a SKU.
4. A missing, draft, unpublished, soft-deleted or inactive-category slug returns
   the same safe not-found experience.
5. A product without image, description, brand or reviews renders stable
   fallbacks without fabricated content.
6. Catalog, homepage and Quick View navigation reach the canonical product URL
   on mobile and desktop.
7. If price/stock changes after render, cart/checkout responses remain
   authoritative and the page does not promise a reservation.

Acceptance status: `UAT PENDING`.

## Open questions

None for v1. Any addition of Buy Now, wishlist, compare, reviews,
recommendations, HTML product descriptions or price/stock reservation requires
an explicit requirement and a spec update before implementation.
