---
name: implement-feature
description: Implement new or changed ABTechZone behavior from requirements through tests, verification, review, and handoff. Use for feature work and intentional behavior changes. Do not use for review-only requests or defects whose primary intent is bug fixing.
---

# Implement Feature

## Purpose

Implement new or changed behavior through spec gates, a working verification
harness, bounded checkpoints, and evidence-based handoff.

Canonical lifecycle and harness rules are defined in:

- `.agents/rules/workflow.md`
- `.agents/rules/verification-harness.md`

Do not duplicate those rules here.

## Step 1 — Explore Without Implementing

Perform the exploration phase directly. Do not activate `explore-repository` as
a second primary workflow unless the user explicitly asks for a separate
investigation.

Establish:

- governing behavior
- relevant source
- existing tests
- blast radius
- the smallest command or runtime path that exercises current behavior

Run the smallest useful baseline where practical. Report pre-existing failures
separately. Do not write feature code during this step.

## Step 2 — Pass the Spec Gate

Find the governing specification.

For a substantial feature, map planned work to accepted requirement and
acceptance-criterion IDs.

If substantial new behavior has no accepted specification, create or update one
using:

`.agents/templates/spec-template.md`

Resolve material open questions and obtain human acceptance of the spec before
implementing the affected product behavior. Do not implement extra behavior
that is absent from the accepted spec.

For a trivial, explicit change, the user request may serve as the task-local
contract; state that decision instead of creating paperwork.

Identify applicable requirement and acceptance-criterion IDs when available.

## Step 3 — Plan Bounded Checkpoints

Determine:

- scope
- expected files/modules
- test strategy
- independently verifiable checkpoints
- implementation steps
- verification
- UAT

For substantial work that requires a persistent plan, use:

`.agents/templates/plan-template.md`

and follow:

`.agents/rules/documentation-workflow.md`

Each checkpoint must name the requirement/risk it proves and the focused signal
that will control it. Do not accumulate an unbounded feature-sized diff.

## Step 4 — Establish the Harness Baseline

Before feature code:

1. choose the fastest reliable signal for the first checkpoint
2. run it and record PASS, expected RED, pre-existing FAIL, or NOT AVAILABLE
3. repair the minimum relevant harness gap if no reliable signal exists
4. record exact commands in the plan or working notes

Do not continue as though an unavailable or broken harness were green.

## Step 5 — Bind Scope and Architecture

Before executing checkpoints, bind the plan to the applicable scope and domain
instructions. Implement only the smallest coherent change.

Follow:

`.agents/rules/scope-control.md`

Use applicable domain skills:

Backend:
`.agents/skills/backend-architecture/`

Frontend:
`.agents/skills/frontend-architecture/`

UI:
`.agents/skills/frontend-ui/`

## Step 6 — Execute the Test/Implementation Loop

For backend work, follow `.agents/rules/testing-workflow.md`. For frontend work,
follow `client/AGENTS.md` and the existing test conventions for the affected
feature.

For each checkpoint:

1. add or identify the smallest test/check capable of proving the requirement
2. observe RED where meaningful, or state why RED is not applicable
3. implement only that checkpoint
4. observe GREEN
5. run focused surrounding verification
6. review the behavior and diff before continuing

Never weaken expected behavior to make the harness pass.

## Step 7 — Final Verification

Run focused verification first.

Then broaden according to affected risk and repository testing rules.

Do not treat code inspection as verification. Re-run all applicable checkpoint
signals, then broaden according to the combined risk.

## Step 8 — Self Review and Harness Improvement

Before handoff, inspect:

- requirement compliance
- edge cases
- architecture
- security
- unintended changes

Ask whether the work exposed a meaningful harness gap. Add focused regression
protection when it prevents recurrence; otherwise record why no harness change
is justified.

Use GitNexus change detection where required by repository instructions.

## Step 9 — Handoff for Human Acceptance

Summarize the implementation, then use the completion-evidence categories from
`.agents/rules/workflow.md`. Do not create a second completion format.

Provide concrete UAT for user-visible or business-critical behavior and report
`UAT PENDING` until a human explicitly accepts the result. Automated PASS does
not authorize the agent to claim final product acceptance.

For a procedural checkout example that does not invent product requirements,
read [checkout-example.md](checkout-example.md).
