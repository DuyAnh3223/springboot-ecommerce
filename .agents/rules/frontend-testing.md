---
trigger: always_on
---

# Frontend Verification

Áp dụng cho thay đổi trong `client/**`.

Với structural move, import migration hoặc cross-feature change, chạy từ `client/`:

```bash
npm run lint
npx tsc --noEmit
npm run build
```

Audit source loại trừ `.next/` và `node_modules/` để tìm stale aliases, relative imports, action barrels và audience-boundary violations. Build pass không thay thế smoke-test các route/component bị ảnh hưởng.

Structural changes cần kiểm tra các entry point liên quan; nhóm chính của project là Admin products/categories/attributes/customers và Customer home/catalog/profile/address.
