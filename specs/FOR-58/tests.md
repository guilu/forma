# FOR-58 Test Plan

## Scope

Verify the settings screen renders grouped sections, distinguishes editable vs
read-only, and keeps unsupported options inert.

## Domain Tests

N/A.

## Application Tests

N/A.

## API Tests

N/A — no profile backend yet (mocked where relevant).

## UI Tests

- Settings render grouped sections (profile, units, connections, objectives,
  notifications, security, about).
- The profile summary shows name/email and personal fields.
- Editable and read-only values are visually distinguishable.
- Unsupported options (e.g. 2FA, export/import) are shown as inert/"próximamente",
  not active.
- Settings are reachable from the main navigation (`/ajustes`).

## Edge Cases

- Missing profile backend → static/mock profile renders cleanly.
- Mobile → sections form a scrollable list.

## Fixtures

- A mock profile + preferences object; flags for supported vs unsupported options.
