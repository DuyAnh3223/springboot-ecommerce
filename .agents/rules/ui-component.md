---
trigger: always_on
---

# Frontend UI Rules

Áp dụng khi sửa `client` UI. Chi tiết decomposition, state placement và checklist nằm trong `.agents/skills/frontend-ui/`.

- Ưu tiên component nhỏ, rõ trách nhiệm; tránh refactor ngoài scope.
- Server Component là mặc định; chỉ dùng `"use client"` cho interaction, browser API hoặc client state.
- Dùng React Hook Form cho validated forms; dùng `useAsyncAction` cho async action handlers.
- Zustand chỉ là shared client state trong phạm vi use-case, không phải API cache; không đưa field do React Hook Form quản lý vào store.
- Logic dùng chung đặt ở `client/shared/hooks/` hoặc `client/shared/utils/`; không duy trì duplicate `client/hooks/`.
- Tất cả UI copy và dữ liệu hiển thị mới phải bằng tiếng Việt.
- Tuân thủ boundary Admin/Customer và canonical paths trong `client/AGENTS.md`.
