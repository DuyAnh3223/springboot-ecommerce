---
name: explore-repository
description: Explore or explain unfamiliar ABTechZone code without implementing changes. Use to trace execution paths, locate governing contracts and tests, assess blast radius, or answer how a subsystem works. Do not use as a second workflow during feature implementation or bug fixing.
---

# Explore Repository

## Purpose

Understand a subsystem sufficiently before changing it.

## Workflow

### Scope

Determine:

- frontend
- backend
- cross-layer
- infrastructure
- agent system

Read the nearest scoped `AGENTS.md` for every area inspected.

### Governing Documents

Look for relevant:

- spec
- ADR
- implementation plan
- API contract

### Execution Path

Understand:

ENTRYPOINT
→ BUSINESS LOGIC
→ DOMAIN/PERSISTENCE
→ EXTERNAL BOUNDARY
→ OUTPUT

Use GitNexus when available for unfamiliar flows.

### Tests

Inspect existing tests to determine:

- protected behavior
- known edge cases
- missing boundaries

### Baseline

Run the smallest useful existing verification where practical.

### Impact

Before editing shared symbols, determine blast radius.

## Output

Summarize:

- current behavior
- governing contract
- execution path
- relevant tests
- likely change scope
- risks
- unknowns
