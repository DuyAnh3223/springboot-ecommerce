# Project Rules & Architecture Overview

> **Verified against commit `5e44606` on 2026-07-25**

## Architecture Overview
This is a Spring Boot 3 + Java 21 e-commerce backend built with a Layered Domain Architecture:
`Controller (@RestController)` -> `Service (@Service)` -> `Repository (JpaRepository)` / `Mapper (MapStruct)` -> `Entity (JPA)`.

## Golden Rules (Non-negotiable)

1. **Authentication Rule**:
   Always use `AuthService` (`isAuthenticated()`, `validateAuthenticated()`, `getCurrentUsername()`) for security checks and retrieving current user context. Never invoke `SecurityContextHolder` directly in controllers or services.

2. **Exception Handling Rule**:
   Always re-throw `AppException` in catch blocks. Never swallow exceptions or re-wrap business exceptions into generic `RuntimeException`.

3. **Service Layer Boundary Rule**:
   All business logic, validation, transaction boundaries, and data transformations reside strictly in `@Service` classes under `spring.abtechzone.modules.<feature>.service`. `@RestController` classes must remain thin and only handle HTTP request mapping and DTO delegation.

## Maintenance & Regeneration Policy
When modifying method signatures or domain logic of classes listed in the backend reference documentation, update the corresponding documentation file in `.agents/skills/backend-architecture/` in the same commit/PR.

## Deep-Dive Technical Documentation
For detailed domain models, 8-module specs, checkout sequence diagrams, and unit test patterns, refer to:
👉 [backend-architecture SKILL](file:///d:/dev/ABTechZone/.agents/skills/backend-architecture/SKILL.md)
