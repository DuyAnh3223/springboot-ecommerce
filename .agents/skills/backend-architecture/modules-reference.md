# Backend Modules & Domain Reference

> Reference snapshot: verify relevant symbols against current code and tests.

## 1. Module Overview Table

| Module | Primary Services | Key Entities / Objects | Primary Responsibilities |
|---|---|---|---|
| **auth** | `AuthService` | `User`, `Role`, `Permission`, `UserRole`, `UserRoleId`, `InvalidatedToken` | JWT issuing (HS512), introspection, token invalidation, authentication context helper methods |
| **user** | `UserService`, `AddressService` | `User`, `Address` | User CRUD, profile retrieval, shipping address management with ownership validation |
| **product** | `ProductService`, `ProductSkuService`, `SkuImageService`, `SkuVariantPreviewCalculator`, `ProductAttributeValidator` | `Product`, `ProductSku`, `ProductImage`, `AttributeUtils` | Catalog management, dynamic variant SKU generation, price & stock aggregation, S3 gallery sync |
| **category** | `CategoryService`, `AttributeService` | `Category`, `Attribute`, `CategoryAttribute` | Parent-child taxonomy, category-attribute mapping, slug generation |
| **cart** | `CartService` | `Cart`, `CartItem` | Active cart management (`findByUserIdAndStatus`), quantity accumulation, price sync |
| **order** | `OrderService` | `Order`, `OrderItem`, `OrderStatusHistory` | Checkout execution, Redisson lock coordination, status transition lifecycle |
| **inventory** | `InventoryService` | `ProductSku`, `Reservation` | Stock deduction, stock reservation locking & release |
| **voucher** | `VoucherService`, `VoucherValidator` | `Voucher`, `VoucherType`, `VoucherApplyScope` | Coupon validation (PERCENTAGE / FIXED), min order threshold, per-user usage caps |
| **common** | `AwsS3FileService` | `AwsS3FileResponse`, `AwsS3AccessUrlResponse`, `ErrorCode` | Storage (dual-mode CloudFront public/signed URL), exception handling, app initialization |

---

## 2. Module Specifications

### 2.1 Auth Module (`spring.abtechzone.modules.auth`)
- **`AuthService`**:
  - `isAuthenticated()`: Checks if current `SecurityContext` has non-null, authenticated, non-anonymous token.
  - `validateAuthenticated()`: Throws `AppException(ErrorCode.UNAUTHENTICATED)` if not authenticated.
  - `getCurrentUsername()`: Validates auth, checks non-null `Authentication`, and returns `authentication.getName()`.
  - Configurable properties:
    - `@Value("${jwt.signerKey}") protected String signerKey;`
    - `@Value("${jwt.valid-duration}") protected Long validDuration;`
    - `@Value("${jwt.refreshable-duration}") protected Long refreshableDuration;`

### 2.2 User Module (`spring.abtechzone.modules.user`)
- **`UserService`**:
  - `createUser(UserCreationRequest)`: Encodes password, assigns default `PredefinedRole.USER_ROLE` with composite key `UserRoleId`.
  - `getMyInfo()`, `getCurrentUser()`: Resolves authenticated user via `authService.getCurrentUsername()`.
- **`AddressService`**:
  - Ownership validation: Throws `AppException(ErrorCode.ADDRESS_NOT_BELONG_TO_USER)` or `ErrorCode.ACCESS_DENIED` if requested address does not belong to the current authenticated user.

