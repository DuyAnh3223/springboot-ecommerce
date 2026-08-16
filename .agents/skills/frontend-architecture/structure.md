# Frontend Structure Reference

## Canonical tree

```text
client/features/
├── auth/
├── products/
├── categories/
├── attributes/
├── skus/
├── users/
├── orders/
├── vouchers/
├── admin/
│   ├── catalog/
│   │   ├── products/
│   │   ├── categories/
│   │   ├── attributes/
│   │   └── vouchers/
│   └── customers/
└── customer/
    ├── catalog/
    ├── cart/
    └── account/

client/shared/
├── actions/                  # Infrastructure actions (e.g. S3 file upload)
├── hooks/                    # Generic hooks (e.g. useAsyncAction, useTagInput)
├── http/                     # Technical HTTP clients/adapters (e.g. api.ts)
├── images/                   # TypeScript-imported static image modules
├── services/                 # Infrastructure services (e.g. file.service.ts)
├── types/                    # Technical types (e.g. PageResponse<T>)
└── utils/                    # Generic utility functions (e.g. slugify, cn)

client/app/(customers)/_components/home/
```

## Placement

- Generic, technical infrastructure with no business domain logic belongs in `client/shared/` (`http/`, `types/`, `hooks/`, `services/`, `actions/`, `utils/`, `images/`). Never create `client/types/` or `client/hooks/`.
- Use `client/shared/images/` for assets imported by TypeScript; use `client/public/` for URL-only static assets.
- Shared e-commerce domain models, types, services, actions, schemas and utils belong in `client/features/<domain>/`.
- Admin catalog UI belongs in `client/features/admin/catalog/<domain>/`.
- Admin customer-management UI belongs in `client/features/admin/customers/`.
- Customer catalog read/use-case UI belongs in `client/features/customer/catalog/`.
- Customer cart UI and audience-specific orchestration belong in `client/features/customer/cart/`.
- Customer profile/address UI belongs in `client/features/customer/account/`.
- Home sections, config, types and home-specific orchestration belong in `client/app/(customers)/_components/home/`.

`client/app/` may use route groups such as `(catalog)` or `(customers)`. Those route-group names must not be copied into `client/features/`. Do not create `client/features/home/` for a route-specific composition.
