# Server Project Rules

- Use `AuthService` for authentication checks and current-user context. In production controllers/services, do not call `SecurityContextHolder` directly.
- Re-throw `AppException`; do not swallow or wrap business exceptions in generic runtime exceptions.
- Keep business logic, validation, transaction boundaries, and data transformations in `@Service` classes. Keep controllers thin HTTP/DTO adapters.
- When the affected module or behavior is documented, consult `.agents/skills/backend-architecture/`.
- Select the smallest test level that proves the changed behavior; follow `.agents/rules/testing-workflow.md` for integration boundaries.
