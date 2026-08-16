---
name: fix-bug
description: Diagnose and fix incorrect ABTechZone behavior using reproduction, root-cause analysis, regression protection, and focused verification. Use when the user reports a defect, failure, or regression. Do not use for feature work or review-only requests.
---

# Fix Bug

## Purpose

Repair a defect by proving the failure, correcting its root cause, and adding
durable regression protection.

Follow the canonical lifecycle in:

`.agents/rules/workflow.md`

Also follow:

- `.agents/rules/scope-control.md`
- `.agents/rules/testing-workflow.md` for backend changes

## 1. Observe

Capture concrete evidence:

- failing test
- error
- incorrect API response
- incorrect UI state
- log
- reproducible request

## 2. Reproduce

Establish the smallest reliable reproduction.

Do not begin by changing code.

## 3. Determine Expected Behavior

Resolve expected behavior from:

- accepted spec
- API contract
- domain invariant
- acceptance criteria
- known existing requirement

Do not invent behavior merely to make the bug disappear.

## 4. Root Cause

Trace the execution path.

State a defensible root-cause hypothesis before editing.

Use code intelligence when shared symbols or cross-module behavior are involved.

## 5. Regression Test

Create or identify the smallest test reproducing the problem.

Preferred sequence:

FAIL
→ FIX
→ PASS

## 6. Fix

Correct the root cause with the smallest coherent change.

## 7. Verify

Run:

1. regression test
2. relevant surrounding tests
3. boundary/integration verification where required

## 8. Improve the Harness

Ask:

Why was this defect not detected earlier?

Possible outcomes:

- missing negative test
- missing boundary test
- missing invariant
- missing UAT scenario
- insufficient static check

Only add additional harness coverage when it prevents a meaningful recurrence.

## Handoff

Report:

### Symptom
### Root Cause
### Fix
### Regression Protection
### Verification
### Remaining Risk
