# FOR-157 Test Plan

Strict TDD: failing tests first. Mock the shopping client — no real backend.

## Scope

The over-budget chip in `ShoppingWidget` and the shopping read-model type. FOR-152 backend is done
and out of scope.

## API client (`shopping.ts`)

- The shopping read model types `weeklyThresholdEur` and `overThreshold` (extend if missing).

## ShoppingWidget

- `overThreshold: false` → OK/neutral chip; weekly + monthly cost still rendered.
- `overThreshold: true` → warning chip.
- Threshold label renders `weeklyThresholdEur` from the payload (not a hardcoded 120).
- Read model without the threshold fields → no chip, cost display intact (fallback).
- Loading → `LoadingState`; fetch error → `ErrorState` (FOR-60).

## Accessibility

- Chip status has a text/aria label (not color alone); axe coverage extended.

## Fixtures

- Mocked shopping payloads: under threshold, over threshold, and without threshold fields.
