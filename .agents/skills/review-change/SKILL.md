---
name: review-change
description: Review an existing ABTechZone change for correctness, regressions, architecture, test coverage, and unintended scope without implementing fixes. Use for diffs, pull requests, or review-only requests. Do not modify code unless the user separately asks for fixes.
---

# Review Change

## Purpose

Evaluate whether an implementation satisfies its intended behavior without
unintended scope or architectural damage.

Use the authority and completion-evidence sections in
`.agents/rules/workflow.md`. The behavior-changing lifecycle does not apply
unless the user separately asks you to implement fixes.

## Review Order

### 1. Intended Behavior

Determine:

- governing spec
- acceptance criteria
- current task
- non-goals

### 2. Behavioral Correctness

Check:

- happy paths
- failure paths
- edge cases
- state transitions
- authorization
- validation
- transaction behavior
- concurrency where relevant

### 3. Architecture

Use applicable architecture skills.

Check:

- module boundaries
- dependency direction
- ownership
- persistence boundaries
- frontend audience/domain boundaries

### 4. Tests

Evaluate whether tests prove behavior.

Do not reward test quantity.

Look for:

- missing regression protection
- tests coupled to implementation
- duplicated coverage
- missing boundary evidence

### 5. Scope

Look for:

- unrelated refactoring
- accidental API changes
- speculative abstraction
- dependency changes
- unrelated formatting churn

### 6. Verification Evidence

Do not accept claims of correctness without evidence.

## Findings

Report findings ordered by severity:

BLOCKER
HIGH
MEDIUM
LOW

Each finding should identify:

- location
- problem
- consequence
- violated requirement/rule when applicable
- recommended correction

If no findings exist, state what was reviewed and what evidence supports that
conclusion.
