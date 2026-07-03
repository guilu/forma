# FOR-88: Create API skeleton and error response baseline

Jira: https://dbhlab.atlassian.net/browse/FOR-88
Story points: 3
Epic: FOR-79 Project Bootstrap

## Goal

Create the API skeleton and baseline error response model for FORMA.

## Business Value

Gives future API stories a consistent contract for routes, validation and errors from the first implementation sprint.

## Scope

Create API structure and error behavior only.

The API skeleton should include:

- Versioned API base path decision.
- Basic controller package/structure.
- Standard error response shape.
- Validation error response placeholder.
- Unauthorized/forbidden response placeholder where practical.
- Simple smoke endpoint or health-adjacent endpoint if useful.

## Architecture Notes

- Align with ADR-005.
- Avoid creating product endpoints before their stories.
- Do not expose stack traces.
- Keep response shapes documented.
- Controllers must remain thin.

## Acceptance Criteria

- API base convention is present or documented.
- Standard error shape exists.
- Validation error strategy is prepared.
- API smoke behavior can be tested.
- No product feature endpoint is implemented accidentally.

## Out of Scope

- Product endpoints.
- Authentication flow implementation unless only represented as safe placeholders.
- Full OpenAPI documentation unless trivial and aligned with the stack.

## Definition of Done

- API skeleton committed through PR.
- Basic API test added where practical.
- Error model documented.
- Local verification completed.
