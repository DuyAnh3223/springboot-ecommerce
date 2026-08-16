# Verification Harness

## Purpose

Define the executable feedback contract that controls AI-generated changes.
The harness is the smallest repeatable set of checks capable of detecting the
risks in the current task. It is not synonymous with running every repository
check.

## Pre-Implementation Gate

Before feature implementation begins, record:

| Field | Required content |
|---|---|
| Risk | Behavior or boundary that could break. |
| Signal | Test, lint, typecheck, build, runtime probe, static check, or UAT scenario that detects it. |
| Command / procedure | Exact repeatable invocation. |
| Baseline | PASS, expected RED, pre-existing FAIL, or NOT AVAILABLE. |
| Next action | Use it, repair it, replace it, or report why work cannot be verified. |

At least one focused signal must be usable before the first behavior-changing
checkpoint. If the relevant harness is missing or unreliable, create or repair
the minimum harness needed to control the task before writing feature code.

Do not silently fix unrelated baseline failures. Record them and choose a
scope-relevant signal when possible.

## Checkpoint Loop

For every independently verifiable checkpoint:

1. Map the checkpoint to a requirement, acceptance criterion, regression, or
   named engineering risk.
2. Add or identify the smallest check that can prove that risk.
3. Observe RED when a meaningful pre-implementation failure is possible. If it
   is not, state why and identify the alternate evidence.
4. Implement only the checkpoint scope.
5. Observe GREEN on the focused check.
6. Run the smallest surrounding verification capable of detecting collateral
   damage.
7. Review the diff and runtime result before starting the next checkpoint.

Do not weaken assertions, skip required checks, or redefine expected behavior
merely to obtain GREEN.

## Improve the Harness

Reassess the harness when:

- a defect escaped existing checks
- a checkpoint required repeated manual diagnosis
- a test was too broad, slow, flaky, or noisy to guide implementation
- a boundary or invariant had no executable protection
- human UAT found behavior that automated checks missed

Possible improvements include a regression test, a narrower test fixture, a
stable command, a static rule, a contract assertion, or a new UAT scenario.

Improve only the gap exposed by the current risk. Do not build test
infrastructure or chase coverage percentage without a concrete failure mode.

## Evidence Rules

- Code inspection can identify risks; it cannot prove runtime correctness.
- A command that was not run is `NOT VERIFIED`.
- A timed-out background process is not PASS.
- Pre-existing failures must be labeled separately from introduced failures.
- Automated PASS means implementation evidence exists; it does not mean human
  acceptance is complete.

For backend test-level selection, follow `testing-workflow.md`. For frontend
checks, follow `../../client/AGENTS.md` and the affected package scripts and test
conventions.
