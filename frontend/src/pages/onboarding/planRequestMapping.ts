/**
 * Maps the onboarding wizard's Spanish, human-readable answers onto the
 * machine vocabulary {@code POST /api/v1/plan-requests} requires (ADR-015
 * decision 6, slice 3).
 *
 * <p><b>The frontend owns this mapping.</b> The backend has an equivalent
 * table (`dev.diegobarrioh.forma.domain.SpanishWeekdayLabels` for weekdays,
 * and `TrainingEquipment`'s own javadoc for equipment), but neither is
 * exposed through an endpoint the wizard could call — `PlanRequestCreateRequest`
 * requires `Set<DayOfWeek>`/`Set<TrainingEquipment>` already resolved, with
 * no translation step in between. So the two tables below are intentionally
 * the frontend half of a mapping that exists on both sides of the HTTP
 * boundary, kept in step by inspection rather than by a shared endpoint —
 * the same posture ADR-015 decision 6 states for every Spanish-label
 * boundary in this feature ("the Spanish UI label is a label; it never
 * crosses the port").
 */
import type {
  CreatePlanRequestInput,
  CuisineStyle,
  DayOfWeekName,
  DietPattern,
  TrainingEquipment,
} from '../../api/planRequests';
import type { OnboardingAnswers } from './onboardingStorage';

/** Mirrors the backend's `SpanishWeekdayLabels` table verbatim. */
const WEEKDAY_LABELS: Readonly<Record<string, DayOfWeekName>> = {
  Lunes: 'MONDAY',
  Martes: 'TUESDAY',
  Miércoles: 'WEDNESDAY',
  Jueves: 'THURSDAY',
  Viernes: 'FRIDAY',
  Sábado: 'SATURDAY',
  Domingo: 'SUNDAY',
};

/**
 * Resolves one of {@code TrainingAvailabilityStep}'s seven Spanish day
 * labels to its {@link DayOfWeekName}.
 *
 * @throws {@link Error} if the label is not one the wizard offers — never
 *   silently dropped, mirroring the backend's own `SpanishWeekdayLabels.
 *   fromLabel` javadoc: a label the wizard did not send is a bug worth
 *   surfacing, not a day quietly missing from someone's training week.
 */
export function mapWeekdayLabelToDayOfWeek(label: string): DayOfWeekName {
  const day = WEEKDAY_LABELS[label];
  if (!day) {
    throw new Error(`Unknown weekday label: ${label}`);
  }
  return day;
}

/** Mirrors the backend's `TrainingEquipment` javadoc table verbatim. */
const EQUIPMENT_LABELS: Readonly<Record<string, TrainingEquipment>> = {
  'Sin equipamiento (peso corporal)': 'BODYWEIGHT',
  Mancuernas: 'DUMBBELLS',
  'Barra y discos': 'BARBELL',
  'Bandas elásticas': 'BANDS',
  'Máquinas de gimnasio': 'MACHINES',
  'Cinta o bicicleta estática': 'CARDIO_MACHINE',
};

/**
 * Resolves one of {@code EquipmentStep}'s six Spanish sentences to its
 * {@link TrainingEquipment}.
 *
 * @throws {@link Error} if the label is not one the wizard offers.
 */
export function mapEquipmentLabelToTrainingEquipment(label: string): TrainingEquipment {
  const equipment = EQUIPMENT_LABELS[label];
  if (!equipment) {
    throw new Error(`Unknown equipment label: ${label}`);
  }
  return equipment;
}

/**
 * Maps {@code NutritionBasicsStep}'s "Preferencia alimentaria" answer to
 * {@link DietPattern}. `''` ("Prefiero no decirlo") and `'OTHER'` ("Otra")
 * both resolve to `UNSPECIFIED` — {@link DietPattern} has no fifth,
 * catch-all value, and "otra" is not itself an exclusion rule FORMA knows
 * how to honor.
 */
export function mapDietPatternAnswer(preference: string): DietPattern {
  switch (preference) {
    case 'OMNIVORE':
    case 'VEGETARIAN':
    case 'VEGAN':
    case 'GLUTEN_FREE':
      return preference;
    default:
      return 'UNSPECIFIED';
  }
}

/**
 * Maps {@code NutritionBasicsStep}'s "Estilo de cocina" answer to {@link
 * CuisineStyle}. `''` ("Prefiero no decirlo") resolves to `UNSPECIFIED`,
 * itself a real, stored answer (ADR-015 decision 6).
 */
export function mapCuisineStyleAnswer(cuisineStyle: string): CuisineStyle {
  return cuisineStyle === 'ESPANOLA' || cuisineStyle === 'MEDITERRANEA'
    ? cuisineStyle
    : 'UNSPECIFIED';
}

/**
 * Builds the {@code POST /api/v1/plan-requests} body from the wizard's
 * complete local draft, or {@code undefined} when the one field with no
 * honest default — {@link DirectionStep}'s answer — was never chosen.
 * Callers must not submit when this returns {@code undefined}; ADR-015's own
 * amendment is the reason there is no inferred fallback here.
 *
 * <p>An empty {@code training.days}/{@code equipment.items} is treated as
 * "nobody said" (an omitted, {@code undefined} field) rather than as a
 * deliberate "zero days available"/"no equipment" answer: the wizard's
 * checkboxes have no separate way to record "I looked and chose none", so
 * collapsing "skipped this step" and "answered with nothing" into the same
 * empty array cannot be told apart here — and ADR-015 decision 6 treats
 * `NULL`/omitted as the safer of the two readings.
 */
export function buildPlanRequestInput(
  answers: OnboardingAnswers,
): CreatePlanRequestInput | undefined {
  const direction = answers.direction.selected;
  if (!direction) {
    return undefined;
  }

  const weekdays = answers.training.days.map(mapWeekdayLabelToDayOfWeek);
  const equipment = answers.equipment.items.map(mapEquipmentLabelToTrainingEquipment);

  return {
    direction,
    trainingDaysPerWeek: weekdays.length,
    trainingWeekdays: weekdays.length > 0 ? weekdays : undefined,
    equipment: equipment.length > 0 ? equipment : undefined,
    mealsPerDay: answers.nutrition.mealsPerDay,
    dietPattern: mapDietPatternAnswer(answers.nutrition.preference),
    cuisineStyle: mapCuisineStyleAnswer(answers.nutrition.cuisineStyle),
  };
}
