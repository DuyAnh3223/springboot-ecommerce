---
name: backend-architecture
description: Primary architectural reference for Spring Boot backend. Use when creating or modifying backend services, controllers, entities, repositories, handling exceptions, adding AWS S3 & CloudFront storage, or writing backend unit/integration tests for auth, user, product, category, cart, order, inventory, or voucher modules.
---

# Backend Architecture Skill Index

> **Verified against commit `5e44606` on 2026-07-25**

Welcome to the Spring Boot backend architecture knowledge base for ABTechZone (`springboot-ecommerce`).

## Topic Index

1. **[Modules & Domain Reference](file:///d:/dev/ABTechZone/.agents/skills/backend-architecture/modules-reference.md)**
   - Comprehensive specifications for all 8 business modules (`auth`, `user`, `product`, `category`, `cart`, `order`, `inventory`, `voucher`) and the `common` infrastructure package (`AwsS3FileService`, CloudFront CDN & Signed URLs, `ErrorCode`, `GlobalExceptionHandler`).

2. **[Checkout & Distributed Transaction Flow](file:///d:/dev/ABTechZone/.agents/skills/backend-architecture/checkout-flow.md)**
   - Detailed sequence diagrams and step-by-step logic for `OrderService.createOrder(...)`, Redisson distributed locking (`tryLock`), transaction boundaries, voucher validation, inventory reservation, and explicit failure/rollback compensating actions.

3. **[Testing Guidelines & Mockito Patterns](file:///d:/dev/ABTechZone/.agents/skills/backend-architecture/testing-guidelines.md)**
   - Standardized unit testing patterns using `@ExtendWith(MockitoExtension.class)`, `@Mock AuthService`, MapStruct `@Spy` mappers, exact query method stubbing, and `lenient()` rationale to prevent `UnnecessaryStubbingException`.

## Maintenance Policy
If any method signature, entity relationship, or business rule in the backend codebase is modified, update the corresponding reference document in this skill directory within the same commit.
