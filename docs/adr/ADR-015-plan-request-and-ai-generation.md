# ADR-015: The plan request, and the plan an external agent writes

## Status

Proposed (target: Accepted once the first migration — `plan_request` — lands and is exercised
against both H2 and PostgreSQL).

"Proposed" is load-bearing in this repository, not a formality. [ADR-011](ADR-011-data-model-v2.md)
is still Proposed, and `V53__nutrition_plan.sql` used exactly that fact to record a deliberate
deviation from it ("ADR-011 is Proposed, not Accepted, which is what makes this a deviation to
record rather than one to refuse") — and, in the same migration, to *fix* a hole in an ADR-011 CHECK
constraint that would have accepted an ACTIVE plan with no marker. A Proposed ADR here is a shape
that the first implementation is expected to argue with. This one should be read the same way.

Builds on [ADR-003](ADR-003-persistence.md) (Flyway, hand-written JDBC, no JPA, H2 in
`MODE=PostgreSQL`), [ADR-004](ADR-004-integrations.md) (provider-neutral ports, adapters at the
boundary), [ADR-005](ADR-005-api-design.md) (`/api/v1`, one error shape),
[ADR-011](ADR-011-data-model-v2.md) (no JSONB, no partial indexes, derived aggregates computed on
read), [ADR-012](ADR-012-authentication-and-multi-user-isolation.md) (`users.id UUID`,
`CurrentUserProvider`, cross-user reads answer 404) and [ADR-008](ADR-008-observability.md) (secrets
never logged).

## Context

The onboarding wizard asks six screens' worth of questions and then loses almost all of them.

What it collects lands in `user_profile`'s `onboarding_*` columns (`V8__user_profile.sql`, carried
across verbatim by `V28__classb_user_id.sql` when `owner_id` became `user_id UUID`): the training
days as a comma-joined list of Spanish weekday labels (`'Lunes'`, `'Martes'`, … —
`frontend/src/pages/onboarding/steps/TrainingAvailabilityStep.tsx`), the equipment as a comma-joined
list of Spanish sentences (`'Sin equipamiento (peso corporal)'`, `'Barra y discos'`, … —
`EquipmentStep.tsx`), a nutrition preference enum, and a free-text "Alimentos que prefieres evitar".
Three problems compound there:

1. **Nothing reads them.** Every one of those step components says so in its own comment: "No
   training-plan backend consumes this yet (bootstrap)". `frontend/src/pages/settings/
   TrainingNutritionSection.tsx` says the same.
2. **The write path is full-replace and keeps no history.** `UserProfileService#submitOnboardingAnswers`
   rebuilds the whole `UserProfile` record and calls `repository.save`, which is one
   `UPDATE user_profile SET … WHERE user_id = ?` over 38 columns
   (`JdbcUserProfileRepository`). Answering the wizard a second time overwrites the first
   answers with no trace. A plan request is an event — "on this date, with this weight, I asked for
   this" — and an event cannot live in a row that is continuously overwritten.
3. **The wizard's answers never even reach the canonical profile fields.** `submitOnboardingAnswers`
   copies `current.mainGoal()` through unchanged; the goal the user picked stays in the
   `onboarding_goal_selected` draft column and never becomes `user_profile.main_goal`. Verified in
   `UserProfileService` — every constructor argument except `answers` and `firstRunCompleted` is
   `current.*`.

Meanwhile the *output* side is already built and is genuinely good. `V53__nutrition_plan.sql`
models a plan as `nutrition_plan` → `nutrition_plan_day` → `nutrition_plan_meal` →
`nutrition_plan_meal_item`, with AI origin as a first-class fact (`generated_by VARCHAR(16) CHECK IN
('HUMAN','AI')`, `generation_prompt TEXT`, `generation_metadata TEXT` — TEXT and not JSONB because
H2 has none and nothing queries inside it). `POST /api/v1/nutrition/plans`
(`NutritionPlanController`, `NutritionPlanRequest`) already accepts that entire nested shape
including `generation{by,prompt,metadata}`, always creates as DRAFT whatever the body asks for, and
leaves activation to `POST /{id}/activation` through `PlanActivationService` (+ `plan_acceptance`,
`V58__plan_acceptance.sql`). `GET /api/v1/nutrition/plans/import/catalog`
(`PlanImportController#catalog`, `ImportCatalogResponse`) already hands a model the exact vocabulary
it is allowed to use, and `docs/FORMA_Formato_Plan_JSON.md` already documents the file format in
prose meant to be pasted into a prompt.

So the gap is not "how do we store an AI plan". It is: **there is no durable, structured record of
what was asked for, and no infrastructure for a request that takes time to answer.**

Three further facts constrain the design, each verified:

