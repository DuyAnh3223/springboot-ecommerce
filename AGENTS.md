# ABTechZone Repository Instructions

- `client/` is the Next.js frontend.
- `server/` is the Spring Boot backend.
- `.agents/` contains rules and skills shared by Codex and Antigravity IDE,
  plus specifications, plans, walkthroughs, templates, and architecture
  decisions.

## Authority

For product behavior, use this order:

Current explicit user requirement
> accepted specification and acceptance criteria
> current implementation plan
> existing implementation

An accepted ADR constrains the architecture it covers. Do not silently reverse
one; surface the conflict and update or supersede the ADR when the requested
change intentionally replaces it.

Existing code is evidence of current behavior, not proof of intended behavior.
Do not invent requirements. Surface material ambiguity before making a product
or architecture decision.

For engineering instructions, the nearest scoped `AGENTS.md` overrides this
file. Repository rules override skills when their guidance differs.

## Task Routing

Use one primary workflow skill:

| Intent | Primary skill |
|---|---|
| Understand or investigate | `explore-repository` |
| Add or change behavior | `implement-feature` |
| Fix incorrect behavior | `fix-bug` |
| Review an existing change | `review-change` |

Load only the relevant domain skill or skills in addition to the primary
workflow:

- backend: `backend-architecture`
- frontend structure: `frontend-architecture`
- frontend UI: `frontend-ui`

Do not load multiple primary workflow skills unless the user explicitly asks
for separate workflows.

## Engineering Guardrails

- Read the nearest scoped `AGENTS.md` for every area the task touches:
  `client/AGENTS.md`, `server/AGENTS.md`, or `.agents/AGENTS.md`.
- For non-trivial behavior-changing work, follow
  `.agents/rules/workflow.md` through the selected workflow skill.
- For substantial product behavior, implement only accepted requirement and
  acceptance-criterion IDs. Update and accept the spec before implementing a
  conflicting explicit requirement.
- Establish and run a focused feedback baseline before feature code, then use
  test/implementation/verification/review checkpoints. Follow
  `.agents/rules/verification-harness.md`.
- Make the smallest coherent change. Follow
  `.agents/rules/scope-control.md`.
- Use executable evidence appropriate to the risk. Backend test-level guidance
  lives in `.agents/rules/testing-workflow.md`.
- Do not claim success without stating what was verified. Report required checks
  that could not run as `NOT VERIFIED`.
- Provide concrete manual UAT for user-visible or business-critical behavior.
- Automated PASS is implementation evidence, not final acceptance. Report
  `UAT PENDING` until a human explicitly signs off.
- Follow `.agents/rules/documentation-workflow.md` when a persistent plan or
  walkthrough is required.

Exclude generated/dependency outputs from source audits:

- `client/.next/`
- `client/node_modules/`
- `server/target/`


<!-- gitnexus:start -->
# GitNexus — Code Intelligence

This project is indexed by GitNexus as **springboot-ecommerce** (7521 symbols, 32377 relationships, 300 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

> Index stale? Run `node .gitnexus/run.cjs analyze` from the project root — it auto-selects an available runner. No `.gitnexus/run.cjs` yet? `npx gitnexus analyze` (npm 11 crash → `npm i -g gitnexus`; #1939).

## Always Do

- **MUST run impact analysis before editing any symbol.** Before modifying a function, class, or method, run `impact({target: "symbolName", direction: "upstream"})` and report the blast radius (direct callers, affected processes, risk level) to the user.
- **MUST run `detect_changes()` before committing** to verify your changes only affect expected symbols and execution flows. For regression review, compare against the default branch: `detect_changes({scope: "compare", base_ref: "main"})`.
- **MUST warn the user** if impact analysis returns HIGH or CRITICAL risk before proceeding with edits.
- When exploring unfamiliar code, use `query({search_query: "concept"})` to find execution flows instead of grepping. It returns process-grouped results ranked by relevance.
- When you need full context on a specific symbol — callers, callees, which execution flows it participates in — use `context({name: "symbolName"})`.
- For security review, `explain({target: "fileOrSymbol"})` lists taint findings (source→sink flows; needs `analyze --pdg`).

## Never Do

- NEVER edit a function, class, or method without first running `impact` on it.
- NEVER ignore HIGH or CRITICAL risk warnings from impact analysis.
- NEVER rename symbols with find-and-replace — use `rename` which understands the call graph.
- NEVER commit changes without running `detect_changes()` to check affected scope.

## Resources

| Resource | Use for |
|----------|---------|
| `gitnexus://repo/springboot-ecommerce/context` | Codebase overview, check index freshness |
| `gitnexus://repo/springboot-ecommerce/clusters` | All functional areas |
| `gitnexus://repo/springboot-ecommerce/processes` | All execution flows |
| `gitnexus://repo/springboot-ecommerce/process/{name}` | Step-by-step execution trace |

## CLI

| Task | Read this skill file |
|------|---------------------|
| Understand architecture / "How does X work?" | `.claude/skills/gitnexus/gitnexus-exploring/SKILL.md` |
| Blast radius / "What breaks if I change X?" | `.claude/skills/gitnexus/gitnexus-impact-analysis/SKILL.md` |
| Trace bugs / "Why is X failing?" | `.claude/skills/gitnexus/gitnexus-debugging/SKILL.md` |
| Rename / extract / split / refactor | `.claude/skills/gitnexus/gitnexus-refactoring/SKILL.md` |
| Tools, resources, schema reference | `.claude/skills/gitnexus/gitnexus-guide/SKILL.md` |
| Index, status, clean, wiki CLI commands | `.claude/skills/gitnexus/gitnexus-cli/SKILL.md` |

<!-- gitnexus:end -->
