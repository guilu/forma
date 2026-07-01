# FOR-010 Manual body measurements

## Epic

Body Composition

## Goal

Allow the user to manually enter body measurements.

## Business value

Manual entry provides value before Withings integration and gives the Body module a stable domain model.

## Technical notes

Fields:

- measuredAt
- source = MANUAL
- weightKg
- bodyFatPercentage
- bmi
- optional notes

Derived:

- fatMassKg
- leanMassKg

## Acceptance criteria

- [ ] User can create a measurement through the UI.
- [ ] Backend persists the measurement.
- [ ] API returns measurements ordered by date.
- [ ] Fat mass and lean mass are calculated consistently.

## Definition of Done

- [ ] Backend tests cover derived metric calculation.
- [ ] Frontend handles validation errors.
- [ ] PR references FOR-010.
