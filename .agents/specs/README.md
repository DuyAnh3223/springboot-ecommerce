# Specifications

Specifications define intended behavior.

They are not implementation notes.

An `Accepted` specification is the source of truth for substantial product,
domain, API, and cross-layer behavior. A `Draft` specification is a proposal,
not implementation authorization.

## Location

Feature specifications:

`features/<feature-name>.md`

Architecture specifications:

`architecture/<area>.md`

Product-level specifications:

`product/<area>.md`

## Naming

Prefer:

`SPEC-001-authentication.md`

or, where IDs are unnecessary:

`checkout.md`

## Rule

Do not create a spec for every trivial code edit.

Create or update a spec when work introduces or materially changes:

- product behavior
- API behavior
- domain rules
- authorization
- business workflows
- cross-module contracts

Use stable requirement (`R...`) and acceptance-criterion (`AC...`) IDs so plans,
tests, implementation checkpoints, and UAT can trace back to intended behavior.

Do not mark a spec `Accepted` while a material open question remains. If a
current explicit user requirement intentionally conflicts with an accepted
spec, update and re-accept the spec before implementation rather than letting
code and specification diverge.
