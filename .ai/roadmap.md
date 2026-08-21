# FORMA AI Roadmap Context

## Current phase

Feature development on a running application. Backend, frontend, database, local environment and CI have all existed for months; work now means changing a product that people can use, not building scaffolding.

`docs/roadmap.md` is the record of what has shipped. `AGENTS.md` is the working agreement. This file holds only what an agent needs that neither of those covers.

## How work arrives

**A pull request is the unit of work.** It carries the intent, the scope and what was deliberately left out; the commit messages carry the reasoning.

Work up to PR #170 (2026-07-26) was tracked as Jira stories with `FOR-XXX` keys and spec folders under `specs/`. That stopped: keeping Jira in sync cost more than it returned. The 125 spec folders that exist are still useful reading for the areas they cover, but **new work does not get a Jira key and does not get a spec folder** unless you are asked for one.

Practical consequences:

- Do not look for a Jira story behind a recent change. There isn't one, and that is not a gap.
- Do not scaffold `specs/FOR-XXX/` for new work on your own initiative.
- Put the reasoning where it will be read: the PR description and the commit message. A decision explained only in a chat is a decision nobody will find in six months.

## Implementation principle

Implement one coherent change at a time, with tests, in a focused PR. Do not take on broad chunks of the product at once.

This has not changed since the first line of code and it is the rule most worth keeping.

## Before you build something

Check whether it already exists. This repository has repeatedly grown a second implementation of something it already had — invented numbers rendered beside real data because a TypeScript type omitted the fields the API was already returning, and an apology in the UI about an endpoint that had shipped weeks earlier.

Read the code before adding to it, and delete the message that says a thing is impossible once it becomes possible.
