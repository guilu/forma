-- The plan that was living in Java, moved into the tables V53 made for it.
--
-- Additive on top of V53 (ADR-003). Every meal, food and amount below is transcribed verbatim from
-- domain/NutritionDayCatalog.java, which this migration's slice deletes. Nothing is invented: the
-- same foods, the same grams, the same times, the same optional post-run recovery (FOR-134).
--
-- WHAT CHANGES SHAPE, AND WHY IT HAS TO
--
-- The old catalog held THREE day TEMPLATES — one running day, one strength day, one rest day — and
-- served whichever the caller asked for. This is a real week, seven rows, because that is what the
-- new model says a plan is: concrete days, not archetypes. Monday, Wednesday and Saturday are
-- running days and carry the same meals today; the point is that they no longer have to. The old
-- model could not say "Wednesday's run is longer, so Wednesday eats more" without changing every
-- running day at once.
--
-- Which weekday is which kind is NOT decided here. It is read from WeeklyTrainingDayPolicy — running
-- Mon/Wed/Sat, strength Tue/Thu/Sun, rest Fri — the same policy the training calendar runs on, so
-- the two cannot drift.
--
-- NO TARGETS ARE SEEDED, and that is deliberate. The old catalog set each day's target to the
-- computed total of its own meals, which made the target comparison a tautology: every seeded day
-- reached its target by construction, and TargetComparison could only ever answer yes. Rather than
-- copy that, every target here is left NULL — nobody decided one — and resolution falls back:
--
--     day target  ->  plan target  ->  the user's own profile figures (V20)
--
-- The profile already holds real numbers from the Perfil sheet (2300 kcal, 160 g protein, 260 g
-- carbohydrate, 70 g fat). Copying them onto the plan would be the same fact in two places; leaving
-- them where they are means correcting the profile corrects the plan.
--
-- ONE ACCOUNT, THE LEGACY ONE. The plan is seeded for the placeholder user V26 created, because that
-- is the only account with data. A newly registered account gets no plan and the nutrition endpoint
-- returns its empty state — which is truthful, and is the same "configure your plan" screen the
-- first-run gate already shows. The old behaviour, where every account silently shared three
-- constants compiled into the jar, was not truthful.

-- Status and marker go in together. The CHECK ties them, so an INSERT saying ACTIVE with a null
-- marker is refused on the spot rather than fixed by a follow-up UPDATE that might not run.
INSERT INTO nutrition_plan (id, user_id, name, description, status, active_marker, generated_by)
VALUES ('aaaaaaaa-0000-4000-8000-000000000001', '00000000-0000-0000-0000-000000000000',
        'Plan base', 'La semana que hasta ahora vivía en NutritionDayCatalog.java.',
        'ACTIVE', '1', 'HUMAN');

