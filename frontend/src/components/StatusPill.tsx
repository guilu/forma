import { Badge, type BadgeTone } from './Badge';

export type StatusKind =
  'severity' | 'connection' | 'plazo' | 'source' | 'training' | 'goalStatus' | 'muscleLoad';

interface StatusMapping {
  readonly tone: BadgeTone;
  readonly label: string;
}

/**
 * Recommendation severity (backend `RecommendationSeverity`, FOR-41): INFO is a
 * neutral observation, WARNING deserves attention without alarm, ACTION is a
 * concrete suggested adjustment. ui-guidelines.md says the accent should
 * "highlight actions" — so ACTION maps to accent rather than danger; nothing
 * here should read as alarming (docs/ui-guidelines.md interaction style).
 */
const SEVERITY_TONES: Record<string, StatusMapping> = {
  INFO: { tone: 'neutral', label: 'Info' },
  WARNING: { tone: 'warning', label: 'Atención' },
  ACTION: { tone: 'accent', label: 'Acción' },
};

/** Integration connection status (e.g. Settings/integrations screens). */
const CONNECTION_TONES: Record<string, StatusMapping> = {
  Conectado: { tone: 'accent', label: 'Conectado' },
  'No conectado': { tone: 'neutral', label: 'No conectado' },
};

/** Plazo (term) tags — purely categorical, no severity implied. */
const PLAZO_TONES: Record<string, StatusMapping> = {
  'Corto plazo': { tone: 'neutral', label: 'Corto plazo' },
  'Medio plazo': { tone: 'neutral', label: 'Medio plazo' },
  'Largo plazo': { tone: 'neutral', label: 'Largo plazo' },
};

/**
 * Measurement origin (FOR-52): distinguishes manually entered body
 * measurements from ones imported from an external provider (`BodyMeasurement
 * .source`, FOR-15). `UNKNOWN` is not a backend value — callers map a
 * falsy/unrecognized `source` to it so a missing origin always renders a
 * clearly-labelled neutral badge instead of a blank one (spec FOR-52 edge
 * case: "Import source unknown/missing → default to a neutral source
 * label").
 */
const SOURCE_TONES: Record<string, StatusMapping> = {
  MANUAL: { tone: 'neutral', label: 'Manual' },
  WITHINGS: { tone: 'accent', label: 'Withings' },
  UNKNOWN: { tone: 'neutral', label: 'Origen desconocido' },
};

/**
 * Training session completion status (`SessionStatus`, FOR-27): `PLANNED` is
 * the neutral default, `COMPLETED` reads as a positive/accent state,
 * `SKIPPED` gets the warning tone (not danger — skipping a session is not an
 * error, mirrors ui-guidelines.md "no guilt language").
 */
const TRAINING_TONES: Record<string, StatusMapping> = {
  PLANNED: { tone: 'neutral', label: 'Planificado' },
  COMPLETED: { tone: 'accent', label: 'Completado' },
  SKIPPED: { tone: 'warning', label: 'Saltado' },
};

/**
 * Goal lifecycle status (backend `GoalStatus`, FOR-125/FOR-122): `ACTIVE` is
 * the neutral default (being tracked), `ACHIEVED` reads as a positive/accent
 * state (mirrors `TRAINING_TONES.COMPLETED`), `ARCHIVED` is neutral — the
 * user gave up on it without it being an error or a failure state.
 */
const GOAL_STATUS_TONES: Record<string, StatusMapping> = {
  ACTIVE: { tone: 'neutral', label: 'Activo' },
  ACHIEVED: { tone: 'accent', label: 'Conseguido' },
  ARCHIVED: { tone: 'neutral', label: 'Archivado' },
};

/**
 * Muscle load level (backend `MuscleLoad`, FOR-136): `HIGH` gets the accent
 * tone (mirrors `TRAINING_TONES.COMPLETED` — it highlights the muscles a
 * strength session emphasizes most), `MEDIUM`/`LOW` stay neutral since they
 * are not warnings or errors, just a lower emphasis level.
 */
const MUSCLE_LOAD_TONES: Record<string, StatusMapping> = {
  HIGH: { tone: 'accent', label: 'Carga alta' },
  MEDIUM: { tone: 'neutral', label: 'Carga media' },
  LOW: { tone: 'neutral', label: 'Carga baja' },
};

const TABLES: Record<StatusKind, Record<string, StatusMapping>> = {
  severity: SEVERITY_TONES,
  connection: CONNECTION_TONES,
  plazo: PLAZO_TONES,
  source: SOURCE_TONES,
  training: TRAINING_TONES,
  goalStatus: GOAL_STATUS_TONES,
  muscleLoad: MUSCLE_LOAD_TONES,
};

interface StatusPillProps {
  readonly kind: StatusKind;
  readonly value: string;
}

/**
 * Domain-aware {@link Badge} wrapper (FOR-50) for the three tag families every
 * screen needs: severity, connection and plazo. This is presentation-only
 * mapping (label + tone), not a business rule — the domain values themselves
 * come from the backend (ADR-006: frontend consumes read models). An
 * unrecognized value always falls back to a neutral badge showing the raw
 * value, so a status can never render broken/unstyled.
 */
export function StatusPill({ kind, value }: StatusPillProps) {
  const resolved = TABLES[kind][value] ?? { tone: 'neutral', label: value };
  return <Badge tone={resolved.tone}>{resolved.label}</Badge>;
}
