<!-- BEGIN:nextjs-agent-rules -->
# Next.js project rules

This version has breaking changes — APIs, conventions, and file structure may differ from training data. Read the relevant guide in `node_modules/next/dist/docs/` before writing code and heed deprecation notices.
<!-- END:nextjs-agent-rules -->

## Architecture & Placement Rules

- `features/<domain>/` contains e-commerce domain capabilities (models, types, services, schemas, actions).
- `features/admin/` contains Admin UI only; `features/customer/` contains Customer UI only.
- `client/shared/` contains generic technical infrastructure and utilities only (no e-commerce domain logic, no features/ imports):
  - `client/shared/types/`: Global technical types (e.g. `PageResponse<T>`). Never create `client/types/`.
  - `client/shared/hooks/`: Generic technical hooks (e.g. `useAsyncAction`, `useTagInput`). Never create `client/hooks/`.
  - `client/shared/http/`: Technical HTTP clients/adapters (e.g. `api.ts`).
  - `client/shared/services/` & `client/shared/actions/`: Infrastructure services/actions (e.g. S3 file upload `file.service.ts`).
  - `client/shared/utils/`: Generic non-domain helpers (e.g. `slugify`, `cn`, image URL helpers).
  - `client/shared/images/`: TypeScript-imported static image modules; use `public/` for URL-only assets.
- `app/` owns route structure and route groups. Home-only composition belongs beside its route under `app/(customers)/_components/home/`.
- Admin UI and Customer UI must not import each other's UI; both may import shared domain capabilities and `client/shared/`.
- Use `@/features/...` for cross-domain imports, `@/shared/...` for generic shared utilities.
- Keep Server Components as the default; services are the API boundary; use Server Actions for mutations or server-only operations.

## Shared client utilities

- Use `client/shared/` as the single canonical location for generic shared hooks, types, services and utils.
- Do not recreate or maintain duplicate directories under `client/hooks/` or `client/types/`.

## Verification

From `client/`, run the relevant checks, normally:

```bash
npm run lint
npx tsc --noEmit
npm run build
```

For architecture details, load `.agents/skills/frontend-architecture/`. For detailed UI component guidance, load `.agents/skills/frontend-ui/`.
