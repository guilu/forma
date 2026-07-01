# Definition of Done

A story is done when all applicable items below are complete.

## Product

- Acceptance criteria are satisfied.
- User-facing behavior matches the story goal.
- No known critical edge case is ignored.
- Product copy is clear and not misleading.

## Architecture

- Hexagonal boundaries are respected.
- Business rules live in the backend/domain/application layers.
- External provider details stay in adapters.
- No unnecessary abstractions were added.

## Code

- Code is readable and maintainable.
- Naming reflects the domain glossary.
- Dead code and debug artifacts are removed.
- No secrets or personal credentials are committed.

## Tests

- Relevant tests are added or updated.
- Existing tests pass locally or in CI.
- Regression tests are added for bug fixes.
- Domain rules are tested at the lowest practical level.

## Security and privacy

- Authorization rules are respected.
- Sensitive data is not logged.
- User-facing errors do not expose internals.
- New endpoints validate input.

## Documentation

- Relevant docs are updated.
- ADRs are updated or added when architectural decisions change.
- Story specs are updated if implementation clarifies behavior.

## Review

- Pull request is small enough to review.
- PR description explains what changed and how it was tested.
- Known limitations are explicit.