- **Calculated macros are never stored, and the AI's arithmetic is not trustworthy.**
  `NutritionPlanReader` resolves each item to grams and sums it through `NutritionCalculator.
  itemTotals` on every read; `V53` explains at length why no `calculated_*` column exists. Section 11
  of `docs/FORMA_Spec_Modelo_Datos_Plan_Alimentacion.md` asks for the recalculated total to be
  compared against the AI's claimed `target_*` *before storing or activating*, with example
  tolerances (±5 % kcal, ±5 g protein, ±10 g carbs, ±5 g fat). **That validation does not exist.**
  The closest thing, `TargetComparison` (FOR-32), is four booleans asking `totals >= targets` — a
  "reached / short" display aid with no tolerance band and no drift figure. And the need is not
  hypothetical: `V56__excel_diet_plan.sql` imported a real LLM-authored week whose stated daily
  calories were 379–702 kcal above what its own listed food actually sums to, every single day,
  while getting protein essentially right. That plan is still the seeded plan the application ships.
- **There is no asynchronous infrastructure of any kind.** A repository-wide search for `@Async`,
  `@EnableAsync`, `CompletableFuture`, `ExecutorService` and `TaskExecutor` returns exactly one
  file, and it matches none of those: `adapter/scheduling/PlanLeadRetentionJob`, a single
  `@Scheduled` cron job. Whatever waiting model this design picks is new infrastructure and has to
  justify itself as such.
- **There is one external-provider precedent and it is worth copying.** Withings: provider-neutral
  ports in `application/` (`ProviderOAuthGateway`, `ProviderMeasuresGateway`, both of whose javadoc
  states the rule — no provider type ever appears in the signature), a concrete adapter in
  `adapter/withings/` split into `WithingsHttpTransport` (seam) and `JdkHttpWithingsTransport`
  (JDK `HttpClient`, fixed 10 s timeout, no retry anywhere), `@Value`-bound `forma.integrations.
  withings.*` config that defaults to empty so an unconfigured environment still boots, and
  fixture-based tests under `backend/src/test/resources/fixtures/withings/`.

## Decision

1. **A new table, `plan_request`, one row per request, keyed to `users(id)`.** Not more columns on
   `user_profile`: its write path is full-replace with no history (Context, point 2), so a second
   request would silently erase the first, and a request is precisely the thing you need to be able
   to read back months later to explain why a plan says what it says. Not `plan_lead` either: that
   table has no `user_id` at all, deliberately models an *anonymous* funnel lead
   (`V61__plan_lead.sql`: "Un lead no es una cuenta"), and carries marketing-consent columns that
   have nothing to do with a signed-in user asking for a plan. Both alternatives were rejected by
   the product owner before this ADR and the reasoning above is why that call was right.

2. **The request row carries a status lifecycle and the frontend polls.** `PENDING` → `GENERATING`
   → `READY` | `FAILED`. Who moves it, precisely:

   | transition | who | when |
   |---|---|---|
   | (none) → `PENDING` | `PlanRequestService#request`, in the `POST` that creates the row | the row is written already answered-for: every input is resolved and frozen before insert |
   | `PENDING` → `GENERATING` | `PlanRequestDispatchJob`, by a conditional `UPDATE … WHERE id = ? AND status = 'PENDING'` | immediately before calling the port |
   | `GENERATING` → `READY` | the ingest, **in the same transaction** that writes the `nutrition_plan` rows | a `READY` row therefore always has a `nutrition_plan_id`, enforced by CHECK |
   | `GENERATING` → `FAILED` | the ingest on a structural rejection, or the sweeper on timeout | |

   `READY` and `FAILED` are terminal. A retry is a **new row**, never a resurrected one — the same
   reasoning `V61` gives for not deduplicating leads by email: "la segunda petición es un hecho
   distinto de la primera".

3. **The waiting model is a `@Scheduled` dispatcher, not `@Async`.** This is new infrastructure and
   the minimal version is the one that survives a restart. An `@Async` method (or a
   `CompletableFuture`) handed the request on the POST thread is lost the moment the process dies:
   the row stays `PENDING` forever with nothing in the system whose job is to notice. A sweeper that
   claims `PENDING` rows resumes by construction, needs no `@EnableAsync`, no executor, no new
   dependency, and reuses the *only* async pattern this repository has
   (`PlanLeadRetentionJob`). The claim is the conditional `UPDATE` in the table above: its
   affected-row count is the lock, so two instances racing produce one winner and one no-op — the
   same SQL-level guard `JdbcUserRepository#linkGoogleSubject` uses (`ADR-014` decision 3) and the
   same "safe to run twice, so no leader election" argument `PlanLeadRetentionJob`'s javadoc
   already makes.

   **A second sweep is mandatory, not optional**: a `GENERATING` row older than a configured
   deadline becomes `FAILED` with `failure_code = 'TIMEOUT'`. Without it, one crashed dispatch wedges
   that user permanently — because of decision 4, they cannot open another request while one is
   open.

