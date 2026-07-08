# FOR-38 AI Context

## Story

FOR-38 — Calculate weekly and monthly shopping budget
(https://dbhlab.atlassian.net/browse/FOR-38)

## Intent

Turn the weekly list into a cost the user understands. Success is deterministic
weekly + monthly (×4.33) budget calculation that reflects current quantities and
prices, ready for the FOR-39 UI and the dashboard.

## Relevant Documents

- `AGENTS.md`
- `docs/domain-model.md` (Shopping)
- `docs/ui-guidelines.md` ("no fake precision")
- `docs/adr/ADR-001-architecture.md`, `docs/adr/ADR-007-testing.md`
- `specs/FOR-35/`, `specs/FOR-37/`
- `specs/FOR-21/`, `specs/FOR-28/`, `specs/FOR-32/` (pure calc + thin service
  precedents)
- Jira: https://dbhlab.atlassian.net/browse/FOR-38

## Domain Notes

- Weekly = sum of item costs; monthly = weekly × 4.33 (named constant).
- Cost source must match FOR-37's decision (stored vs. derived from product price
  × quantity). Deriving keeps the budget live when prices/quantities change.
- Money is `BigDecimal`; sum raw, round once for display.

## Architectural Constraints

- Pure calculation in the domain (a `ShoppingBudgetCalculator` / `ShoppingBudget`
  value type), optionally wrapped by a thin application service (FOR-21/FOR-28
  pattern). No persistence, no controller logic here.

## Common Pitfalls

- Rounding per item then summing (accumulated error).
- `double` money instead of `BigDecimal`.
- Holding a stale copy instead of reading current prices/quantities.
- Inventing category breakdown (deferred/optional).

## Suggested Implementation Order

1. Define a `ShoppingBudget` value type (weekly, monthly).
2. Implement weekly sum (from the FOR-37 cost source) and monthly × 4.33.
3. Add a thin service if useful for the UI/dashboard.
4. Tests: known list total, empty list = 0, monthly factor, price/quantity
   change reflected.

## Validation

Run `./gradlew test` from `backend/`.
