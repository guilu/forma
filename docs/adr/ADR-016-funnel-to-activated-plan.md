# ADR-016: From an anonymous funnel to an activated plan

## Status

Proposed.

This ADR does not replace [ADR-015](ADR-015-plan-request-and-ai-generation.md). It adds a second
entrance to the same building. ADR-015 describes how a **signed-in** user asks for a plan and how an
external agent writes it; everything it decided about `plan_request`, the status lifecycle, the
tolerance audit and the structural rejection stands unchanged here. What this ADR adds is the path
that starts before an account exists, and the single ingest both paths share.

## Context

### 1. The funnel already promises something the repository cannot do

`frontend/src/pages/generator/StepContact.tsx` tells every visitor, under the submit button:

> "Sin tarjeta · Te enviaremos el plan por email"

and again in the privacy checkbox label:

> "Solo para generarte el plan y enviártelo."

and the success screen, `frontend/src/pages/generator/PlanReady.tsx`:

> "Hemos recogido lo que necesitamos para construir tu plan y lo enviaremos a {email}."

There is no outbound email capability in this repository. No `spring-boot-starter-mail`, no SMTP
configuration, no mailer port, no adapter, no template. `PlanGeneratorController` says so itself and
states the rule this ADR exists to satisfy:

> "Still not generated, still not sent. The plan, the PDF and the mail do not exist yet, and
> `PlanLead` is what makes building them later possible for the people who asked first. **The success
> screen must not promise a delivery until they do.**"

That rule is currently broken — step 4's copy promises the delivery. So this is not a new feature
being invented. It is a published promise with nothing behind it, and either the promise comes down
or the delivery gets built. This ADR builds the delivery.

### 2. What the funnel collects, and what it deliberately does not

`V61__plan_lead.sql` stores one row per submission: `full_name`, `email`, `country`,
`heard_about_us`, `sex`, `age_years`, `weight_kg`, `height_cm`, `activity_level`, `objective`,
`days_per_week`, `meals_per_day`, `eating_style`, `plan_kcal`, and the consent columns
(`accepts_privacy_policy`, with a CHECK making `false` unstorable, `privacy_policy_version`,
`wants_marketing`).

`email` is deliberately **not** unique: "la misma persona puede pedir un plan dos veces... Deduplicar
aquí perdería la petición más reciente o la más antigua, y ninguna de las dos es basura."

No pathology, allergy or dietary-restriction column exists, on purpose: those are GDPR article 9
special-category data, and the funnel shows them only as locked teasers. **This ADR does not change
that.** Reference products in this space segment by clinical profile ("plan glucémico-metabólico",
"plan cardiometabólico DASH"); adopting that is a legal-regime change, not a feature, and it needs
its own decision.

### 3. A lead is not an account, and nothing links them

V61 is explicit: "Un lead no es una cuenta. No tiene contraseña, no puede entrar... Cuando alguien se
registre, será una fila de `users` con su propio correo; **enlazar las dos cosas es otra historia y
necesita su decisión**." This ADR is that decision.

`PlanLeadRepository` has exactly two operations, `save` and `deleteOlderThan` — no reader, no
`findByEmail`. `PlanLeadRetentionJob` deletes leads older than 12 months. Neither the Google success
handler nor password login nor registration consults `plan_lead`.

### 4. Email ownership is proven for Google and unproven for everything else

`users` has no `email_verified` column. Its real columns, across `V26__users.sql` and
`V62__google_login.sql`, are `id`, `email`, `password_hash` (nullable since V62), `created_at`,
`last_login_at`, `is_active`, `google_subject`. The only code that mentions `emailVerified` is
`GoogleIdentity` and `UserService`, where it is a claim read off Google's token and checked at login
(`UserService.java:96` refuses an unverified one) — never persisted.

This is load-bearing for everything below. Any design in which an account inherits data by matching
an email address is safe when Google vouched for that address and is an **account-takeover vector**
when it was merely typed into a registration form.

### 5. The twelve-week programme already contains the commercial seam

`V64__plan_request_programme.sql` decided that a programme is three four-week blocks, each its own
`plan_request` with its own capture, its own audit and its own activation. Whatever the eventual
business model — one-off per plan, per-part unlocks, or free first block then paid continuation —
the boundary it would charge at already exists as a row boundary. This ADR therefore decides **no**
monetization mechanics, and records why deferring costs nothing.

