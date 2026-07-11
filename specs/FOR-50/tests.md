# FOR-50 Test Plan

## Scope

Verify the base components render correct variants from tokens and stay reusable
and accessible.

## Domain Tests

N/A.

## Application Tests

N/A.

## API Tests

N/A.

## UI Tests

- Button renders each variant (primary/accent, secondary, ghost, destructive)
  and a disabled/loading state.
- Badge/Status renders severity (`INFO`/`WARNING`/`ACTION`), connection
  (`Conectado`/`No conectado`) and plazo variants; unknown value → neutral
  fallback.
- Input field renders label + error text association (feeds FOR-61).
- Card and chart-container render children with consistent framing.
- Components expose semantic elements (`<button>`, labelled inputs).

## Edge Cases

- Unknown status value → neutral badge, not unstyled.
- Disabled button is not activatable and is announced as disabled.

## Fixtures

- Sample labels/values per variant; a short usage/examples surface.