-- Día 1 (RUNNING)
INSERT INTO nutrition_plan_day (id, nutrition_plan_id, week_number, day_number, day_type, notes)
VALUES ('f72b69a8-b55b-5c06-93d6-07d7418be722', 'aaaaaaaa-0000-4000-8000-000000000001', 1, 1, 'RUNNING', 'Día de carrera: más carbohidratos, antes de correr.');
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, scheduled_time, optional)
VALUES ('5be2d51e-ed14-5b32-8c11-5f08692acbc8', 'f72b69a8-b55b-5c06-93d6-07d7418be722', 'BREAKFAST', 'Desayuno', 0, TIME '08:00:00', FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('a4deab81-5039-5ba4-afd7-ce45b92d9bfa', '5be2d51e-ed14-5b32-8c11-5f08692acbc8', 'oats', 120.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('c594ba19-dc51-55af-b176-0fbd3a0b991e', '5be2d51e-ed14-5b32-8c11-5f08692acbc8', 'banana', 120.0, 1);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('77adda86-b965-52b0-86f3-c492d48fa665', '5be2d51e-ed14-5b32-8c11-5f08692acbc8', 'whey-protein', 30.0, 2);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, scheduled_time, optional)
VALUES ('c3f3045d-5742-513a-9d56-08a4a68a54e5', 'f72b69a8-b55b-5c06-93d6-07d7418be722', 'LUNCH', 'Comida', 1, TIME '14:00:00', FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('0ad0e9c2-500d-58d8-908b-7cd23bf701ae', 'c3f3045d-5742-513a-9d56-08a4a68a54e5', 'rice', 200.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('a0a5f3b3-a97c-551e-8ae8-02b604921ad8', 'c3f3045d-5742-513a-9d56-08a4a68a54e5', 'chicken', 150.0, 1);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('4da17954-c84c-56cb-9593-2323702b423e', 'c3f3045d-5742-513a-9d56-08a4a68a54e5', 'vegetables', 150.0, 2);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, scheduled_time, optional)
VALUES ('928d7001-77d8-55e6-a0ec-73130580de46', 'f72b69a8-b55b-5c06-93d6-07d7418be722', 'PRE_WORKOUT', 'Snack pre-carrera', 2, TIME '18:00:00', FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('b4781b89-9b83-5274-b083-828d2e7486ae', '928d7001-77d8-55e6-a0ec-73130580de46', 'banana', 120.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('28d73975-f0a3-583e-969d-9d8075af0482', '928d7001-77d8-55e6-a0ec-73130580de46', 'oats', 40.0, 1);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, scheduled_time, optional)
VALUES ('9983b465-eab3-5a60-ac0b-60789052820d', 'f72b69a8-b55b-5c06-93d6-07d7418be722', 'POST_WORKOUT', 'Recuperación (opcional)', 3, TIME '20:00:00', TRUE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('9d821ffe-ddd6-50f3-b3ce-5b47e9ba89fb', '9983b465-eab3-5a60-ac0b-60789052820d', 'whey-protein', 20.0, 0);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, scheduled_time, optional)
VALUES ('99f66e6d-3543-57bd-9024-2a8df5726d93', 'f72b69a8-b55b-5c06-93d6-07d7418be722', 'DINNER', 'Cena ligera', 4, TIME '21:30:00', FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('58818fc4-72d0-5b5b-9266-92335a78b139', '99f66e6d-3543-57bd-9024-2a8df5726d93', 'fish', 150.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('08b62d6e-a5b2-5a5b-9c58-fa7343b71350', '99f66e6d-3543-57bd-9024-2a8df5726d93', 'potato', 150.0, 1);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('434e5fcb-9c7b-5bf2-b913-cf6a773c1c9b', '99f66e6d-3543-57bd-9024-2a8df5726d93', 'vegetables', 150.0, 2);

-- Día 2 (STRENGTH)
INSERT INTO nutrition_plan_day (id, nutrition_plan_id, week_number, day_number, day_type, notes)
VALUES ('3a89ea02-c02d-541a-9f2b-c9d5bc2ab1e1', 'aaaaaaaa-0000-4000-8000-000000000001', 1, 2, 'STRENGTH', 'Día de fuerza: proteína alta, carbohidratos moderados.');
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, scheduled_time, optional)
VALUES ('a26c1500-8c1c-5fc9-88bf-d3d6052d5cff', '3a89ea02-c02d-541a-9f2b-c9d5bc2ab1e1', 'BREAKFAST', 'Desayuno', 0, TIME '08:00:00', FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('4508965f-83cf-57d7-b7f0-3a74dcb54f3d', 'a26c1500-8c1c-5fc9-88bf-d3d6052d5cff', 'eggs', 150.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('062d39d5-f005-5515-8564-9aa0eca66172', 'a26c1500-8c1c-5fc9-88bf-d3d6052d5cff', 'oats', 60.0, 1);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('e6eb98da-50ba-585c-9905-300e3a690fd4', 'a26c1500-8c1c-5fc9-88bf-d3d6052d5cff', 'yogurt', 125.0, 2);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, scheduled_time, optional)
VALUES ('47faa7e5-1950-5fce-b708-a5986f0ef4da', '3a89ea02-c02d-541a-9f2b-c9d5bc2ab1e1', 'MID_MORNING', 'Media mañana', 1, TIME '11:00:00', FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('89ee710a-bf0a-5abe-9ba0-447f819c3074', '47faa7e5-1950-5fce-b708-a5986f0ef4da', 'whey-protein', 30.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('2c604e7c-3668-588e-9261-3452c6827406', '47faa7e5-1950-5fce-b708-a5986f0ef4da', 'banana', 100.0, 1);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, scheduled_time, optional)
VALUES ('8dd98c4b-de3a-5238-b4cf-da84962dc238', '3a89ea02-c02d-541a-9f2b-c9d5bc2ab1e1', 'LUNCH', 'Comida', 2, TIME '14:00:00', FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('4a37ac16-9676-55fe-bf3a-bf21378cdb3b', '8dd98c4b-de3a-5238-b4cf-da84962dc238', 'chicken', 150.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('00e2793d-7d53-528d-ba73-be3e81e04eac', '8dd98c4b-de3a-5238-b4cf-da84962dc238', 'rice', 250.0, 1);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('c0c84f85-4ffd-55a8-9706-994e17610d87', '8dd98c4b-de3a-5238-b4cf-da84962dc238', 'vegetables', 200.0, 2);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, scheduled_time, optional)
VALUES ('8fb7171a-2ccc-5d73-9af0-09b5cfacba11', '3a89ea02-c02d-541a-9f2b-c9d5bc2ab1e1', 'DINNER', 'Cena', 3, TIME '21:00:00', FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('7f21d1e4-3dcd-5f70-ad98-85ee0829fe51', '8fb7171a-2ccc-5d73-9af0-09b5cfacba11', 'turkey', 100.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('839e007f-3de5-57c7-97ff-85b73679fc34', '8fb7171a-2ccc-5d73-9af0-09b5cfacba11', 'potato', 150.0, 1);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('423c6874-3999-5dc6-be32-4777565915b6', '8fb7171a-2ccc-5d73-9af0-09b5cfacba11', 'vegetables', 150.0, 2);

-- Día 3 (RUNNING)
INSERT INTO nutrition_plan_day (id, nutrition_plan_id, week_number, day_number, day_type, notes)
VALUES ('42ddd7b2-3033-5f6f-a18a-74c6d6e474e4', 'aaaaaaaa-0000-4000-8000-000000000001', 1, 3, 'RUNNING', 'Día de carrera: más carbohidratos, antes de correr.');
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, scheduled_time, optional)
VALUES ('7e72d70a-6353-51b8-a8d4-8fcc115295ff', '42ddd7b2-3033-5f6f-a18a-74c6d6e474e4', 'BREAKFAST', 'Desayuno', 0, TIME '08:00:00', FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('28a3285c-6a08-5123-93b2-deabc7ec0de3', '7e72d70a-6353-51b8-a8d4-8fcc115295ff', 'oats', 120.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('fc6e32a0-d963-51af-b0ec-62b7a8232efb', '7e72d70a-6353-51b8-a8d4-8fcc115295ff', 'banana', 120.0, 1);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('fa09cb3b-7587-54dc-a37e-972437c57ccd', '7e72d70a-6353-51b8-a8d4-8fcc115295ff', 'whey-protein', 30.0, 2);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, scheduled_time, optional)
VALUES ('6957087c-ac04-506d-a59b-9d916cf077bf', '42ddd7b2-3033-5f6f-a18a-74c6d6e474e4', 'LUNCH', 'Comida', 1, TIME '14:00:00', FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('047096ac-4f48-5276-982a-25c2ad5e3838', '6957087c-ac04-506d-a59b-9d916cf077bf', 'rice', 200.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('d0003c9f-f989-5db8-a74e-a4eb9d39d2e0', '6957087c-ac04-506d-a59b-9d916cf077bf', 'chicken', 150.0, 1);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('b4386eb0-cb79-581a-984e-88d19e1f3509', '6957087c-ac04-506d-a59b-9d916cf077bf', 'vegetables', 150.0, 2);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, scheduled_time, optional)
VALUES ('cb66ebdf-c81c-5cd3-830b-b603b9f6aaff', '42ddd7b2-3033-5f6f-a18a-74c6d6e474e4', 'PRE_WORKOUT', 'Snack pre-carrera', 2, TIME '18:00:00', FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('8e614949-12ea-5e49-8dab-18e2f01be757', 'cb66ebdf-c81c-5cd3-830b-b603b9f6aaff', 'banana', 120.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('f439cbdc-5b77-53c3-9328-1ce72a9f4ccd', 'cb66ebdf-c81c-5cd3-830b-b603b9f6aaff', 'oats', 40.0, 1);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, scheduled_time, optional)
VALUES ('7a3d3ee2-04da-5c04-a1d0-a160145e504b', '42ddd7b2-3033-5f6f-a18a-74c6d6e474e4', 'POST_WORKOUT', 'Recuperación (opcional)', 3, TIME '20:00:00', TRUE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('9a0fb231-eb8e-5fd2-bcab-98c31b807dcd', '7a3d3ee2-04da-5c04-a1d0-a160145e504b', 'whey-protein', 20.0, 0);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, scheduled_time, optional)
VALUES ('6fe0cc84-15ab-5112-94f9-4c2ebf11d1a5', '42ddd7b2-3033-5f6f-a18a-74c6d6e474e4', 'DINNER', 'Cena ligera', 4, TIME '21:30:00', FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('33037849-dbfe-52e0-88f6-7b6b81f4aef4', '6fe0cc84-15ab-5112-94f9-4c2ebf11d1a5', 'fish', 150.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('22cb190f-54d1-5b01-b7e3-9c4b4e0f8326', '6fe0cc84-15ab-5112-94f9-4c2ebf11d1a5', 'potato', 150.0, 1);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('6ac15fd2-b48a-58b2-90fa-fb7f023ebffe', '6fe0cc84-15ab-5112-94f9-4c2ebf11d1a5', 'vegetables', 150.0, 2);

-- Día 4 (STRENGTH)
INSERT INTO nutrition_plan_day (id, nutrition_plan_id, week_number, day_number, day_type, notes)
VALUES ('15c70ba9-4ad2-558b-ac38-538329ee2b20', 'aaaaaaaa-0000-4000-8000-000000000001', 1, 4, 'STRENGTH', 'Día de fuerza: proteína alta, carbohidratos moderados.');
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, scheduled_time, optional)
VALUES ('3dab1643-5a67-5530-877e-16cafd16e333', '15c70ba9-4ad2-558b-ac38-538329ee2b20', 'BREAKFAST', 'Desayuno', 0, TIME '08:00:00', FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('49113099-c51f-503a-ba23-617f543cc6d8', '3dab1643-5a67-5530-877e-16cafd16e333', 'eggs', 150.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('5839a425-fba5-56bd-8567-993b9df11b30', '3dab1643-5a67-5530-877e-16cafd16e333', 'oats', 60.0, 1);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('2f191fb6-779a-594f-87df-efe8c29e6dc8', '3dab1643-5a67-5530-877e-16cafd16e333', 'yogurt', 125.0, 2);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, scheduled_time, optional)
VALUES ('cf1d3023-1f2a-5839-94bf-4cd71b44a687', '15c70ba9-4ad2-558b-ac38-538329ee2b20', 'MID_MORNING', 'Media mañana', 1, TIME '11:00:00', FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('31bd49f1-6b5d-5108-9582-2a16107ca1bb', 'cf1d3023-1f2a-5839-94bf-4cd71b44a687', 'whey-protein', 30.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('c0aba18e-9375-5906-b088-7cc496bb56bc', 'cf1d3023-1f2a-5839-94bf-4cd71b44a687', 'banana', 100.0, 1);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, scheduled_time, optional)
VALUES ('f645e24d-8ffb-5516-920e-b26a82b5ae41', '15c70ba9-4ad2-558b-ac38-538329ee2b20', 'LUNCH', 'Comida', 2, TIME '14:00:00', FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('f1442987-cd3c-5ce0-84f2-4fc611b6002a', 'f645e24d-8ffb-5516-920e-b26a82b5ae41', 'chicken', 150.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('a3646e03-b4fd-5a53-8f51-b3a34f48bc78', 'f645e24d-8ffb-5516-920e-b26a82b5ae41', 'rice', 250.0, 1);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('0c731465-32e7-5237-a26b-51042478c399', 'f645e24d-8ffb-5516-920e-b26a82b5ae41', 'vegetables', 200.0, 2);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, scheduled_time, optional)
VALUES ('65bea136-df5c-5fe6-9964-16cfd4b0ee8d', '15c70ba9-4ad2-558b-ac38-538329ee2b20', 'DINNER', 'Cena', 3, TIME '21:00:00', FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('a5b24999-efdf-5e05-8946-0ef280030952', '65bea136-df5c-5fe6-9964-16cfd4b0ee8d', 'turkey', 100.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('d7e05656-fa2d-5084-8e0d-aff71c856ffa', '65bea136-df5c-5fe6-9964-16cfd4b0ee8d', 'potato', 150.0, 1);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('8428c2d3-20f6-5254-b5a5-c43ebc02ba1b', '65bea136-df5c-5fe6-9964-16cfd4b0ee8d', 'vegetables', 150.0, 2);

-- Día 5 (REST)
INSERT INTO nutrition_plan_day (id, nutrition_plan_id, week_number, day_number, day_type, notes)
VALUES ('3974001b-db01-58c9-aada-3530a0aac4c3', 'aaaaaaaa-0000-4000-8000-000000000001', 1, 5, 'REST', 'Día de descanso: menos carbohidratos.');
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, scheduled_time, optional)
VALUES ('e1c6f33d-a49c-599b-9e92-ce1bfea845a8', '3974001b-db01-58c9-aada-3530a0aac4c3', 'BREAKFAST', 'Desayuno', 0, TIME '09:00:00', FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('08f81905-cd6a-5de8-b444-44b569a43a13', 'e1c6f33d-a49c-599b-9e92-ce1bfea845a8', 'eggs', 150.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('0fdef5e8-f3c3-5c1e-b387-9ea02bc84bd9', 'e1c6f33d-a49c-599b-9e92-ce1bfea845a8', 'yogurt', 125.0, 1);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('763534ee-c94a-531f-88d3-db7cd9e95fc6', 'e1c6f33d-a49c-599b-9e92-ce1bfea845a8', 'fresh-cheese', 100.0, 2);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, scheduled_time, optional)
VALUES ('262d659c-dcbd-50c2-8721-6b5ead2cf372', '3974001b-db01-58c9-aada-3530a0aac4c3', 'MID_MORNING', 'Media mañana', 1, TIME '12:00:00', FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('29d46b55-9041-5c8f-964d-21def0298cb5', '262d659c-dcbd-50c2-8721-6b5ead2cf372', 'yogurt', 125.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('149ae371-b341-5cd8-9828-b9ba8e27bed6', '262d659c-dcbd-50c2-8721-6b5ead2cf372', 'whey-protein', 10.0, 1);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, scheduled_time, optional)
VALUES ('e3612f2b-3c48-5037-a017-4598059b9f57', '3974001b-db01-58c9-aada-3530a0aac4c3', 'LUNCH', 'Comida', 2, TIME '14:30:00', FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('cb2ac8cb-de47-558f-be1c-ef0bdbe24c68', 'e3612f2b-3c48-5037-a017-4598059b9f57', 'chicken', 200.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('fcfe891d-49ab-5567-9b9d-3990e0e9bafa', 'e3612f2b-3c48-5037-a017-4598059b9f57', 'vegetables', 200.0, 1);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('12118d71-167a-5998-9e66-c37afbbe034a', 'e3612f2b-3c48-5037-a017-4598059b9f57', 'potato', 200.0, 2);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, scheduled_time, optional)
VALUES ('a377e22a-d9f6-5f67-83d2-e005fdf3e977', '3974001b-db01-58c9-aada-3530a0aac4c3', 'DINNER', 'Cena', 3, TIME '21:00:00', FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('c8deb1b1-b252-5239-b210-7dd226b734a6', 'a377e22a-d9f6-5f67-83d2-e005fdf3e977', 'fish', 200.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('e219516f-f608-5f73-a96b-80a93f3c4876', 'a377e22a-d9f6-5f67-83d2-e005fdf3e977', 'vegetables', 200.0, 1);

-- Día 6 (RUNNING)
INSERT INTO nutrition_plan_day (id, nutrition_plan_id, week_number, day_number, day_type, notes)
VALUES ('1a09fc11-458d-5718-8e71-773611c6bea9', 'aaaaaaaa-0000-4000-8000-000000000001', 1, 6, 'RUNNING', 'Día de carrera: más carbohidratos, antes de correr.');
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, scheduled_time, optional)
VALUES ('ee92c218-dee0-5c4c-8990-19cde36d9f78', '1a09fc11-458d-5718-8e71-773611c6bea9', 'BREAKFAST', 'Desayuno', 0, TIME '08:00:00', FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('6831d13e-bd7b-57ce-8709-6f5e11f22ed8', 'ee92c218-dee0-5c4c-8990-19cde36d9f78', 'oats', 120.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('6b9acb4a-cc48-5ffe-afcf-5094b3d7c571', 'ee92c218-dee0-5c4c-8990-19cde36d9f78', 'banana', 120.0, 1);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('34b56c08-602c-5159-a971-4a99954e9953', 'ee92c218-dee0-5c4c-8990-19cde36d9f78', 'whey-protein', 30.0, 2);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, scheduled_time, optional)
VALUES ('214a26c5-d3e4-5c1d-b117-65b04c28e7fa', '1a09fc11-458d-5718-8e71-773611c6bea9', 'LUNCH', 'Comida', 1, TIME '14:00:00', FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('f9c45e61-0143-57e0-8714-10e31276510f', '214a26c5-d3e4-5c1d-b117-65b04c28e7fa', 'rice', 200.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('ff715080-a3b5-58e8-9238-6831c6aa5746', '214a26c5-d3e4-5c1d-b117-65b04c28e7fa', 'chicken', 150.0, 1);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('323f5280-d197-5086-b133-7cdb63f6df56', '214a26c5-d3e4-5c1d-b117-65b04c28e7fa', 'vegetables', 150.0, 2);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, scheduled_time, optional)
VALUES ('6683f3e7-ddfc-5538-a55f-f84617c00db9', '1a09fc11-458d-5718-8e71-773611c6bea9', 'PRE_WORKOUT', 'Snack pre-carrera', 2, TIME '18:00:00', FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('ac9c54a0-a0ac-5094-b9c3-6a0c28c99f95', '6683f3e7-ddfc-5538-a55f-f84617c00db9', 'banana', 120.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('f9159647-4b47-5e25-9f4a-868f261a5187', '6683f3e7-ddfc-5538-a55f-f84617c00db9', 'oats', 40.0, 1);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, scheduled_time, optional)
VALUES ('db9f0944-ecc1-525e-8c35-5cba79d42284', '1a09fc11-458d-5718-8e71-773611c6bea9', 'POST_WORKOUT', 'Recuperación (opcional)', 3, TIME '20:00:00', TRUE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('cb219fed-2566-5fb4-98c4-312ec7357aae', 'db9f0944-ecc1-525e-8c35-5cba79d42284', 'whey-protein', 20.0, 0);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, scheduled_time, optional)
VALUES ('76fcaaf3-d24a-5019-b2f0-6b12e6e480c1', '1a09fc11-458d-5718-8e71-773611c6bea9', 'DINNER', 'Cena ligera', 4, TIME '21:30:00', FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('4cd4f720-dedd-54e1-be9c-b5d618928089', '76fcaaf3-d24a-5019-b2f0-6b12e6e480c1', 'fish', 150.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('0dac5e42-5d1e-5fc8-96f3-e07a19c51a44', '76fcaaf3-d24a-5019-b2f0-6b12e6e480c1', 'potato', 150.0, 1);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('be012fea-7bda-5f8e-844d-cfd8b841fa42', '76fcaaf3-d24a-5019-b2f0-6b12e6e480c1', 'vegetables', 150.0, 2);

-- Día 7 (STRENGTH)
INSERT INTO nutrition_plan_day (id, nutrition_plan_id, week_number, day_number, day_type, notes)
VALUES ('09c34039-9a53-5e34-85f4-e9c67d21282e', 'aaaaaaaa-0000-4000-8000-000000000001', 1, 7, 'STRENGTH', 'Día de fuerza: proteína alta, carbohidratos moderados.');
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, scheduled_time, optional)
VALUES ('ca3f7f1a-6a90-57b9-9714-3b56e31e14e2', '09c34039-9a53-5e34-85f4-e9c67d21282e', 'BREAKFAST', 'Desayuno', 0, TIME '08:00:00', FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('315223f5-b543-5d21-af1b-2621f862b06b', 'ca3f7f1a-6a90-57b9-9714-3b56e31e14e2', 'eggs', 150.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('de9c5419-b584-532a-9e28-5b9d16c3bf20', 'ca3f7f1a-6a90-57b9-9714-3b56e31e14e2', 'oats', 60.0, 1);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('e034030d-14c9-50f1-9b30-924a336ff590', 'ca3f7f1a-6a90-57b9-9714-3b56e31e14e2', 'yogurt', 125.0, 2);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, scheduled_time, optional)
VALUES ('a0ef83c8-10fb-5ccf-b666-4c9b42623c1a', '09c34039-9a53-5e34-85f4-e9c67d21282e', 'MID_MORNING', 'Media mañana', 1, TIME '11:00:00', FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('adda4bb2-e518-5128-b08f-1e15062cd456', 'a0ef83c8-10fb-5ccf-b666-4c9b42623c1a', 'whey-protein', 30.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('12d43d5c-2986-578b-864c-5200fbb525fb', 'a0ef83c8-10fb-5ccf-b666-4c9b42623c1a', 'banana', 100.0, 1);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, scheduled_time, optional)
VALUES ('78977827-2cb5-54dd-9569-888f1f65aaf1', '09c34039-9a53-5e34-85f4-e9c67d21282e', 'LUNCH', 'Comida', 2, TIME '14:00:00', FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('27381bd6-5d00-59f0-bff0-b08306e1a58c', '78977827-2cb5-54dd-9569-888f1f65aaf1', 'chicken', 150.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('af35f551-23ff-5c56-a28f-39b43368f564', '78977827-2cb5-54dd-9569-888f1f65aaf1', 'rice', 250.0, 1);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('eee38d9f-b346-5b90-aba4-960dc99641e0', '78977827-2cb5-54dd-9569-888f1f65aaf1', 'vegetables', 200.0, 2);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, scheduled_time, optional)
VALUES ('bf75b067-b28b-56a7-8d9a-cf6f8f98c238', '09c34039-9a53-5e34-85f4-e9c67d21282e', 'DINNER', 'Cena', 3, TIME '21:00:00', FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('edcdbcc1-8e3a-5f2b-a52d-1220b000ec53', 'bf75b067-b28b-56a7-8d9a-cf6f8f98c238', 'turkey', 100.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('7d0d8a51-8db7-51c5-9863-8d612550ac2c', 'bf75b067-b28b-56a7-8d9a-cf6f8f98c238', 'potato', 150.0, 1);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, amount, sort_order)
VALUES ('2cea1508-234d-560d-8f02-11d735beae62', 'bf75b067-b28b-56a7-8d9a-cf6f8f98c238', 'vegetables', 150.0, 2);