## Decision

### 1. The funnel's plan is parked, not owned, until someone claims it

A visitor has no `user_id`, and `plan_request.user_id` is `NOT NULL REFERENCES users(id) ON DELETE
CASCADE` (V63:45). Two ways to bend that were considered and are rejected in "Alternatives rejected"
below: making `user_id` nullable, and growing the generation lifecycle onto `plan_lead`.

Instead, a new table, **`plan_delivery`**, one row per generated-plan-waiting-for-an-owner:

| column | meaning |
|---|---|
| `id UUID PK` | the correlation id the agent pushes against |
| `plan_lead_id UUID NOT NULL REFERENCES plan_lead(id) ON DELETE CASCADE` | the funnel answers that produced it |
| `status VARCHAR(16)` | `PENDING` → `GENERATING` → `READY` → `CLAIMED`, or `FAILED` |
| `open_marker CHAR(1)` + `UNIQUE(plan_lead_id, open_marker)` | at most one open delivery per lead, the V63 nullable-sentinel pattern, written `IS NOT NULL` |
| `payload TEXT` | the agent's plan as sent, stored verbatim, never edited |
| `contract_version`, `catalog_version`, `agent_model`, `agent_generated_at` | provenance |
| `claimed_by_user_id UUID REFERENCES users(id)` | NULL until claimed |
| `claimed_at`, `failure_code`, `failure_detail`, `attempt_count`, timestamps | as `plan_request` |

The payload is stored **as received** and turned into rows only at claim time. A parked delivery is
not a plan: nothing in `nutrition_plan` exists for it, nothing is active, and a lead that is never
claimed leaves no orphan plan behind. `ON DELETE CASCADE` from `plan_lead` means the existing
twelve-month retention sweep already collects parked deliveries, with no second retention rule to
write or forget.

**`plan_request` is not touched by this ADR.** It keeps its `NOT NULL user_id`, its CHECKs and its
lifecycle exactly as ADR-015 wrote them.

### 2. One correlation id, two owners, one ingest

The agent pushes a generated plan against a correlation id. That id is either a `plan_request.id`
(the signed-in path, ADR-015) or a `plan_delivery.id` (the funnel path, this ADR). The ingest
resolves which, and the difference is only *when the rows get written*:

| correlation | owner at push time | what the ingest does |
|---|---|---|
| `plan_request.id` | known (`plan_request.user_id`) | validate, write `nutrition_plan` rows, request → `READY`, in one transaction (ADR-015 decision 2) |
| `plan_delivery.id` | none | validate, store the payload, delivery → `READY`. No `nutrition_plan` rows yet |

Everything else is shared and written once: the structural check
(`NutritionPlanService#problemsIn` — unknown `foodId`, a `servingId` belonging to another food),
which rejects the whole payload and writes nothing; the section-11 tolerance audit
(`PlanToleranceAuditor`), which reports drift and never corrects it; and the rule that a drifting
plan is stored as `DRAFT` with its report and never auto-activated (ADR-015 decision 9).

A payload that fails structurally is rejected identically on both paths. There is one validation
policy, not two.

### 3. The agent pushes; FORMA authenticates it with a shared secret

ADR-015 decision 8 deferred the inbound endpoint because there was no auth model for a caller that
is not a person. This ADR settles it: **`POST /api/v1/agent/plans`**, authenticated by a shared
secret in a request header, compared in constant time, bound to config
`forma.plan-agent.inbound-secret`.

Three properties are not optional:

- **An unset secret refuses everything.** The empty default must mean "no caller can be
  authenticated", never "no authentication required". This is the failure mode that turns a missing
  environment variable into an open write path into the database, and it is the single most
  dangerous line in this design.
- **The endpoint is outside the session-cookie and CSRF machinery** (ADR-012), because the caller has
  no session and no browser. It must be explicitly permitted in the security configuration rather
  than inheriting the authenticated-user rules by accident.
- **A push is idempotent by correlation id.** A correlation already `READY`, `CLAIMED` or `FAILED`
  is terminal: a second push for it is answered, not applied. Retries on the agent's side must not
  produce two plans.