### 2.3 Product Module (`spring.abtechzone.modules.product`)
- **`ProductService` & `ProductSkuService`**:
  - `Product`: Aggregates `skuCount`, `activeSkuCount`, `totalStock`, `priceMin`, `priceMax`.
  - `ProductSku`: Unique SKU code, price, stock, attributes (JSON/Map), `isActive`.
  - `SkuImageService`: Public service handling SKU multi-image gallery synchronization (`syncSkuImages`), enforcing PATCH semantics, and invoking `s3ObjectLifecycleHelper.deleteAfterCommit(...)` for obsolete gallery images. Explicitly flushes deletes and non-primary updates before saving new images to prevent PostgreSQL partial unique constraint violations (`idx_product_image_sku_primary`).
  - `SkuVariantPreviewCalculator`: Public helper encapsulating ENUM attribute validation and Cartesian-product SKU preview generation.
  - MapStruct Mappers (`ProductMapper`, `ProductSkuMapper`, `ProductImageMapper`): Pure DTO/entity mappers. `ProductService` and `ProductSkuService` resolve S3 access URLs directly using `AwsS3FileService.resolveAccessUrl(...)`.
  - `ProductAttributeValidator`: Validates variant combinations against assigned category attributes. Modularized with Java 16+ pattern matching and type-specific scalar validators (`validateStringScalar`, `validateNumberScalar`, `validateBooleanScalar`, `validateEnumScalar`).
  - `AttributeUtils`: Utility class (`spring.abtechzone.modules.product.util.AttributeUtils`) providing `extractAllowedEnumValues(Attribute)` to share allowed ENUM value extraction across validators and calculators.
  - Product JSON contract uses `draft`, `published`, and `skus`; legacy `isDraft`, `isPublished`, and `productSkus` keys are not accepted or returned.
  - `ProductSkuUpdateRequest.images` is PATCH-aware without `JsonNullable`: omit it to preserve the gallery, send `[]` to clear it, or send the final image list to synchronize it. Explicit JSON `null` is rejected.

### 2.4 Category Module (`spring.abtechzone.modules.category`)
- **`CategoryService`**:
  - Tree structure: Parent-child category relations (`parent_id`).
  - `CategoryAttribute`: Defines required/optional attributes per category.

### 2.5 Cart Module (`spring.abtechzone.modules.cart`)
- **`CartService`**:
  - Active Cart Resolution: Calls `cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)`.
  - `addToCart(CartItemRequest)`: Validates request format, creates cart if absent, accumulates quantity if SKU exists, or appends new `CartItem`, validating cumulative quantity against current `ProductSku.stock`.
  - `getCart()`: Synchronizes unit prices from current `ProductSku.price` on read and persists synced prices to the database.

### 2.6 Order Module (`spring.abtechzone.modules.order`)
- **`OrderService`**:
  - Main entry: `createOrder(CreateOrderRequest)`.
  - Uses Redisson distributed locks and `TransactionTemplate.execute(...)`. (See `checkout-flow.md` for full sequence details).

### 2.7 Inventory Module (`spring.abtechzone.modules.inventory`)
- **`InventoryService`**:
  - Atomic stock validation and reservation holding during checkout execution.

### 2.8 Voucher Module (`spring.abtechzone.modules.voucher`)
- **`VoucherService` & `VoucherValidator`**:
  - `VoucherType`: `PERCENTAGE` or `FIXED_AMOUNT`.
  - `VoucherApplyScope`: `ALL`, `SPECIFIC`.
  - `VoucherValidator`: Validates start/end dates, `minOrderValue`, `maxUses`, and per-user usage limits (`maxPerUser`).

### 2.9 Common Package (`spring.abtechzone.common`)
- **`AwsS3FileService`**:
  - Upload API: `upload(MultipartFile file, String folderName)` is the sole upload method and stores at `${folderName}/${uuid}`.
  - Public Folders Config: Read from property key `aws.s3.public-folders` (fallback defaults: `products`, `categories`, `avatars`).
  - Expiration Config: Read from property key `aws.s3.presigned-url-expiration` (default `60` minutes).
  - CloudFront CDN & Signing Config:
    - `@Value("${cloudfront.url:}") String cloudfrontUrl;`
    - `@Value("${cloudfront.key-pair-id:}") String keyPairId;`
    - `@Value("${cloudfront.private-key:}") String privateKeyContent;`
  - Dual-mode resolution:
    - **Public folder key** ➔ Returns CloudFront CDN URL (`${cloudfront.url}/${fileKey}`). Direct S3 fallback disabled to enforce OAC.
    - **Private folder key** ➔ Generates CloudFront Signed URL with Canned Policy using `CloudFrontUtilities.getSignedUrlWithCannedPolicy(...)` signed via RSA/EC PKCS#8 `cachedPrivateKey` and `keyPairId`.
- **`GlobalExceptionHandler` & `ErrorCode`**:
  - Catches `AppException` and maps `ErrorCode` (containing `code`, `message`, `httpStatus`) to standard JSON response DTO.
