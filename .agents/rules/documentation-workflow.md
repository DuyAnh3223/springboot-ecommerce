---
trigger: always_on
---

# Documentation Workflow

- Non-trivial implementation hoặc architecture changes bắt buộc có plan trước và walkthrough sau.
- Trivial typo, one-line hoặc mechanical local edit không bắt buộc plan/walkthrough.
- Lưu project-local plan tại `.agents/docs/plans/YYYY-MM-DD-*-plan.md`.
- Lưu walkthrough tại `.agents/docs/walkthroughs/YYYY-MM-DD-*-walkthrough.md`.
- Đây là deliverables canonical; `~/.commandcode/plans/` chỉ là Plan Mode approval artifact.
- Rule/skill-only changes phải có docs nếu chúng thay đổi architecture hoặc workflow.