Not mTLS: the operational burden of client certificates is out of proportion here. Not OAuth client
credentials: there is no identity provider for machines in this system, and introducing one to
authenticate a single caller is the speculative abstraction the repository's own rules forbid.

### 4. The draft email is ours and costs nothing; the full plan comes from the agent

Two artifacts, deliberately separate:

- **The draft**, sent immediately on funnel completion. FORMA computes it from the funnel answers —
  `EnergyRequirement` already does this, and `plan_lead.plan_kcal` already stores the result. It
  needs no AI call, so it cannot be made expensive by submission volume, and it makes step 4's
  existing promise true.
- **The full plan**, produced by the external agent and pushed back against the delivery's
  correlation id.

The draft's format is a decision this ADR leaves open (see Open points): an HTML email is buildable
now, a PDF needs a generator that does not exist.

### 5. Email leaves through a port; the provider is chosen later

A `PlanDraftMailer` port in `application/`, with a logging adapter that records what it would have
sent. This is the pattern the repository has already used twice — `PlanGenerationGateway` shipped in
ADR-015 slice 5 with no live call, and `ProviderOAuthGateway`/`ProviderMeasuresGateway` before it —
and it keeps the provider decision (SMTP via `spring-boot-starter-mail`, or a provider's HTTP API
with the `PlanAgentTransport` seam pattern) out of the critical path of building the circuit.

No provider type crosses the port (ADR-001/ADR-004).

### 6. The claim happens at Google login, and only there, until email verification exists

When a Google login completes, FORMA looks up `plan_delivery` rows in status `READY` whose lead's
email matches the authenticated address, and offers to activate the plan.

**Password registration and password login claim nothing.** Not as a simplification — because
`users` has no column proving the address belongs to whoever typed it (Context, point 4). Letting a
password account claim by email would let anyone register with a stranger's address and receive that
stranger's plan along with the sex, age, weight and height behind it.

This is a restriction with an expiry date, not a permanent shape: when email verification exists,
password accounts join the same path with no change to this design. What must not happen is claiming
by email before that verification exists.

The claim itself is a user action, never automatic: the plan is materialized into `nutrition_plan`
rows with the claiming user as owner, a `plan_request` row is written recording that this plan came
from delivery `X` and lead `Y`, and the delivery moves to `CLAIMED`. The existing one-active-plan
invariant (`nutrition_plan.active_marker`, V53) decides what activation means; a claimed plan lands
as `DRAFT` and the user activates it, exactly as ADR-015 decision 9 requires.

### 7. No monetization mechanics, and why that is safe

This ADR decides none, and adds no column for any. The seam a business model would cut at is the
four-week block (Context, point 5), and it is already a row boundary with its own activation. Free
first block with paid continuation, one-off per plan, and per-part unlocks all attach to structures
that exist. Continuous-access subscription is the one model that would need new tables, and nothing
here forecloses it.

Deciding later is therefore not debt. Deciding now, against a circuit that has never run end to end,
would be.

## Consequences

- The funnel's published promise becomes true instead of coming down.
- `plan_request` keeps its invariants: no nullable owner, no lifecycle grafted onto a marketing row.
- One structural-validation policy and one tolerance audit serve both entrances.
- A never-claimed delivery expires with its lead under the existing twelve-month sweep. No second
  retention rule.
- FORMA gains its first inbound write path from a non-human caller. That is a genuinely new class of
  surface in this system and is treated as such above.
- ADR-015 slice 6 is absorbed: the dispatcher, the timeout sweep and the ingest are built once, for
  both paths, rather than built for the signed-in path and retrofitted.

## Risks

- **Cost per anonymous submission.** The public endpoint is unauthenticated and `plan_lead.email` is
  intentionally non-unique, so nothing today stops a thousand submissions. Generating on funnel
  completion means each one is a paid model call. Mitigation: per-IP and per-email rate limiting in
  front of the dispatch, and a configured ceiling. The dispatch trigger is deliberately a policy in
  one place, so moving it to claim time later is a small change, not a redesign.
- **The unset-secret failure mode.** Covered in decision 3; restated here because it is the one
  defect in this design that would be silent.
- **Deliverability.** An email nobody receives is indistinguishable, from the visitor's side, from
  the promise we have now.
