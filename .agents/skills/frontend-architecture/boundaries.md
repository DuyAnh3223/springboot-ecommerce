# Frontend Dependency Boundaries

```text
app routes -> audience UI -> shared domain capabilities -> generic technical shared (client/shared/)
route-local home -> customer catalog/category capabilities
shared domain -> no Admin/Customer UI imports
generic shared (client/shared) -> no features/ or app/ imports
Admin UI <-> Customer UI -> no direct UI imports
```

- `client/shared/` contains generic infrastructure and utilities only (no domain logic, no `features/` imports).
- `client/shared/http/` contains technical HTTP clients; feature/shared services are the API-facing use-case adapters.
- Services are the only layer that calls the backend API.
- Server Actions are for mutations or server-only operations such as cookies, redirects, revalidation and uploads.
- Server Components should call services directly for GET data when no Server Action behavior is needed.
- Client Components own interactive UI state, browser APIs and event handlers.
- Use `@/features/<domain>/...` for shared domain imports, `@/shared/...` for generic shared utilities.
- Route-local implementation details may use route-relative imports.
- After moving files, audit nested relative imports, action barrels and dynamic imports.
