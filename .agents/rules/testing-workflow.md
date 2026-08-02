---
trigger: always_on
---

# Efficient Testing Workflow

## Objective

Keep feedback fast without losing confidence in boundaries that mocks cannot prove.
The default test is a unit test. An integration test is a deliberate exception,
not a copy of every service test for every module.

## 1. Select the Smallest Test That Can Prove the Risk

| Change or risk | Required test | Do not use |
|---|---|---|
| Pure business branching, validation, null handling, mapping, casting, calculation, and null-safe `Specification` composition | JUnit + Mockito unit test (or a focused pure unit test) | `@SpringBootTest` / Testcontainers |
| MVC binding, request validation, JSON shape, status/error mapping, Spring Security filter behavior | `@WebMvcTest` or a focused MockMvc slice | Database container unless persistence is part of the assertion |
| Repository query semantics, PostgreSQL JSONB/native SQL, indexes/constraints, ORM mapping | `@DataJpaTest` with PostgreSQL Testcontainers | H2 as a substitute for PostgreSQL-specific behavior |
| Cross-layer HTTP -> service -> PostgreSQL behavior, transaction rollback, external-infrastructure wiring | One focused `@SpringBootTest` integration test | Duplicating every service branch end-to-end |
| Multi-resource checkout, locking, stock/voucher/order consistency | Focused integration tests plus service unit tests | Assuming mocks prove transaction or database behavior |

Do not add an integration test merely because a module exists. Add one only when
the behavior crosses a real boundary that the lower-level test cannot verify.

### Mandatory Test Decision Gate

Before creating a test, answer these questions in order:

1. **Can a plain Java test prove the required behavior?** If yes, write a unit
   test and stop. This covers business decisions, validation, calculations,
   null handling, mapping, and building a `Specification`.
2. **Is the risk only an HTTP/Spring MVC contract?** If yes, write a
   `@WebMvcTest` slice and stop. This covers routing, JSON binding, bean
   validation, error responses, and security filter behavior.
3. **Does the assertion require PostgreSQL, JPA, a transaction, or a real
   external integration?** If yes, add the smallest focused integration test.
   Name the unmockable boundary in the test name or comment.

One behavior gets one primary test level. Add a second level only when it proves
a different risk; state that distinct risk in the test name, `@DisplayName`, or
a short comment. Do not duplicate the same happy path and validation matrix at
unit, MVC-slice, and integration levels.

## 2. Test Portfolio Limits

- Cover the decision matrix and failure branches in unit tests; these should form
  the large majority of tests.
- Keep a small smoke suite of integration tests per *risk area*, not per module.
  A module may need zero integration tests, while a single flow may cover several
  modules.
- Prefer one or two representative end-to-end paths for ordinary CRUD/API
  modules: a successful request and one important contract/security failure.
- Keep dedicated repository integration tests only for custom derived queries,
  JPQL/native SQL, JSONB, constraints, cascading, or fetch behavior that could
  differ from a mock.
- Delete or convert an integration test when it only repeats assertions already
  covered by a unit or MVC-slice test and exercises no real boundary.
- A full-stack happy path may coexist with unit tests only when it proves a
  distinct persistence, transaction, or infrastructure contract; it must not
  repeat every business-rule assertion.

For this backend, PostgreSQL catalog filtering/sorting, repository semantics,
the checkout transaction/rollback path, and one authenticated API smoke path
are justified integration coverage. `Specification` unit tests should cover
filter assembly and null-safe composition; PostgreSQL integration tests should
cover the resulting ORM/SQL query. Plain cart, address, voucher, product, and
category validation/branching should normally be unit-tested; their full-stack
tests should be kept only where they prove a distinct HTTP, security, or
persistence contract.

## 3. Debugging and Execution Order

1. First inspect the code path, framework/library contract, null behavior, and
   casts. State a specific root-cause hypothesis with the relevant code location
   before changing code.
2. At the start of a debugging session, use already-available CI/local failure
   output. If no baseline exists, run the affected module/test scope once and
   group failures by root cause. Do not automatically run the repository-wide
   suite just to diagnose one local change.
3. After a fix, run one focused representative test method. Then run the
   affected unit-test class or failure group.
4. Run the relevant integration smoke test only if the changed code crosses its
   boundary. Run the full affected module suite before handoff; run the
   repository-wide suite in CI or for cross-module/shared changes.

If two careful code-tracing passes do not yield a defensible hypothesis, run the
smallest focused test to obtain more evidence. Do not guess, but also do not
analyze indefinitely.

## 4. Testcontainers Discipline

- Never start a container to debug pure logic.
- Share one PostgreSQL container through a common test base/configuration rather
  than declaring one container per integration-test class.
- Reset the database deterministically between tests. Reusable containers can
  retain state.
- Local container reuse may be enabled to reduce startup time; do not rely on it
  in CI, where clean and reproducible environments take priority.
- For long-running background tests, write output to a known log and wait for
  the process exit code/status. Do not treat a polling timer expiry as a test
  result.

## 5. Completion Gate

A change is ready when its focused unit/slice tests pass, its applicable
integration smoke test passes, and the relevant module suite passes. A full
repository suite is a release/CI gate or is required when shared infrastructure
or multiple modules changed.
