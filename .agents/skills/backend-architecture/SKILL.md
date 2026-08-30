---
name: backend-architecture
description: Primary architectural reference for Spring Boot backend. Use when creating or modifying backend services, controllers, entities, repositories, handling exceptions, adding AWS S3 & CloudFront storage, or writing backend unit/integration tests for auth, user, product, category, cart, order, inventory, or voucher modules.
---

# Backend Architecture Skill Index

These references are architecture aids, not a replacement for current code,
tests, accepted specs, or ADRs. Verify the relevant symbols before relying on a
reference for implementation.

Welcome to the Spring Boot backend architecture knowledge base for ABTechZone (`springboot-ecommerce`).

## Topic Index

1. **[Modules & Domain Reference](modules-reference.md)**
   - Comprehensive specifications for all 8 business modules (`auth`, `user`, `product`, `category`, `cart`, `order`, `inventory`, `voucher`) and the `common` infrastructure package (`AwsS3FileService`, CloudFront CDN & Signed URLs, `ErrorCode`, `GlobalExceptionHandler`).

2. **[Checkout & Distributed Transaction Flow](checkout-flow.md)**
   - Detailed sequence diagrams and step-by-step logic for
     `OrderCreationService.createOrder(...)`, its shared `CheckoutService`
     computation, Redisson distributed locking (`tryLock`), transaction
     boundaries, voucher validation, inventory reservation, and explicit
     failure/rollback actions.

3. **[Testing Guidelines & Mockito Patterns](testing-guidelines.md)**
   - Standardized unit testing patterns using `@ExtendWith(MockitoExtension.class)`, `@Mock AuthService`, MapStruct `@Spy` mappers, exact query method stubbing, and `lenient()` rationale to prevent `UnnecessaryStubbingException`.

## Maintenance Policy
Update the corresponding reference when a change invalidates documented module
ownership, entity relationships, business invariants, transaction boundaries,
or reusable testing guidance. Do not require documentation churn for an
ordinary method signature that does not change those concepts.
