# Agent System Structure

This file is a map, not an instruction source. Detailed constraints belong to
the canonical owner listed below.

## Root

- `../AGENTS.md` — repository-wide instructions and workflow routing.
- `AGENTS.md` — maintenance rules for the agent system itself.

## Guidance

- `rules/workflow.md` — canonical non-trivial engineering lifecycle and
  completion evidence.
- `rules/verification-harness.md` — baseline, RED/GREEN, checkpoint, and harness
  improvement contract.
- `rules/scope-control.md` — change-scope constraints.
- `rules/testing-workflow.md` — backend test-level selection and execution.
- `rules/documentation-workflow.md` — persistent artifact policy.
- `skills/` — four primary workflow skills and relevant domain skills.

## Durable Artifacts

- `specs/` — intended product and domain behavior.
- `docs/plans/` — intended execution for a specific change.
- `docs/walkthroughs/` — actual implementation and verification records.
- `decisions/` — architecture decision records.
- `templates/` — formats for specs, plans, UAT procedures, and ADRs.

## Supported Agent Entry Points

- Codex uses `../AGENTS.md` for repository routing and `.agents/skills/` for
  progressive workflow loading.
- Antigravity IDE uses `rules/` as workspace rules and `skills/` as workspace
  skills directly.
- `templates/feature-request-prompt.md` is the explicit invocation prompt for
  either agent.

No platform-specific copy owns lifecycle details.

## Ownership Principle

Each detailed instruction has one canonical owner. Other files link to that
owner instead of copying it.
