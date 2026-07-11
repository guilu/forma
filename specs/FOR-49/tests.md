# FOR-49 Test Plan

## Scope

Verify the shell renders its regions, derives navigation from `NAV_ITEMS`, marks
the active section, and stays usable on mobile.

## Domain Tests

N/A — presentational shell, no domain logic.

## Application Tests

N/A — no application layer.

## API Tests

N/A — no endpoints.

## UI Tests

- `AppShell` renders header, primary navigation and a content outlet.
- Sidebar renders one entry per `NAV_ITEMS` item with correct labels.
- The active route's nav item is marked current (aria-current / active style).
- Mobile navigation shows the `primary` items plus a "Más" overflow entry.
- Content routes render inside the shell (e.g. Dashboard at `/`).
- Unknown route renders `NotFoundPage` within the shell.

## Edge Cases

- Narrow viewport: no horizontal scroll; bottom nav visible.
- Long page title truncates/wraps without breaking the header.

## Fixtures

- The real `NAV_ITEMS` list; a memory router seeded at a couple of routes
  (`/`, `/mediciones`, an unknown path).
