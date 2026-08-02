---
trigger: always_on
---

# Frontend Architecture Boundaries

Áp dụng cho `client/**`. Canonical tree và placement matrix nằm trong `.agents/skills/frontend-architecture/structure.md`; không lặp lại ở rule này.

- Dependency direction: app routes → audience UI → shared domain capabilities (`@/features/...`) → generic technical shared (`@/shared/...`).
- `client/shared/` chỉ chứa technical infrastructure không phụ thuộc e-commerce domain: `http/`, `hooks/`, `types/`, `services/`, `actions/`, `utils/`, `images/`. Không tạo `client/hooks/` hay `client/types/`.
- `client/features/<domain>/` chứa business domain capabilities (models, types, services, schemas, actions).
- Shared domain không import Admin/Customer UI.
- Admin UI và Customer UI không import trực tiếp UI của nhau.
- Services là API boundary; Server Actions dành cho mutation/server-only operations; GET nên gọi service trực tiếp từ Server Component khi phù hợp.
- Structural moves phải giữ route URLs, exports, component names, API contracts và behavior.
- Dùng `@/features/...` cho cross-domain imports, `@/shared/...` cho technical utilities dùng chung; khi move file phải audit cả absolute, relative, barrel và dynamic imports.
- Không tạo abstraction, barrel hoặc compatibility alias mới nếu task không yêu cầu.
