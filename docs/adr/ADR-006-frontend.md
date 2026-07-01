# ADR-006: Frontend Architecture

## Status

Accepted

## Context

FORMA's frontend must present several domains coherently: body composition, training, nutrition, shopping, insights and integrations.

## Decision

The frontend will be organized around reusable UI components, feature pages and API clients/read models.

The frontend renders state and collects commands. It does not own domain rules.

## Consequences

- UI components remain reusable and predictable.
- Feature pages consume read models rather than reconstructing backend logic.
- Loading, empty and error states are standardized.
- Accessibility and responsive behavior are first-class MVP concerns.

## Rules

- Do not duplicate backend calculations in the UI.
- Keep navigation definitions centralized.
- Use design tokens from the design system.
- Every major screen must handle loading, empty and error states.
- Forms must display validation errors close to fields.
- Mobile usability is required for MVP screens.
