# Scope Control

## Default Rule

Make the smallest coherent change that satisfies the task.

## Do Not Add Unrequested Work

Avoid:

- unrelated refactoring
- broad naming cleanup
- formatting unrelated files
- dependency upgrades
- speculative abstraction
- feature expansion
- API changes outside the requested behavior

## Incidental Problems

If an unrelated issue is discovered:

1. do not silently fix it
2. record it as an observation
3. continue the requested task unless it blocks correctness

## Refactoring

Refactoring is justified when it is necessary to:

- implement safely
- make behavior testable
- preserve architectural boundaries
- eliminate direct duplication introduced by the requested change

Otherwise keep it separate.

## Blast Radius

Before changing shared components, determine:

- direct callers
- downstream behavior
- cross-module impact
- contract implications

Use GitNexus impact analysis when applicable.