4. **At most one open request per user, enforced in the database by the nullable-sentinel pattern.**
   `open_marker CHAR(1)` holds `'1'` while the status is `PENDING` or `GENERATING` and `NULL`
   otherwise, with `UNIQUE (user_id, open_marker)`. SQL compares NULLs as distinct on both engines,
   so any number of finished requests coexist and at most one open one does. This is ADR-011's
   portable substitute for a PostgreSQL partial unique index (H2 cannot parse a `WHERE` clause on
   `CREATE INDEX`), already used by `nutrition_plan.active_marker` (V53) and `food_serving.
   default_marker` (V49). The paired CHECK is written **two-valued** — `open_marker IS NOT NULL`,
   never `open_marker = '1'` — because V53 found and documented the hole in the `= '1'` form: for a
   row in an open status with a NULL marker the comparison evaluates to UNKNOWN, and a CHECK accepts
   UNKNOWN, letting through exactly the row the unique index cannot constrain.

5. **Both objective vocabularies are carried, with an explicit mapping, because they answer
   different questions and one of them is load-bearing arithmetic.** `MainGoal`
   (`COMPOSICION|RENDIMIENTO|HABITO`) is the profile's standing answer, and it is what
   `nutrition_plan.objective` stores — V53 says so in the column comment: "in the vocabulary the
   profile already speaks". `PlanObjective` (`WEIGHT_LOSS|MUSCLE_GAIN|MAINTENANCE|HEALTHY_EATING`)
   is the per-plan clinical objective and the only one of the two carrying a number:
   `PlanObjective#factor()` is what `EnergyRequirement.of` multiplies by, and `PlanObjective`'s own
   javadoc argues at length that collapsing the two "would make the funnel's four options into three
   that do not fit". Picking one would mean either losing the factor or breaking V53's column
   contract. So `plan_request` carries both:

   | `main_goal` (wizard, standing) | default `plan_objective` | factor applied |
   |---|---|---|
   | `COMPOSICION` | `WEIGHT_LOSS` | 0.80 |
   | `RENDIMIENTO` | `MAINTENANCE` | 1.00 |
   | `HABITO` | `HEALTHY_EATING` | 1.00 |

   The default is a **starting point the user can override in the wizard**, and the redesigned
   wizard should ask. Deriving `COMPOSICION → WEIGHT_LOSS` silently applies a 20 % deficit to
   somebody who may have meant to gain; `MUSCLE_GAIN` is an equally honest reading of
   "recomposición". A derived default that nobody sees is the kind of hidden decision `PlanObjective`'s
   javadoc exists to prevent ("son EDITORIAL, no aritmético… escritas aquí para que haya un sitio
   donde discutirlas").

6. **The remaining vocabulary mismatches resolve toward the machine-readable side, at the delivery
   boundary.** The Spanish UI label is a label; it never crosses the port (ADR-004's rule, applied
   to our own frontend rather than to a provider).

   | fact | wizard collects | funnel collects | `plan_request` stores |
   |---|---|---|---|
   | sex | `Sex` = `MALE\|FEMALE\|OTHER` | `MALE\|FEMALE` only | `Sex`, all three, verbatim |
   | training availability | Spanish weekday labels (`'Lunes'`…) | `daysPerWeek: number` | **both**: `training_weekdays` as `java.time.DayOfWeek` names, plus `training_days_per_week` |
   | diet | `preference` = `OMNIVORE\|VEGETARIAN\|VEGAN\|GLUTEN_FREE\|OTHER` | `eatingStyle` = `ESTANDAR_ESPANOL\|MEDITERRANEA` | **two orthogonal fields**: `diet_pattern` (exclusion rule) + `cuisine_style` (`ESPANOLA\|MEDITERRANEA\|UNSPECIFIED`) |
   | equipment | Spanish sentences | not collected | `Equipment` enum CSV, mapped 1:1 from the six existing labels |

   - **Sex needs no new vocabulary and no `UNSPECIFIED`.** `EnergyRequirement#sexOffset` already
     handles all three values — `OTHER` takes −78.0, the midpoint — and its javadoc already argues
     why ("refusing would leave somebody unable to use the funnel at all; picking one would decide
     something about them that they did not"). The funnel's two-value restriction is the funnel's
     own narrowing, not a domain limit.
   - **Both weekday forms are kept because both are needed.** `nutrition_plan_day.day_number` is
     1 = Monday and `day_type` is `RUNNING|STRENGTH|REST` (V53), so a plan that puts the carb-heavy
     day on the wrong weekday is wrong even with the right count. `days_per_week` stays because it
     is what `PlanDraftRequest` already speaks and what an agent can reason about directly.
   - **Diet is genuinely two axes, and mashing them would be the mistake.** `ESTANDAR_ESPANOL` and
     `MEDITERRANEA` are cuisine styles; `VEGAN` and `GLUTEN_FREE` are exclusion rules. A vegan
     Mediterranean diet is not a contradiction, and a single column cannot say it. `ESTANDAR_ESPANOL`
     maps to `cuisine_style = ESPANOLA` with `diet_pattern = OMNIVORE`.
   - **Equipment is stored as a CSV in one `VARCHAR`**, matching the existing
     `onboarding_equipment_items` TEXT-CSV precedent and ADR-011's no-JSONB rule. It is carried but
     **unused by this slice** — it is a training input. See decision 11.

7. **Targets: where the request reads from, and where an accepted plan writes to.** No sixth
   location is invented, and the one new column that looks like a target is not one.

   *Read, at request time, and then frozen on the row*: height, sex, birth date and activity level
   from `user_profile` (V8/V28); the goal from `user_profile.main_goal`, falling back to the
   `onboarding_goal_selected` draft because — verified above — the wizard never promotes the draft;
   weight from the newest `body_measurement` row (`BodyMeasurementRepository#list` is documented and
   implemented as `ORDER BY measured_at DESC`, V30); optional macro targets from
   `user_profile.protein_target_g` (V8) and `fat_target_g`/`carbs_target_g` (V20). From those,
   `EnergyRequirement.of(...)` computes `plan_kcal`, which is **stored and never recomputed on
   read** — the identical decision `V61` made for `plan_lead.plan_kcal`, for the identical stated
   reason: it is the figure that person was shown and accepted, and a plan has to be comparable
   against the number that convinced them rather than the number today's formula would give.

   *Written, on acceptance*: `nutrition_plan.target_kcal_min`/`target_kcal_max` and
   `target_protein_g`/`target_carbs_g`/`target_fat_g`, plus `nutrition_plan.objective` = the
   `MainGoal`. **Nothing writes back to `user_profile`.** `PersonalTargets` (V20) is Diego's
   hand-transcribed *Perfil* reference data, seeded verbatim from `docs/fitness_os.xlsm`; a
   generated plan quietly overwriting it would make "personal targets" mean two different things on
   two different screens. `DefaultObjectives` and `ProfileBaseline` are likewise left alone.

   `plan_request.plan_kcal` is deliberately **not** a sixth target. It is request audit: what was
   asked for, at that moment, from those inputs. Nothing renders it as a target and nothing
   compares against it at read time. That distinction has to stay true, and it is the first thing
   to check if this column ever starts being read by a screen.

8. **The AI plan lands through a NEW endpoint, reusing the existing request body verbatim.**
   `POST /api/v1/nutrition/plans` is not extended. Its contract is "the caller states a complete
   plan; it is created as DRAFT" and the plans screen depends on exactly that; the AI ingest
   additionally has to bind to a `plan_request`, run the tolerance audit, move a status and record a
   validation report. Bolting four responsibilities onto an endpoint the UI already uses is how one
   rule ends up with two doors that disagree — the failure `PlanActivationService`'s javadoc
   describes happening for real when activation had two doors and one of them did not record the
   acceptance.

   But the **body** is `NutritionPlanRequest` unchanged, wrapped. This is the same call
   `PlanImportRequest` already made and documented: "a format for a model to generate and a format
   for a form to submit are the same information, and having two would mean every field added to one
   had to be remembered in the other."

   **Direction, for the first slice: FORMA calls the agent and reads the plan from the HTTP
   response.** The alternative — the agent calling back into `POST /api/v1/plan-requests/{id}/plan` —
   needs an inbound credential for a caller that is not a human session, and ADR-012's
   session-cookie model has no answer for that; inventing one is a whole security surface for a
   convenience we do not yet need. The status lifecycle is what makes the swap cheap later: when the
   agent becomes slow enough to need a callback, only the adapter and one new authenticated endpoint
   change, and `PENDING`/`GENERATING` already mean the right things.

9. **Section 11's tolerance validation is implemented at ingest, and a drift does NOT reject the
   plan.** The arithmetic needs nothing new: `NutritionPlanReader` already resolves items to grams
   and `NutritionCalculator.itemTotals` already sums them, and reusing that is the only way the
   audit can agree with what the screens will later show. What is new is the comparison — per day,
   claimed `target_*` against recalculated sum, against the section 11 tolerances (±5 % kcal, ±5 g
   protein, ±10 g carbs, ±5 g fat), configurable.

   **Two kinds of failure, two different answers:**

   - **Structural** — an unknown `foodId`, a `servingId` that belongs to a different food, an item
     that is both a food and a recipe or neither: **reject the whole payload**, write nothing,
     `FAILED`. This check already exists as `NutritionPlanService#problemsIn`, which collects *every*
     problem rather than the first precisely because the thing writing these payloads is a language
     model.
   - **Arithmetic drift** — the food is all real, the totals the agent claimed are not what it
     listed: **store the plan as DRAFT, mark the request `READY`, record the drift in
     `validation_report`, and never auto-activate.** The evidence for this policy is in the
     repository: V56's diet drifted 379–702 kcal on all seven days, and it is *still the plan the
     application ships*, because the food was fine — only the model's addition was wrong, and the
     application recomputes the true totals on every read anyway. Rejecting would have thrown away a
     usable week over a number nothing depends on. Refusing to auto-activate is what keeps that from
     becoming permission to ship nonsense: a human sees the drift and decides.

   **The claimed targets are stored as sent, never overwritten with the recomputed sum.** V53 is
   explicit that target and calculated total being different things *is the point* — "a day that adds
   up to 2100 against a target of 2320 is 220 kcal short, and a model where the target IS the sum can
   never say so". Silently correcting the target would delete the only evidence that the agent was
   wrong.

10. **The future AI client is a port in `application/`, an adapter in `adapter/`, following
    Withings.** Port: `PlanGenerationGateway`, with one method
    `GeneratedNutritionPlan generate(PlanGenerationCommand command)` throwing
    `PlanGenerationException`. What crosses the boundary is **domain and application types only —
    never provider JSON, never a Jackson tree, never an HTTP status**; this is ADR-004's rule as both
    `ProviderOAuthGateway` and `ProviderMeasuresGateway` state it in their own javadoc. Adapter:
    `adapter/planagent/`, split into a `PlanAgentTransport` seam and an `HttpPlanAgentTransport`
    using the JDK `HttpClient` — the same split `WithingsHttpTransport`/`JdkHttpWithingsTransport`
    uses, which exists so tests run against fixtures and never a network. Fixtures go under
    `backend/src/test/resources/fixtures/plan-agent/`, mirroring `fixtures/withings/`.

    **Two honest departures from that precedent, called out rather than smuggled in:**
    - *Timeout*. `JdkHttpWithingsTransport.REQUEST_TIMEOUT` is a fixed 10 s. A model generating a
      week of meals will not answer in 10 s. This adapter needs a configurable, much larger timeout
      (proposed default 120 s), and that number is a guess until something real is measured.
    - *No retry, still*. There is no retry or backoff anywhere in this codebase and this adds none.
      A failed attempt becomes a `FAILED` row the user can see and act on, which is louder and more
      honest than a silent second attempt. `attempt_count` exists on the row so that adding retry
      later does not need a migration.

    Configuration is `forma.plan-agent.*` read with plain `@Value`, **empty by default** so CI and
    every unconfigured environment still boots — the exact lesson ADR-014 decision 6 records after
    Spring Boot's own property binding failed startup on an empty `client-id`. Documented in
    `docs/configuration.md`. The API key is a secret and is never logged (ADR-008), and neither is
    the response body.

11. **Training is out of this slice, and the request is shaped so nothing has to be rebuilt when it
    arrives.** No `training_plan` tables — for the same reason V53 declined to create ADR-011's
    `plan` parent: it would be a join through an empty table. But `plan_request` carries
    `training_weekdays`, `training_days_per_week` and `equipment` **from day one**, unused, because
    the request is the *input* and the nutrition plan is only one of its *outputs*. When ADR-011's
    `training_plan`/`training_session` tables land, the same row feeds them; a second request table
    would be the mistake this column choice prevents.

    One column is expected to move: `plan_request.nutrition_plan_id` points at `nutrition_plan(id)`
    today because that is the only child that exists. When ADR-011's `plan` parent is created and
    both children hang off it, this becomes `plan_id`. Saying so now is cheaper than discovering it
    then.

12. **No health data, and the free-text question that collected some is removed.** No allergies, no
    pathologies, no injuries — GDPR article 9 special category, and the same posture `V61` took
    deliberately and explained at length ("recogerlos en un formulario público para no usarlos
    todavía sería lo peor de las dos opciones").

    The consequence is concrete: `user_profile.onboarding_nutrition_restrictions` (`VARCHAR(500)`,
    V8, carried by V28) currently feeds nothing and, under this decision, will never feed anything.
    **Recommendation: remove the question from the wizard now, and clear then drop the column in a
    later migration.** The alternative — leave it dormant — is worse, and the reason is what the
    field actually contains rather than what its label says. The step's own comment insists the
    wording is preference-framed and not diagnostic ("«restricciones» asks about personal food
    choices, not medical conditions"), and the label on screen is "Alimentos que prefieres evitar".
    But it is a free-text box, and people type diagnoses into free-text boxes; the repository's own
    test fixture fills it with `'Frutos secos'` (`onboardingStorage.test.ts`), which is a nut
    allergy in every practical sense. Keeping a column that may hold special-category data, that
    nothing reads, that has no retention story and no stated purpose, is precisely the liability
    V61 refused to take on — and GDPR article 5.1(b)/(e) is the reason it is not merely untidy.

    Dropping a column is a destructive migration, which ADR-003 permits only when "documented and
    reviewed"; this decision is that documentation. The migration clears the values first
    (`UPDATE user_profile SET onboarding_nutrition_restrictions = ''`) and drops the column in the
    same script, so the data is gone even where a backup of the pre-drop table is later restored
    column-by-column.

    `onboarding_nutrition_preference` **survives**: an enum of dietary patterns is a preference, not
    a health record, and it is what feeds `diet_pattern`.

### The `plan_request` table

| column | type | null | notes |
|---|---|---|---|
| `id` | UUID | PK | |
| `user_id` | UUID | NOT NULL | FK → `users(id)` — real FK, not a placeholder; ADR-012 shipped `users` |
| `status` | VARCHAR(16) | NOT NULL | `PENDING`/`GENERATING`/`READY`/`FAILED`, default `PENDING` |
| `open_marker` | CHAR(1) | NULL | `'1'` iff status is `PENDING` or `GENERATING` |
| `contract_version` | VARCHAR(16) | NOT NULL | which contract this payload spoke (decision 13 below) |
| `catalog_version` | VARCHAR(32) | NULL | which catalog snapshot travelled with it |
| `sex` | VARCHAR(16) | NOT NULL | `Sex` |
| `age_years` | INTEGER | NOT NULL | derived from `birth_date` at request time, frozen |
| `weight_kg` | NUMERIC(5,1) | NOT NULL | newest `body_measurement`, frozen |
| `height_cm` | NUMERIC(5,1) | NOT NULL | |
| `activity_level` | VARCHAR(32) | NOT NULL | `ActivityLevel` |
| `main_goal` | VARCHAR(16) | NOT NULL | `MainGoal` — what an accepted plan writes to `nutrition_plan.objective` |
| `plan_objective` | VARCHAR(32) | NOT NULL | `PlanObjective` — what carries the arithmetic factor |
| `training_days_per_week` | INTEGER | NOT NULL | |
| `training_weekdays` | VARCHAR(64) | NULL | CSV of `DayOfWeek` names; NULL = nobody said |
| `equipment` | VARCHAR(255) | NULL | CSV of `Equipment`; unused by this slice (decision 11) |
| `meals_per_day` | INTEGER | NOT NULL | |
| `diet_pattern` | VARCHAR(32) | NOT NULL | exclusion rule; `UNSPECIFIED` when not answered |
| `cuisine_style` | VARCHAR(32) | NOT NULL | `ESPANOLA`/`MEDITERRANEA`/`UNSPECIFIED` |
| `plan_kcal` | INTEGER | NOT NULL | frozen `EnergyRequirement#planKcal`, never recomputed (decision 7) |
| `target_protein_g` | NUMERIC(6,1) | NULL | from the profile when set |
| `target_carbs_g` | NUMERIC(6,1) | NULL | |
| `target_fat_g` | NUMERIC(6,1) | NULL | |
| `request_payload` | TEXT | NULL | exactly what crossed the wire, for audit. TEXT, not JSONB (ADR-011) |
| `validation_report` | TEXT | NULL | the section-11 audit result as JSON-in-TEXT; NULL until ingest |
| `nutrition_plan_id` | UUID | NULL | FK → `nutrition_plan(id)`; NOT NULL in practice once `READY` |
| `failure_code` | VARCHAR(32) | NULL | `TIMEOUT`/`UNREACHABLE`/`REJECTED`/`MALFORMED` |
| `failure_detail` | TEXT | NULL | safe summary. Never a stack trace, never a secret (ADR-005, ADR-008) |
| `attempt_count` | INTEGER | NOT NULL | default 0; exists so adding retry needs no migration |
| `requested_at` | TIMESTAMP WITH TIME ZONE | NOT NULL | default `CURRENT_TIMESTAMP` |
| `dispatched_at` | TIMESTAMP WITH TIME ZONE | NULL | when it became `GENERATING` — what the timeout sweep reads |
| `completed_at` | TIMESTAMP WITH TIME ZONE | NULL | |
| `updated_at` | TIMESTAMP WITH TIME ZONE | NOT NULL | default `CURRENT_TIMESTAMP` |

Constraints:

```sql
CHECK (status IN ('PENDING', 'GENERATING', 'READY', 'FAILED'))
CHECK (open_marker IS NULL OR open_marker = '1')
-- Two-valued throughout: status is NOT NULL and IS NOT NULL is never UNKNOWN. See decision 4.
CHECK (
  (status IN ('PENDING', 'GENERATING') AND open_marker IS NOT NULL)
  OR (status NOT IN ('PENDING', 'GENERATING') AND open_marker IS NULL)
)
CHECK (status <> 'READY' OR nutrition_plan_id IS NOT NULL)
CHECK (age_years BETWEEN 14 AND 120)      -- mirrors PlanDraftRequest's bean validation
CHECK (weight_kg > 0 AND height_cm > 0)
CHECK (meals_per_day BETWEEN 3 AND 6)
CHECK (training_days_per_week BETWEEN 0 AND 7)
CHECK (plan_kcal > 0)
```

Indexes:

```sql
CREATE UNIQUE INDEX ux_plan_request_user_open ON plan_request (user_id, open_marker);
CREATE INDEX ix_plan_request_user_requested ON plan_request (user_id, requested_at);
CREATE INDEX ix_plan_request_status ON plan_request (status);  -- the dispatcher's and sweeper's scan
```

`training_days_per_week` allows 0 where `PlanDraftRequest` requires 3: the funnel asks somebody
motivated enough to fill in four screens, and the wizard's training-days step is explicitly optional
("¿Qué días de la semana puedes entrenar? (opcional)").

**Retention.** No automatic deletion, and that is a decision rather than an omission. `plan_lead`
has a twelve-month retention job because its rows are people who never became accounts and the
privacy notice promises a period. A `plan_request` belongs to an account, holds no special category
data (decision 12), and is the audit trail explaining a plan the user may still be following —
deleting it would make an active plan unexplainable. It therefore lives and dies with the account:
the FK to `users(id)` should be `ON DELETE CASCADE`, matching `plan_acceptance` (V58). If the
privacy notice ever states a period for it, that promise needs a job, exactly as
`PlanLeadRetentionJob`'s javadoc argues.

### Contract versioning

13. **Both payloads carry a `contractVersion`, and the outbound one is stored on the row.** The
    repository has **no DTO versioning scheme today** — verified: no `schemaVersion`,
    `contractVersion`, `apiVersion` or `X-Api-Version` anywhere in `backend/src/main/java`. This is
    genuinely new and needs its own argument rather than a claim of precedent.

    ADR-005's answer to versioning is the base path (`ApiPaths.V1`; "a new API version is introduced
    as a new base path"), and for every existing FORMA DTO that is sufficient, because both ends of
    every contract live in this repository and ship in one deployment. The agent is the **first
    consumer FORMA does not deploy**. The two sides will move independently and, on some day nobody
    plans, will disagree — and a payload that cannot say which contract it speaks fails that day by
    being misread rather than rejected. A version field in the body (not the path) is right here
    because there is one endpoint pair whose *shape* evolves, not a whole API surface.

    `catalog_version` is a separate axis and is stored separately: it says which food vocabulary was
    in force, which is data drift, not shape drift. Conflating them would make a newly added food
    look like a contract change.

## Consequences

- **The first real background work in FORMA.** A `@Scheduled` dispatcher, a claim-by-UPDATE, and a
  timeout sweep are a small amount of machinery but a new *category* of it. Everything that follows
  — training generation, plan regeneration, e-mail delivery of the funnel's promised plan — will
  reach for this pattern, so it is worth getting the claim semantics right once.
- **Two clocks now matter.** The dispatch interval and the `GENERATING` deadline are operational
  knobs with user-visible consequences: too long a deadline wedges a user (decision 4), too short a
  one fails plans that were about to succeed.
- **The wizard must be redesigned, not merely re-pointed.** It has to stop asking for restrictions
  (decision 12), start asking the plan objective explicitly (decision 5), and its equipment and
  weekday answers have to be collected as values rather than as Spanish sentences (decision 6). The
  existing screens are draft-only by their own admission, so this is the first time those answers
  are load-bearing.
- **`GET /api/v1/nutrition/plans/import/catalog` becomes a contract surface.** It exists today for
  an admin pasting JSON into a prompt; under this design its shape is what an external system
  consumes. Changing it stops being a local decision.
- **A new error code is needed.** `docs/api-conventions.md` defines exactly five (`VALIDATION_ERROR`,
  `NOT_FOUND`, `UNAUTHORIZED`, `FORBIDDEN`, `INTERNAL_ERROR`). "You already have a request in
  flight" is a 409 and is none of them. Adding `CONFLICT` to `ApiErrorCode` is part of this work and
  touches a shared file.
- **An unconfigured environment must behave well.** With `forma.plan-agent.*` empty, requests would
  sit `PENDING` forever. The dispatcher must refuse to claim rows when unconfigured and the endpoint
  must say so up front, rather than accepting a request nothing will ever answer — the Withings
  "fail loudly at the point of use" posture, not silent acceptance.

## Risks

1. **The agent does not exist, so every number here is unmeasured.** The 120 s timeout, the dispatch
   interval and the `GENERATING` deadline are guesses. Mitigation: all three are configuration, and
   the first real integration is expected to move them.
2. **Embedding the catalog in the request does not scale forever.** At today's size (V25 seeds 24
   foods; V22/V36 add more) it is trivially cheap. At thousands it stops being a payload and the
   agent needs a credential to fetch the catalog itself — which is the inbound-auth surface decision
   8 avoids. Mitigation: stated as a threshold, not pretended away.
3. **A drifting plan stored as DRAFT can still be activated by a user who ignores the warning.**
   Mitigation: activation already requires a deliberate act through `PlanActivationService`, and the
   drift is recorded on the request. This design does not block activation, and that is a product
   call worth confirming.
4. **`plan_kcal` is a frozen number that will eventually look wrong.** The user's weight moves; the
   figure does not. Mitigation: it is audit, not a target (decision 7), and the guard is that
   nothing renders it.

## Alternatives rejected

- **Columns on `user_profile`** (rejected by the product owner; the supporting reasoning is
  Context point 2): full-replace write path, no history, and a request is an event.
- **Reuse `plan_lead`** (rejected by the product owner): no `user_id`, deliberately anonymous, and
  it mixes marketing consent — V61 argues its own separateness at length.
- **A synchronous call on the POST** (rejected by the product owner): ties a user-facing request to
  an unbounded LLM latency, gives the browser nothing to show, and loses the work on any failure with
  no record that it was ever asked for.
- **`@Async`/`CompletableFuture` instead of a sweeper**: does not survive a restart, and leaves
  `PENDING` rows nobody owns (decision 3).
- **Extending `POST /api/v1/nutrition/plans`**: four responsibilities on an endpoint the UI depends
  on, and two doors into one rule (decision 8).
- **A second request table for training later**: the request is the input; its outputs are plural
  (decision 11).
- **Storing the recomputed totals as `calculated_*` columns**: V53 refused this for stated reasons
  and ADR-011 reached the same conclusion independently; the answer to "the AI can state a wrong
  total" is to validate it, not to keep a second copy (decision 9).
- **Rejecting a plan that fails the tolerance check**: V56 is the counter-example, and it is in the
  repository (decision 9).

## Rules

- One open `plan_request` per user, enforced only by the nullable-sentinel + `UNIQUE (user_id,
  open_marker)` pattern, with the CHECK written `IS NOT NULL` and never `= '1'`.
- A `READY` request always points at a stored plan; the status and the plan are written in one
  transaction.
- Terminal states are terminal: a retry is a new row.
- Never store a recalculated macro total; never overwrite a claimed target with it.
- An AI plan is created as DRAFT and is never activated by the system.
- No allergies, pathologies or injuries in the request, the payload, or the table — in any slice.
- Nothing provider-shaped crosses `PlanGenerationGateway`: domain and application types only.
- The agent's API key, the outbound payload's secrets and the raw response body are never logged.
- The Spanish UI label never crosses the boundary; it is mapped to a value at the delivery layer.

## Implementation plan

PR-sized slices in dependency order. Each carries its own verification; none of them is "and then
everything works".

1. **Vocabulary and schema.** `Equipment`, `DietPattern`, `CuisineStyle` enums; the weekday-label →
   `DayOfWeek` mapping; Flyway `V63__plan_request.sql` (V62 is currently the highest).
   *Verify*: migration applies on PostgreSQL 17 and H2 `MODE=PostgreSQL`; tests that the open-marker
   unique index admits many closed requests and exactly one open one, and that each CHECK rejects
   the state it exists for.
2. **Capture.** `PlanRequestService` + `JdbcPlanRequestRepository`; `POST /api/v1/plan-requests`
   (resolves profile + newest measurement, computes and freezes `plan_kcal`, writes `PENDING`) and
   `GET /api/v1/plan-requests/current`. `CONFLICT` added to `ApiErrorCode`.
   *Verify*: integration test through the real HTTP layer; a second request while one is open
   answers 409 with the new code; another user's request answers 404, never 403 (ADR-012).
3. **The wizard, redesigned.** Drops the restrictions textarea; asks the plan objective; collects
   equipment and weekdays as values; submits to the new endpoint and polls.
   *Verify*: component tests for the new questions; `frontend/e2e/apiFixtures.ts` gains fixtures for
   both new endpoints, or every fixture-backed run renders an error state (AGENTS.md).
4. **The section-11 audit, against a plan that is known to be wrong.** A pure domain service
   comparing claimed `target_*` to `NutritionCalculator` sums, with configurable tolerances. No
   ingest yet — it runs against plans already in the database.
   *Verify*: run it over V56's seeded plan and assert it reports the known 379–702 kcal drift on all
   seven days while protein passes. That is a regression test with a real, documented answer.
5. **Port and adapter, with no live call.** `PlanGenerationGateway`, `PlanGenerationCommand`,
   `GeneratedNutritionPlan`, `PlanGenerationException`; `adapter/planagent/` with the transport
   seam; `forma.plan-agent.*` config, empty by default; `docs/configuration.md` updated.
   *Verify*: fixture-driven tests under `fixtures/plan-agent/` covering a good plan, a malformed
   response and an unreachable agent; the application boots with the config empty; a log assertion
   that no configured secret appears in output.
6. **Dispatcher and ingest.** `PlanRequestDispatchJob` (claim, call, ingest, transition) and the
   timeout sweep; the new ingest endpoint reusing `NutritionPlanRequest`; `validation_report`
   written; structural failures rejected whole via `NutritionPlanService#problemsIn`.
   *Verify*: a `GENERATING` row older than the deadline becomes `FAILED('TIMEOUT')`; two concurrent
   claims produce one winner; a payload with one unknown `foodId` writes no rows and reports every
   problem at once; a payload that drifts produces a DRAFT plan, a `READY` request and a populated
   report.
7. **The waiting and result screens.** Polling, the drift warning, the explicit activation step.
   *Verify*: component tests for pending/ready/failed/drifting states; fixtures for each.
8. **The health-data cleanup.** Migration clearing then dropping
   `user_profile.onboarding_nutrition_restrictions`, plus the domain/DTO/frontend removals that
   follow.
   *Verify*: the column is gone on both engines; no reference to it remains in
   `backend/src/main/java` or `frontend/src`; this ADR is cited in the migration comment as the
   review ADR-003 requires for a destructive change.

## Open points

- **Does `COMPOSICION` default to `WEIGHT_LOSS`?** Decision 5 says yes and recommends asking
  explicitly instead. A product call.
- **May a user activate a plan that failed the tolerance audit?** Decision 9 stores it as DRAFT and
  warns but does not block. A product call.
- **The agent's authentication model.** Decision 8 picks outbound-only for the first slice
  specifically to avoid choosing one. It has to be chosen before any callback exists.
- **Whether `plan_request` is ever surfaced to the user as history** ("you asked for this on this
  date, weighing this"). The table supports it; no screen is proposed here.