- **Article 9 pressure.** The nearest competitors segment by clinical profile. The moment FORMA
  collects a pathology to improve a plan, the legal regime changes.
- **A parked plan is a plan nobody has audited against a real person.** The tolerance audit runs at
  ingest, but the funnel collects less than the signed-in wizard does (no training weekdays, no diet
  pattern, no target macros), so a funnel plan is necessarily a poorer brief than a wizard plan.

## Alternatives rejected

- **Make `plan_request.user_id` nullable and add an email column.** One table instead of two, but it
  breaks three things at once: the `UNIQUE(user_id, open_marker)` sentinel stops constraining funnel
  rows, because NULLs do not collide, so "at most one open request" silently stops applying exactly
  where abuse would arrive; `ON DELETE CASCADE` from `users` no longer reaches those rows, leaving
  GDPR orphans; and `plan_request` stops meaning "a signed-in user asked for this", which is what
  ADR-015 decision 1 chose it to mean.
- **Grow the generation lifecycle onto `plan_lead`.** Duplicates ADR-015's status machine in a second
  table and contradicts V61's own argument for the table's separateness — it would put statuses,
  payloads and plan pointers next to marketing-consent columns.
- **Convert a lead into a `plan_request` at funnel time.** Rejected by the product owner before
  ADR-015 and re-rejected here for the same reason: it requires an owner that does not exist yet.
- **Claim by email at password registration.** Context, point 4. An account-takeover vector.
- **A synchronous call that returns the plan to the browser.** Rejected by the product owner in
  ADR-015 and still wrong: an unbounded model latency on a user-facing request, with nothing to show.
- **Deciding the business model now.** Decision 7.

## Rules

- No provider type crosses a port (ADR-001/ADR-004). The mailer and the agent are adapters.
- The payload is stored as received. Claimed totals are never overwritten with recomputed ones
  (ADR-015 decision 9, V53's reasoning).
- No article 9 data enters the funnel without its own ADR.
- An unconfigured inbound secret refuses; it never permits.
- A terminal correlation id is terminal. Retries create new rows, never resurrect old ones.

## Implementation plan

Each slice is PR-sized and independently verifiable.

1. **The mailer port and the draft email.** `PlanDraftMailer` in `application/`, a logging adapter,
   the draft's content built from the funnel answers, sent on funnel completion. Makes the existing
   promise true. *Verify*: a completed funnel produces exactly one draft, and a funnel that fails
   validation produces none.
2. **`plan_delivery`.** The migration, the record, the port, the JDBC adapter. *Verify*: the
   nullable-sentinel refuses a second open delivery for one lead; a deleted lead takes its deliveries
   with it.
3. **The dispatcher and the timeout sweep** (ADR-015 slice 6, both paths). Claim by conditional
   `UPDATE`, call the port, ingest, transition. *Verify*: a `GENERATING` row past its deadline becomes
   `FAILED('TIMEOUT')`; two concurrent claims produce one winner.
4. **The inbound endpoint.** Authentication, idempotency, the shared ingest, `validation_report`,
   structural rejection. *Verify*: an unset secret refuses; a wrong secret refuses; a replayed push
   is answered and not applied; a payload with one unknown `foodId` writes nothing and reports every
   problem at once.
5. **The claim at Google login.** Lookup by verified email, materialization into `nutrition_plan`
   rows with a `plan_request` recording the provenance, delivery → `CLAIMED`, and the screen that
   offers it. *Verify*: a password account claims nothing; a Google account with no delivery sees no
   offer; claiming twice materializes one plan.
6. **Rate limiting in front of the dispatch.** Per IP and per email, with a configured ceiling.

## Open points

- The draft's format. An HTML email is buildable now; a PDF needs a generator that does not exist.
- What the draft actually shows. The funnel computes a calorie target and nothing else; a draft that
  shows only a number may undersell, and one that shows meals requires generating them.
- The email provider.
- Email verification for password accounts, which is what lets them join decision 6.
- Whether a claimed plan activates immediately or lands in the existing onboarding, which today
  routes on `profile.firstRunCompleted` and not on plan existence
  (`frontend/src/app/OnboardingGate.tsx:77`).
- How a second or third block is requested once the first is claimed — `plan_request.block_number`
  can only ever be 1 today (V64).
