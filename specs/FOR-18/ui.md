# FOR-18 UI Spec

## Screens

- Measurements page (`frontend/src/pages/MeasurementsPage.tsx`) — currently a
  `PagePlaceholder`; this story replaces it with the measurement entry form.

## Components

- Measurement form: date+time field, weight field, body fat % field, BMI
  field, optional notes field, submit action.
- Reuse `frontend/src/components/Card.tsx` for the form container, consistent
  with other pages.
- Inline field-level error messages (per ADR-006).

## States

- Loading: submit button shows a pending state while the request is
  in-flight; fields disabled or submit blocked to prevent double-submit.
- Empty: N/A for a form (not a list) — no empty state needed here.
- Error: API/network error shown near the form, using `ApiError.message`
  (docs/api-conventions.md), not raw response detail.
- Success: clear confirmation after a successful save; form resets or shows
  the saved values.

## Interactions

- Submit triggers client-side validation first; only calls the API when
  valid.
- Successful save triggers a refresh of measurement list/dashboard state
  (exact mechanism — local refetch vs. shared state — left to
  implementation, per architecture in `frontend/src/app/`).

## Accessibility

- Every field has an associated visible label (not placeholder-only).
- Keyboard-only submission works (tab order, enter-to-submit where
  appropriate).
- Validation errors are associated with their field for screen readers.

## Responsive Behavior

- Mobile: this form is mobile priority #2 per docs/ui-guidelines.md ("Add
  measurement") — fields stack vertically, touch targets are large enough,
  no horizontal scrolling.
- Desktop: form may use a wider single-column or two-column layout inside
  the existing page/card container; no additional desktop-only features are
  in scope for this story.
