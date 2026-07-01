# AI Development Guide

## Purpose

This guide explains how AI coding agents should work in FORMA.

## First rule

Read `AGENTS.md` before changing anything.

## Context hierarchy

Use this order:

1. Repository rules: `AGENTS.md`
2. Product context: `.ai/product.md`
3. Architecture context: `.ai/architecture.md`
4. Domain context: `.ai/domain.md`
5. Conventions: `.ai/conventions.md`
6. Roadmap: `.ai/roadmap.md`
7. Global docs: `docs/`
8. Story specs: `specs/FOR-XXX/`
9. Jira issue: `FOR-XXX`

## Implementation workflow

For each story:

1. Read the Jira story.
2. Read `specs/FOR-XXX/` if present.
3. Identify affected bounded context.
4. Identify required layers: domain, application, adapter, API, UI.
5. Make a small plan.
6. Implement the smallest complete vertical slice.
7. Add/update tests.
8. Run checks.
9. Update docs if needed.
10. Open a PR with clear notes.

## What agents must not do

- Do not implement unrelated backlog items.
- Do not silently change architecture.
- Do not add dependencies without justification.
- Do not hardcode personal data except in explicit fixtures.
- Do not weaken auth, validation or logging rules.
- Do not invent provider behavior.

## Handling uncertainty

If a requirement is unclear:

- Prefer the explicit Jira story/spec over assumptions.
- Add a note to the PR.
- Keep the implementation conservative.
- Do not build speculative future features.

## Test expectations

- Add tests for new business behavior.
- Add regression tests for bugs.
- Keep tests close to the layer being tested.
- Do not rely only on end-to-end tests.

## Documentation expectations

Update documentation when:

- A new architectural decision is made.
- A public API contract changes.
- A domain term is introduced.
- A workflow for agents changes.
