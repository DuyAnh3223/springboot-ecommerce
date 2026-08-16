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

This project is indexed by GitNexus as **springboot-ecommerce** (6009 symbols, 11580 relationships, 294 execution flows). Use the GitNexus MCP tools to understand code, assess impact, and navigate safely.

> Index stale? Run `node .gitnexus/run.cjs analyze` from the project root — it auto-selects an available runner. No `.gitnexus/run.cjs` yet? `npx gitnexus analyze` (npm 11 crash → `npm i -g gitnexus`; #1939).

## When GitNexus Is Available

- Before modifying a shared function, class, or method, run `impact({target: "symbolName", direction: "upstream"})` and inspect the blast radius.
- Run `detect_changes()` before committing to verify changes only affect expected symbols and execution flows. For regression review, compare against the default branch: `detect_changes({scope: "compare", base_ref: "main"})`.
- Warn the user before editing when impact analysis returns HIGH or CRITICAL risk.
- When exploring unfamiliar code, use `query({search_query: "concept"})` to find execution flows instead of grepping. It returns process-grouped results ranked by relevance.
- When you need full context on a specific symbol — callers, callees, which execution flows it participates in — use `context({name: "symbolName"})`.
- For security review, `explain({target: "fileOrSymbol"})` lists taint findings (source→sink flows; needs `analyze --pdg`).

If GitNexus or a current index is unavailable, use callers, references, tests,
and contract inspection as the fallback and report the limitation. Do not block
an otherwise safe task solely because GitNexus is unavailable.

Never ignore HIGH or CRITICAL risk results. When GitNexus is available, use its
graph-aware `rename` instead of find-and-replace for shared symbols.

## Resources

| Resource | Use for |
|----------|---------|
| `gitnexus://repo/springboot-ecommerce/context` | Codebase overview, check index freshness |
| `gitnexus://repo/springboot-ecommerce/clusters` | All functional areas |
| `gitnexus://repo/springboot-ecommerce/processes` | All execution flows |
| `gitnexus://repo/springboot-ecommerce/process/{name}` | Step-by-step execution trace |

## CLI

| Task | Use this skill when available |
|------|---------------------|
| Understand architecture / "How does X work?" | `gitnexus-exploring` |
| Blast radius / "What breaks if I change X?" | `gitnexus-impact-analysis` |
| Trace bugs / "Why is X failing?" | `gitnexus-debugging` |
| Rename / extract / split / refactor | `gitnexus-refactoring` |
| Tools, resources, schema reference | `gitnexus-guide` |
| Index, status, clean, wiki CLI commands | `gitnexus-cli` |

<!-- gitnexus:end -->
