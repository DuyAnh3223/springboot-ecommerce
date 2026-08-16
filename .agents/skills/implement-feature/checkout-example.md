# Checkout Feature Workflow Example

Use this example when the user asks to add or materially change checkout. It
demonstrates process only; it does not define checkout product requirements.

## 1. Explore

- Read root, client, and server scoped `AGENTS.md` files for a cross-layer flow.
- Locate accepted checkout specs, ADRs, plans, API contracts, and the current
  checkout architecture reference.
- Trace UI → API → order transaction → inventory/voucher/payment boundaries →
  response.
- Inspect existing service, controller, persistence, integration, and UI tests.
- Determine blast radius for shared checkout symbols.
- Run the smallest current checkout test or runtime scenario and record the
  baseline.

Output the exploration evidence before proposing implementation.

## 2. Spec Gate

Map the requested work to accepted requirement and acceptance-criterion IDs.

If no accepted checkout spec defines the requested behavior:

1. draft or update the checkout spec
2. mark unresolved product decisions as open questions
3. stop product implementation for those decisions
4. request human review and acceptance of the spec

Do not infer payment, pricing, voucher, stock, idempotency, retry, or error
semantics from method names or current code alone.

## 3. Harness Gate

Name and run at least one focused signal for the first checkpoint. For example,
choose the applicable level rather than running all levels automatically:

- service unit test for pricing or validation decisions
- MVC slice for the checkout request/error contract
- PostgreSQL-backed integration test for locking, rollback, stock, voucher, or
  order consistency
- frontend component/action test for UI state and request handling
- manual runtime scenario for end-to-end UX

Record the command, expected signal, and baseline status.

## 4. Checkpoint Loop

Example checkpoint boundaries:

1. request/response contract
2. domain calculation or validation
3. transactional consistency boundary
4. customer UI orchestration
5. failure and recovery paths

Use only boundaries required by the accepted spec. For each checkpoint, perform
TEST → IMPLEMENT → VERIFY → REVIEW before starting the next one.

## 5. Handoff

Report verified checks, failures, not-verified checks, remaining risk, and
concrete checkout UAT including an ugly failure path. Mark acceptance as
`UAT PENDING` until a human signs off.
