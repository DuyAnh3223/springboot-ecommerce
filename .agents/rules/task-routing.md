---
trigger: always_on
---

# Task Routing

1. Xác định file thuộc `client/`, `server/`, `.agents/` hay root.
2. Đọc `AGENTS.md` gần file đang sửa nhất trước; scope-specific rules override broader rules.
3. Chỉ đọc rule/skill liên quan task; không load toàn bộ `.agents/`.
4. Không audit `client/.next/`, `client/node_modules/`, `server/target/` hoặc generated/dependency output.
5. Task cross-layer phải đọc cả client và server scope rules.
