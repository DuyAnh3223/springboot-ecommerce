# Documentation Workflow

## Artifact Ownership

- A specification defines intended product or domain behavior.
- A plan defines how one non-trivial change is intended to be executed.
- A walkthrough records what was actually changed and verified.
- An ADR records a durable architecture decision and its rationale.
- A standalone UAT document defines detailed manual acceptance procedures when
  the scenarios are too large for the plan.

Do not create a parallel task artifact. Implementation plans live in
`.agents/docs/plans/`.

## When Documentation Is Required

- Create a plan before non-trivial implementation, architecture, or agent-system
  changes.
- Require an accepted spec for substantial new product behavior. A draft with
  unresolved material questions is not implementation authorization.
- Create a walkthrough after completing a significant change that required a
  plan.
- Trivial typos, one-line fixes, and mechanical local edits do not require
  persistent artifacts.

Use:

- `.agents/templates/plan-template.md`
- `.agents/templates/spec-template.md` when product behavior needs a durable spec
- `.agents/templates/uat-template.md` only for standalone UAT procedures

Plans belong at `.agents/docs/plans/YYYY-MM-DD-*-plan.md`. Walkthroughs belong
at `.agents/docs/walkthroughs/YYYY-MM-DD-*-walkthrough.md`.

Keep intended work in the plan and actual results in the walkthrough; do not
duplicate completion evidence in both.
