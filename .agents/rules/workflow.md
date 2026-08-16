# AI Engineering Workflow

## Purpose

This rule defines the canonical working agreement for non-trivial engineering
work performed with AI agents.

Other skills may describe how to perform a phase, but they must not redefine
this lifecycle.

## Source of Truth

For product behavior:

Current explicit user requirement
>
Accepted specification and acceptance criteria
>
Current implementation plan
>
Existing implementation

For engineering behavior:

Nearest scoped AGENTS.md
>
Repository AGENTS.md
>
Applicable rules
>
Applicable skill

Accepted ADRs constrain the architecture they cover. If the requested change
intentionally replaces an ADR, surface that fact and update or supersede the
decision instead of silently violating it.

Existing code is evidence of current behavior, not automatically proof of
intended behavior.

Never invent product requirements.

---

# Working Principles

| Principle | Agent rule | Required evidence |
|---|---|---|
| Explore first | Do not write implementation code before tracing the affected behavior and exercising the smallest useful baseline. | Execution path, governing artifacts, existing tests, blast radius, baseline result. |
| Spec-driven | Implement only behavior identified by an accepted spec or an explicit task-local requirement. | Requirement and acceptance-criterion IDs, or a stated trivial task-local contract. |
| Harness first | Establish a reliable feedback command before the first feature-code checkpoint. | Command, expected signal, actual baseline status. |
| Improve the harness | When a risk escapes, decide whether the harness needs durable regression protection. | Added/updated check, or a reason no harness change is justified. |
| Harness controls quality | Use executable evidence, not code plausibility, to decide whether a checkpoint works. | RED/GREEN evidence or an explicit alternate verification method. |
| Review checkpoints | Split work into bounded, independently verifiable checkpoints and review each before accumulating more change. | Checkpoint scope, result, and review notes. |
| Human owns final quality | Treat automated verification as implementation evidence, not final product acceptance. | Concrete UAT plus human sign-off state. |

Exact commands and test levels depend on the affected risk. The gates do not.

---

# Standard Lifecycle

For non-trivial behavior-changing work:

EXPLORE
→ SPEC
→ PLAN
→ HARNESS
→ [TEST → IMPLEMENT → VERIFY → REVIEW] × CHECKPOINT
→ UAT
→ HANDOFF

Not every task requires a persistent document for every phase.

The lifecycle describes required reasoning and evidence, not mandatory
bureaucracy.

---

## 1. Explore

Before modifying unfamiliar or non-trivial behavior:

- read the nearest `AGENTS.md`
- locate relevant source code
- inspect existing tests
- locate applicable specs, ADRs, and contracts
- understand the execution path
- determine likely blast radius
- run the application, affected path, or smallest useful existing check where
  practical
- establish a useful baseline and distinguish pre-existing failures

Do not begin by generating implementation code. Exploration is complete only
when the agent can explain the affected flow, the likely change boundary, and
how it will observe breakage.

For shared symbols, use available code-intelligence tooling before editing.

---

## 2. Spec

Determine what correctness means.

For substantial behavior changes identify:

- goal
- requirements
- acceptance criteria
- non-goals
- edge cases
- domain invariants
- security implications

If an accepted spec already exists, use it and cite the applicable requirement
and acceptance-criterion IDs in the plan and tests.

If a substantial new feature has no accepted spec, draft or update the spec and
resolve material product questions before implementation. A current explicit
user requirement outranks an older spec, but the spec must be updated to reflect
that requirement before code is written.

Do not create a new spec for trivial implementation details.

If behavior is materially ambiguous, do not silently invent a product or
architecture decision. A `Draft` spec with unresolved questions is not approval
to implement the affected behavior.

---

## 3. Plan

For non-trivial work determine:

- intended scope
- likely affected areas
- expected tests
- independently verifiable checkpoints
- implementation sequence
- verification
- UAT
- risks

Persist a plan only when required by
`.agents/rules/documentation-workflow.md`.

---

## 4. Harness

Before the first implementation checkpoint:

- identify the fastest reliable feedback for the affected behavior
- run it and record the baseline
- repair the minimum relevant harness gap when no reliable signal exists
- separate pre-existing failures from failures introduced by the task

Follow `.agents/rules/verification-harness.md`.

Do not use a repository-wide green suite as a substitute for a focused signal,
and do not treat a broken or unrun harness as evidence.

---

## 5. Test

Tests are executable constraints on AI-generated implementation.

For each meaningful checkpoint, establish expected behavior before
implementation when practical:

1. add or identify the smallest check capable of proving the risk
2. observe RED or document why a pre-implementation failure is not meaningful
3. implement the checkpoint
4. observe GREEN

For regressions:

1. reproduce the problem
2. create or identify a regression test
3. confirm the failing behavior
4. implement the fix
5. confirm the regression test passes

For backend work, follow `.agents/rules/testing-workflow.md` for test-level
selection. For frontend work, follow `client/AGENTS.md` and existing test
conventions.

Do not pursue coverage percentage for its own sake.

---

## 6. Implement

Implement the smallest coherent change that satisfies the requirement.

Follow `.agents/rules/scope-control.md`.

Do not:

- introduce unrelated refactors
- invent features
- weaken tests to obtain PASS
- silently change architectural contracts

---

## 7. Verify

Verification determines whether implementation evidence supports correctness.

Use the smallest relevant check first and broaden according to risk.

Applicable evidence may include:

- unit tests
- slice/component tests
- integration tests
- lint
- type checking
- build
- static analysis
- GitNexus change detection
- manual runtime verification

A failed required verification means the checkpoint is not complete. Do not
stack later checkpoints on top of an unverified one unless the plan explicitly
requires them to be inseparable.

---

## 8. Review

Review implementation against:

- specification
- acceptance criteria
- non-goals
- architectural boundaries
- security
- edge cases
- unintended scope expansion

Review the behavior before reviewing style.

Perform review at every checkpoint or at an explicitly defined bounded group of
checkpoints. Always run and observe the result before allowing changes to pile
up.

---

## 9. UAT and Human Sign-Off

Automated verification does not replace human acceptance.

For user-visible or business-critical behavior, provide concrete UAT steps.

Examples:

- checkout
- order cancellation
- voucher application
- admin workflows
- authentication
- inventory-sensitive operations

The agent may execute and report mechanical or browser-assisted scenarios, but
only a human may mark final product acceptance as `ACCEPTED`. Until then, report
`UAT PENDING` even when all automated checks pass.

---

## 10. Handoff Evidence

Never claim:

- fixed
- complete
- working
- passing
- production-ready

without verification evidence.

A completion report should distinguish:

### Verified

Checks actually executed successfully.

### Failed

Checks executed and failed.

### Not Verified

Expected checks that could not be executed.

### Manual UAT

REQUIRED / RECOMMENDED / NOT APPLICABLE.

### Acceptance Status

UAT PENDING / ACCEPTED BY HUMAN / REJECTED BY HUMAN.

### Remaining Risk

Known uncertainty within the changed scope.

Confidence and diff inspection are not evidence.

---

# Architecture Decisions

Durable architecture decisions belong in `.agents/decisions/`.

Create an ADR only when a change establishes a decision future agents must
continue respecting.

Examples:

- module ownership
- transaction architecture
- authentication model
- concurrency strategy
- persistence boundaries
- dependency direction

Do not create ADRs for ordinary implementation details.

An accepted ADR must not be silently reversed.
