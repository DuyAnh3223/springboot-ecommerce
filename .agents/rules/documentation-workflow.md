---
trigger: always_on
---

# Documentation Workflow Rules

Whenever planning or completing code changes, you MUST generate and persist the implementation plan and walkthrough as Markdown files directly in the repository:

1. **Implementation Plan Persistence:**
   - Before editing or generating code for non-trivial tasks, write the implementation plan into a file inside `.agents/docs/plans/`.
   - File naming format: `YYYY-MM-DD-<task-short-name>-plan.md` (e.g., `.agents/docs/plans/2026-08-01-refactor-catalog-repository-plan.md`).

2. **Walkthrough & Summary Persistence:**
   - After code changes are completed, write the walkthrough/summary into a file inside `.agents/docs/walkthroughs/`.
   - File naming format: `YYYY-MM-DD-<task-short-name>-walkthrough.md`.

3. **Execution Rule:**
   - Do not rely solely on transient chat output for plans or walkthroughs.
   - Always create the directory `.agents/docs/` if it does not exist and save the files using file system tools.
