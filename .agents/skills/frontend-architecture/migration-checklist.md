# Frontend Structural Migration Checklist

1. Inventory files, consumers and route entry points.
2. Classify files as shared domain, Admin UI, Customer UI or route-local composition.
3. Move shared domain, then audience UI, then route-local composition.
4. Update absolute and relative imports, nested components and barrels.
5. Search stale aliases, old directories and audience-boundary violations.
6. Run lint, typecheck and build from `client/`.
7. Smoke-test affected Admin/Customer routes.
8. Inspect the diff for unintended behavior changes.
9. Persist the walkthrough in `.agents/docs/walkthroughs/`.
