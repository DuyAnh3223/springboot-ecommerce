<!-- BEGIN:nextjs-agent-rules -->
# Next.js project rules

This version has breaking changes — APIs, conventions, and file structure may differ from training data. Read the relevant guide in `node_modules/next/dist/docs/` before writing code and heed deprecation notices.
<!-- END:nextjs-agent-rules -->

## Scoped Invariants

- `app/` owns route structure and route-local composition.
- `features/<domain>/` owns reusable e-commerce domain capabilities.
- `features/admin/` and `features/customer/` own audience-specific UI and must
  not import each other's UI.
- `shared/` contains generic technical infrastructure only. It must not contain
  e-commerce domain logic or import from `features/` or `app/`.
- Do not recreate `client/hooks/` or `client/types/`; their canonical locations
  are `client/shared/hooks/` and `client/shared/types/`.
- Use `@/features/...` for cross-domain imports and `@/shared/...` for generic
  technical utilities.
- Keep Server Components as the default. Services are the backend API boundary;
  Server Actions are for mutations and server-only operations.
- New UI copy and displayed data must be Vietnamese.

For structural placement and migration details, use the
`frontend-architecture` skill. For component, form, hook, and client-state work,
use the `frontend-ui` skill.

## Verification

From `client/`, run the relevant checks, normally:

```bash
npm run lint
npx tsc --noEmit
npm run build
```
