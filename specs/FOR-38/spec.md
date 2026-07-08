# FOR-38: Calculate weekly and monthly shopping budget

Jira: https://dbhlab.atlassian.net/browse/FOR-38
Epic: FOR-5 Shopping Assistant

## Summary

Compute the estimated weekly total and monthly estimate for a `ShoppingList`
(FOR-37) from its item costs, so the user sees the real cost of the plan. Monthly
= weekly × 4.33. Rule-based, deterministic — mirroring the FOR-21/FOR-28/FOR-32
calculation precedents.

## User/System Flow

This story has no direct user flow. It produces budget values consumed by
FOR-39 (shopping UI) and the dashboard.

## Functional Requirements

- Compute **total weekly cost** = sum of the list's item estimated costs
  (product price × quantity, or the stored item cost — consistent with FOR-37).
- Compute **estimated monthly cost** = weekly cost × 4.33.
- The budget updates when item quantities or product prices change (it reads
  current values, holds no stale copy).
- Compute in the domain/application layer (ADR-001), pure and testable — no
  controller logic; a thin service may expose it (FOR-21/FOR-28 precedent).
- Keep currency in a money-safe type (`BigDecimal`), rounded for display.

## Non-Functional Requirements

- Deterministic: same list/prices always produce the same budget.
- Uses editable estimated prices; no external price automation.

## Data Model Notes

Builds on FOR-37 (`ShoppingList`/`ShoppingListItem`) and FOR-35 (`ShoppingProduct`
prices). Introduces a budget value type (e.g. `ShoppingBudget` with weekly and
monthly totals). No persisted entity — computed on demand. "Cost by category" is
noted as a later, optional extension (only if simple), not required here.

## Edge Cases

- Empty list → weekly and monthly totals are zero (valid).
- Item referencing a product with no/zero price — decide (treat as 0 vs.
  reject); recommend treating missing price as 0 and documenting.
- Rounding: sum raw, round once for display (FOR-32 precedent) — no accumulated
  error.

## Open Questions

- **Cost source** (must match FOR-37): stored `estimatedCostEur` on the item vs.
  derived from product price × quantity at calc time. Recommend deriving from
  current product price so the budget updates when prices change; document.
- **Monthly factor**: 4.33 (weeks/month) per Jira — keep as a named constant.
- Whether category breakdown is included — recommend deferring (optional "if
  simple"); do not invent categories.
