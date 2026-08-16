# `.agents` Instructions

This directory defines reusable guidance and durable engineering artifacts for
AI agents. Repository-wide instructions live in `../AGENTS.md`. Do not place
product implementation code here.

## Canonical Ownership

Each concern has one owner:

- `rules/` — reusable engineering constraints referenced by skills or scoped
  `AGENTS.md` files; they are not assumed to auto-load.
- `skills/` — task-specific procedures. Every `SKILL.md` must have valid
  `name` and `description` front matter.
- `specs/` — intended product and domain behavior.
- `docs/plans/` — intended execution of a specific non-trivial change.
- `docs/walkthroughs/` — what was actually changed and verified.
- `decisions/` — durable architecture decisions.
- `templates/` — reusable artifact and shared invocation formats.

Do not duplicate detailed guidance across these owners. Reference the canonical
owner instead.

## Loading Strategy

Codex automatically discovers `AGENTS.md` files by scope and repository skills
under `.agents/skills/`. Plain files under `rules/` are loaded only when an
active instruction or skill references them.

For a normal task:

1. follow root and nearest scoped `AGENTS.md` instructions
2. select one primary workflow skill using the root router
3. load only the relevant domain skill or skills
4. read only the rules and product artifacts referenced by that workflow

Do not activate multiple primary workflow skills by default.

## Agent-System Changes

- Keep root and scoped `AGENTS.md` files concise.
- Keep skill descriptions specific enough for reliable implicit routing.
- Use relative links inside skills and references.
- Update references whenever a file is renamed or removed.
- Do not add a second artifact type for a concern already represented here.
- Follow `rules/documentation-workflow.md` for non-trivial changes to this
  agent system.

## Supported Agents

Codex and Antigravity IDE share the canonical content directly:

- Codex reads scoped `AGENTS.md` files and discovers `.agents/skills/`.
- Antigravity discovers workspace rules in `.agents/rules/` and workspace
  skills in `.agents/skills/`.

Do not create platform copies of rules or skills. Keep shared behavior in the
canonical owners above and use `templates/feature-request-prompt.md` when an
explicit invocation prompt is useful.

## Conflict Handling

If instructions appear inconsistent:

1. follow platform and user instructions
2. follow the nearest scoped `AGENTS.md`
3. prefer the canonical owner over duplicated wording
4. surface unresolved material conflicts instead of guessing
