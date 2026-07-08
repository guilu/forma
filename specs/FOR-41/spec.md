# FOR-41: Create recommendation model

Jira: https://dbhlab.atlassian.net/browse/FOR-41
Epic: FOR-6 Insights Engine

## Summary

Create the `Recommendation` domain model: an explainable suggestion with a
category, severity, short message and a **reason** referencing observed data.
This is the shared output type of the FOR-42/43/44 rules and the FOR-45 API.
Domain-only.

## User/System Flow

This story has no direct user flow. It defines the type produced by the rules:

1. FOR-42/43/44 rules produce `Recommendation`s from the FOR-40 check-in.
2. FOR-45 returns the main + secondary recommendations to the dashboard.

## Functional Requirements

- Add `Recommendation` under
  `backend/src/main/java/dev/diegobarrioh/forma/domain/`, framework-free
  (ADR-001), per docs/domain-model.md "Recommendation".
- Fields: `createdAt`, `category`, `severity`, `message`, `reason`, optional
  related metric.
- `category` constrained to: `BODY`, `TRAINING`, `NUTRITION`, `RECOVERY`,
  `SHOPPING` (enum).
- `severity` constrained to: `INFO`, `WARNING`, `ACTION` (enum).
- A recommendation includes **both `message` and `reason`** (reason references
  observed data). Messages are short/clear; avoid fake precision.

## Non-Functional Requirements

- Framework-free, deterministic value type; no persistence.
- Copy is neutral and non-alarming (docs/ui-guidelines.md interaction style).

## Data Model Notes

Mirrors docs/domain-model.md's `Recommendation`. **Enum discrepancy**:
docs/domain-model.md lists categories `NUTRITION | RUNNING | STRENGTH | RECOVERY |
SHOPPING`, while the Jira story lists `BODY | TRAINING | NUTRITION | RECOVERY |
SHOPPING`. Use the **Jira** set (`BODY`/`TRAINING` instead of `RUNNING`/
`STRENGTH`) and document the mapping. The "optional related metric" is a light
reference (e.g. a metric name/value), not a full domain object.

## Edge Cases

- Blank `message` or `reason` — reject (both are required).
- `category`/`severity` outside the known set — impossible via enum.
- No related metric — the field is optional.

## Open Questions

- Shape of the "optional related metric": a simple string label vs. a small
  `(name, value)` pair. Recommend a light optional value (e.g. a string) for the
  MVP; document.
- Whether recommendations are ever persisted — recommend computed-on-demand only
  (FOR-45), no persistence, unless a later story needs history.
