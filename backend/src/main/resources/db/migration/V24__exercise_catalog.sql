-- Exercise catalog persistence: first consumer of ADR-011 data model v2 (FOR-172).
--
-- Additive on top of V23 — creates exercise_catalog (global reference data, VARCHAR(64) verbatim
-- ids reused from domain.ExerciseCatalog / domain.SessionType, per ADR-011 Decision 1/4) and its
-- child exercise_catalog_muscle (ADR-011 amendment A1 — preserves the ORDERED
-- Exercise.primaryMuscles() list that MuscleWorkedMapService aggregates by first-appearance; a
-- single scalar muscle_group column was rejected because it would lose that order/multiplicity).
--
-- Modality (STRENGTH | RUNNING today, CYCLING later) reuses one table with nullable
-- per-modality columns instead of JSONB (H2 has no JSONB, h2database#1869) or table-per-modality
-- (would break a single findById) — see ADR-011 "Modality extensibility proof".
--
-- Seed data is transcribed VERBATIM from domain.ExerciseCatalog (16 STRENGTH exercises) and
-- domain.SessionType (4 RUNNING session kinds, id = "running-" + lowercase(SessionType.name())
-- with underscores as hyphens, session_kind column = the exact SessionType.name() value). Columns
-- that do not apply to a modality are left SQL NULL, never fabricated (default_sets/reps and
-- default_distance_km/default_pace_min_per_km belong to prescription, FOR-175+; instructions is
-- STRENGTH-only authored ES cues). Coexists with the static ExerciseCatalog/RunningPlanGenerator —
-- no consumer is repointed in this migration.
--
-- exercise_catalog_muscle PKs are FIXED literal UUIDs (never RANDOM_UUID()/gen_random_uuid()): a
-- non-deterministic seed would break repeatable count/integrity assertions and is non-portable.
-- Parents are created and seeded before children (FK target + insert-order requirement).

CREATE TABLE exercise_catalog (
    id                       VARCHAR(64) PRIMARY KEY,
    name                     VARCHAR(200) NOT NULL,
    modality                 VARCHAR(32)  NOT NULL,
    movement_pattern         VARCHAR(32),
    equipment                VARCHAR(32),
    default_sets             INTEGER,
    default_reps             VARCHAR(16),
    default_distance_km      NUMERIC(5,2),
    default_pace_min_per_km  VARCHAR(8),
    session_kind             VARCHAR(32),
    instructions             TEXT,
    created_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_exercise_catalog_modality ON exercise_catalog (modality);

CREATE TABLE exercise_catalog_muscle (
    id           UUID PRIMARY KEY,
    exercise_id  VARCHAR(64) NOT NULL,
    muscle       VARCHAR(64) NOT NULL,
    ordinal      INTEGER     NOT NULL,
    CONSTRAINT fk_ecm_exercise FOREIGN KEY (exercise_id) REFERENCES exercise_catalog (id)
);
CREATE INDEX idx_exercise_catalog_muscle_exercise ON exercise_catalog_muscle (exercise_id);

-- 16 STRENGTH rows (verbatim from domain.ExerciseCatalog). default_sets/reps, RUNNING columns and
-- session_kind are NULL for STRENGTH.
INSERT INTO exercise_catalog (id, name, modality, movement_pattern, equipment, instructions) VALUES
  ('push-up', 'Flexiones', 'STRENGTH', 'PUSH', 'BODYWEIGHT',
   'Cuerpo recto, baja el pecho hasta cerca del suelo y empuja hacia arriba.'),
  ('dumbbell-shoulder-press', 'Press de hombro con mancuernas', 'STRENGTH', 'PUSH', 'DUMBBELL',
   'Sentado o de pie, empuja las mancuernas por encima de la cabeza sin arquear la espalda.'),
  ('bench-dip', 'Fondos en banco', 'STRENGTH', 'PUSH', 'BENCH',
   'Manos en el banco, baja los codos a 90 grados y empuja hacia arriba.'),
  ('dumbbell-bench-press', 'Press de banca con mancuernas', 'STRENGTH', 'PUSH', 'DUMBBELL',
   'Tumbado en el banco, empuja las mancuernas desde el pecho hasta extender los brazos.'),
  ('lateral-raise', 'Elevaciones laterales', 'STRENGTH', 'PUSH', 'DUMBBELL',
   'De pie, eleva las mancuernas hacia los lados hasta la altura del hombro, codos ligeramente flexionados.'),
  ('pull-up', 'Dominadas', 'STRENGTH', 'PULL', 'PULL_UP_BAR',
   'Cuelga de la barra y tira hasta que la barbilla supere la barra.'),
  ('dumbbell-row', 'Remo con mancuerna', 'STRENGTH', 'PULL', 'DUMBBELL',
   'Con la espalda plana, tira de la mancuerna hacia la cadera y baja controlado.'),
  ('band-face-pull', 'Face pull con banda', 'STRENGTH', 'PULL', 'BAND',
   'Tira de la banda hacia la cara separando las manos al final del movimiento.'),
  ('biceps-curl', 'Curl de bíceps', 'STRENGTH', 'PULL', 'DUMBBELL',
   'De pie, flexiona los codos elevando las mancuernas sin balancear el torso.'),
  ('rear-delt-fly', 'Pájaros posteriores', 'STRENGTH', 'PULL', 'DUMBBELL',
   'Inclinado hacia delante, abre los brazos con las mancuernas hasta la altura de los hombros.'),
  ('goblet-squat', 'Sentadilla goblet', 'STRENGTH', 'SQUAT', 'DUMBBELL',
   'Sujeta la mancuerna al pecho y baja en sentadilla manteniendo el torso erguido.'),
  ('dumbbell-rdl', 'Peso muerto rumano con mancuernas', 'STRENGTH', 'HINGE', 'DUMBBELL',
   'Bisagra de cadera bajando las mancuernas por delante de las piernas con la espalda recta.'),
  ('reverse-lunge', 'Zancada hacia atrás', 'STRENGTH', 'SQUAT', 'BODYWEIGHT',
   'Da un paso atrás y baja la rodilla trasera cerca del suelo; alterna piernas.'),
  ('calf-raise', 'Elevación de gemelos', 'STRENGTH', 'SQUAT', 'BODYWEIGHT',
   'De pie, elévate sobre la punta de los pies y baja controlado sin rebotar.'),
  ('plank', 'Plancha', 'STRENGTH', 'CORE', 'BODYWEIGHT',
   'Antebrazos y puntas de los pies, cuerpo recto, mantén sin hundir la cadera.'),
  ('dead-bug', 'Dead bug', 'STRENGTH', 'CORE', 'BODYWEIGHT',
   'Boca arriba, extiende brazo y pierna opuestos sin arquear la zona lumbar.');

-- exercise_catalog_muscle: ordered primaryMuscles() lists, ordinal 0-based, 31 rows total.
INSERT INTO exercise_catalog_muscle (id, exercise_id, muscle, ordinal) VALUES
  ('ec000001-0000-0000-0000-000000000001', 'push-up', 'pecho', 0),
  ('ec000001-0000-0000-0000-000000000002', 'push-up', 'tríceps', 1),
  ('ec000001-0000-0000-0000-000000000003', 'push-up', 'hombro anterior', 2),
  ('ec000002-0000-0000-0000-000000000001', 'dumbbell-shoulder-press', 'hombro', 0),
  ('ec000002-0000-0000-0000-000000000002', 'dumbbell-shoulder-press', 'tríceps', 1),
  ('ec000003-0000-0000-0000-000000000001', 'bench-dip', 'tríceps', 0),
  ('ec000003-0000-0000-0000-000000000002', 'bench-dip', 'pecho', 1),
  ('ec000004-0000-0000-0000-000000000001', 'dumbbell-bench-press', 'pecho', 0),
  ('ec000004-0000-0000-0000-000000000002', 'dumbbell-bench-press', 'tríceps', 1),
  ('ec000004-0000-0000-0000-000000000003', 'dumbbell-bench-press', 'hombro anterior', 2),
  ('ec000005-0000-0000-0000-000000000001', 'lateral-raise', 'hombro lateral', 0),
  ('ec000006-0000-0000-0000-000000000001', 'pull-up', 'dorsal', 0),
  ('ec000006-0000-0000-0000-000000000002', 'pull-up', 'bíceps', 1),
  ('ec000007-0000-0000-0000-000000000001', 'dumbbell-row', 'dorsal', 0),
  ('ec000007-0000-0000-0000-000000000002', 'dumbbell-row', 'romboides', 1),
  ('ec000007-0000-0000-0000-000000000003', 'dumbbell-row', 'bíceps', 2),
  ('ec000008-0000-0000-0000-000000000001', 'band-face-pull', 'deltoides posterior', 0),
  ('ec000008-0000-0000-0000-000000000002', 'band-face-pull', 'trapecio', 1),
  ('ec000009-0000-0000-0000-000000000001', 'biceps-curl', 'bíceps', 0),
  ('ec000010-0000-0000-0000-000000000001', 'rear-delt-fly', 'deltoides posterior', 0),
  ('ec000011-0000-0000-0000-000000000001', 'goblet-squat', 'cuádriceps', 0),
  ('ec000011-0000-0000-0000-000000000002', 'goblet-squat', 'glúteo', 1),
  ('ec000012-0000-0000-0000-000000000001', 'dumbbell-rdl', 'isquiotibiales', 0),
  ('ec000012-0000-0000-0000-000000000002', 'dumbbell-rdl', 'glúteo', 1),
  ('ec000013-0000-0000-0000-000000000001', 'reverse-lunge', 'cuádriceps', 0),
  ('ec000013-0000-0000-0000-000000000002', 'reverse-lunge', 'glúteo', 1),
  ('ec000014-0000-0000-0000-000000000001', 'calf-raise', 'gemelos', 0),
  ('ec000015-0000-0000-0000-000000000001', 'plank', 'core', 0),
  ('ec000015-0000-0000-0000-000000000002', 'plank', 'abdomen', 1),
  ('ec000016-0000-0000-0000-000000000001', 'dead-bug', 'core', 0),
  ('ec000016-0000-0000-0000-000000000002', 'dead-bug', 'abdomen', 1);

-- 4 RUNNING rows keyed by SessionType.name(); movement_pattern/equipment/default_*/instructions
-- NULL; zero exercise_catalog_muscle children.
INSERT INTO exercise_catalog (id, name, modality, session_kind) VALUES
  ('running-easy', 'Rodaje suave', 'RUNNING', 'EASY'),
  ('running-long-run', 'Tirada larga', 'RUNNING', 'LONG_RUN'),
  ('running-intervals', 'Series', 'RUNNING', 'INTERVALS'),
  ('running-recovery', 'Recuperación', 'RUNNING', 'RECOVERY');
