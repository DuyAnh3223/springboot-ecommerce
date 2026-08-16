# Feature Request Prompt

Use this template to invoke Codex or Antigravity IDE. Replace placeholders; do
not paste requirements the product owner has not decided.

```text
Implement <feature name> for ABTechZone.

Governing behavior:
- Spec: <accepted spec path or "missing">
- Requirements: <R IDs>
- Acceptance criteria: <AC IDs>
- Non-goals: <explicit exclusions>

Follow AGENTS.md and the canonical implement-feature skill at
.agents/skills/implement-feature/SKILL.md.

Before writing feature code:
1. Explore the actual cross-layer flow, governing artifacts, existing tests,
   and blast radius. Run the smallest useful current behavior/test baseline and
   report the evidence.
2. Enforce the spec gate. If this is substantial behavior and the accepted spec
   is missing or materially ambiguous, draft/update the spec and stop product
   implementation until a human accepts it.
3. Establish the verification harness. Name the exact focused command or
   procedure, run it, and record PASS, expected RED, pre-existing FAIL, or NOT
   AVAILABLE. Repair the minimum relevant harness gap before feature code.
4. Plan bounded, independently verifiable checkpoints.

For each checkpoint, perform:
TEST/RED -> IMPLEMENT -> VERIFY/GREEN -> REVIEW.
Do not continue past a failed required checkpoint and do not weaken expected
behavior to obtain GREEN.

At handoff, report Verified, Failed, Not Verified, harness improvements,
remaining risk, concrete manual UAT, and Acceptance Status. Use UAT PENDING
until a human explicitly signs off.
```

## Checkout Example

```text
Implement the checkout behavior defined by <checkout spec path>, requirements
<R IDs>, and acceptance criteria <AC IDs>. Treat pricing, vouchers, stock,
transaction rollback, idempotency, API errors, and customer UI behavior as
undefined unless the accepted spec or an accepted ADR defines them.

Follow the repository implement-feature workflow. First show exploration and a
focused checkout baseline. Then show the spec gate, harness table, and proposed
checkpoints. Do not write checkout code while a material spec question remains.
Verify and review every checkpoint. End with ugly-path checkout UAT and mark
acceptance UAT PENDING for human sign-off.
```
