import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Badge } from '../components/Badge';
import { Card } from '../components/Card';
import { EmptyState } from '../components/EmptyState';
import { ErrorState } from '../components/ErrorState';
import { Icon } from '../components/Icon';
import { LoadingState } from '../components/LoadingState';
import { MacroRing } from '../components/MacroRing';
import { WaterTracker } from '../components/WaterTracker';
import {
  getDayConsumption,
  getNutritionDay,
  type DayConsumption,
  type KeyNutrients,
  type NutritionDay,
  type NutritionMeal,
} from '../api/nutrition';
import { MealLogPanel } from './nutrition/MealLogPanel';
import { ProgressBar } from './dashboard/ProgressBar';
import { formatShortDate } from './dateLabel';
import styles from './NutritionPage.module.css';

/**
 * Nutrition page (FOR-33/34, built out to the mockup by FOR-54):
 * `docs/4-nutricion.png` — day-type selector, the daily meal plan, a macro
 * summary and the FOR-34 running-day guidance, reading only from the
 * `GET /api/v1/nutrition/days/{type}` read model (ADR-006 — no calculations
 * here, ADR-001).
 *
 * <p>This comment used to list four things the mockup showed and the API could
 * not back. Three of them it could, and two of those it always could:
 * <ul>
 *   <li><b>Per-meal macros and kcal</b> — said to be unreturned. They have been
 *       returned since FOR-105; {@code api/nutrition.ts} simply never declared
 *       the field, so the page drew invented chips beside real food.
 *   <li><b>"Objetivo vs actual"</b> — said to have no "actual". The day's own
 *       total has been returned since FOR-105 too, and what was actually EATEN
 *       arrived with the meal log. Both are shown now, in the two places they
 *       belong: the plan card compares the plan to its target, and the log
 *       compares the day to it.
 *   <li><b>Meal logging and key nutrients</b> — said to be modeled nowhere.
 *       Logging has existed since FOR-127 and key nutrients since FOR-134; what
 *       was missing was a screen, which {@link MealLogPanel} now is.
 * </ul>
 *
 * <p>What is still genuinely absent, and stays absent rather than invented:
 * <ul>
 *   <li>A TARGET for fibre, sugars, sodium or saturated fat. Nothing sets one,
 *       so the key-nutrient card shows figures without bars.
 *   <li>Meal photographs — no image data on any endpoint.
 *   <li>A date-parameterised plan. The log is per-date; the plan side can only
 *       be asked by day KIND, so the header's date navigator stays decorative.
 * </ul>
 *
 * <p>Day-type selection: the API takes an explicit `type` path segment
 * (`running`/`strength`/`rest`), so the selector below re-fetches on change.
 * There is no "which type is today" resolution in the backend (the FOR-51
 * dashboard widget and the pre-FOR-54 version of this page both hardcoded
 * `running`); this page now defaults to `running` and lets the user switch.
 */
type DayType = 'running' | 'strength' | 'rest';

type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'empty' }
  | { readonly status: 'ready'; readonly day: NutritionDay };

const DAY_TYPES: ReadonlyArray<{ readonly key: DayType; readonly label: string }> = [
  { key: 'running', label: 'Carrera' },
  { key: 'strength', label: 'Fuerza' },
  { key: 'rest', label: 'Descanso' },
];

/**
 * The FOR-164 placeholders are gone, and two of the three never needed to exist.
 *
 * <p>They were three invented numbers standing in for endpoints that "did not
 * exist yet": consumed calories, per-meal macros, and key nutrients. Two of them
 * were arriving from the API the whole time — the day and each of its meals have
 * carried computed totals since FOR-105, and `api/nutrition.ts` simply never
 * declared the fields, so the page drew fabrications beside real food for want of
 * a type. The third, key nutrients, has been persisted since FOR-134 and became
 * readable when the meal log got a screen.
 *
 * <p>What is genuinely not modeled is a TARGET for fibre, sugars, sodium or
 * saturated fat. Nothing anywhere sets one, so none is shown: a real figure with
 * no bar beside it says less than a bar, and says only true things.
 */

/** Static date label for the visual-only navigator (no date-parameterised API). */
const TODAY_LABEL = formatShortDate(new Date());

/**
 * Today, for the meal log.
 *
 * <p>The consumption endpoint IS date-parameterised, unlike the plan-day one above whose navigator
 * is still decorative. Wiring the navigator to move both is a bigger change than this screen: the
 * plan side would need a way to ask "which kind of day was the fourth of August", which it has no
 * endpoint for.
 */
const TODAY_ISO = new Date().toISOString().slice(0, 10);

export function NutritionPage() {
  const [dayType, setDayType] = useState<DayType>('running');
  const [retryToken, setRetryToken] = useState(0);
  const [state, setState] = useState<State>({ status: 'loading' });

  useEffect(() => {
    let active = true;
    setState({ status: 'loading' });
    getNutritionDay(dayType)
      .then((day) => {
        if (active) {
          setState(day.meals.length === 0 ? { status: 'empty' } : { status: 'ready', day });
        }
      })
      .catch(() => {
        if (active) {
          setState({ status: 'error' });
        }
      });
    return () => {
      active = false;
    };
  }, [dayType, retryToken]);

  // Fetched here rather than inside MealLogPanel, because two things on this page read it: the log
  // itself and the key-nutrient card. One request, one answer — two would be free to disagree by a
  // second.
  const [consumption, setConsumption] = useState<DayConsumption | undefined>(undefined);
  const reloadConsumption = useCallback(() => {
    getDayConsumption(TODAY_ISO)
      .then(setConsumption)
      .catch(() => setConsumption(undefined));
  }, []);

  useEffect(reloadConsumption, [reloadConsumption]);

  return (
    <div className={styles.wrapper}>
      <header className={styles.header}>
        <div className={styles.titles}>
          <h1 className={styles.title}>Nutrición</h1>
          <p className={styles.subtitle}>Alimenta tu cuerpo, alcanza tus objetivos.</p>
        </div>
        {/* Date navigator — visual only (no date-parameterised nutrition API). */}
        <div className={styles.dateNav} aria-hidden="true">
          <span className={styles.dateArrow}>
            <Icon name="chevron" size={16} className={styles.dateArrowPrev} />
          </span>
          <span className={styles.dateLabel}>{TODAY_LABEL}</span>
          <span className={styles.dateArrow}>
            <Icon name="chevron" size={16} />
          </span>
        </div>
      </header>

      {/* What was actually eaten, beside what the plan asked for. The endpoints behind it have
          existed since FOR-127 with no screen calling them. */}
      <MealLogPanel date={TODAY_ISO} day={consumption} onLogged={reloadConsumption} />

      <DayTypeSelector value={dayType} onChange={setDayType} />

      {/* V53/V54: what this page shows is a day of the plan being followed, and
          until now there was no way to reach the plan itself from here. */}
      <Link className={styles.plansLink} to="/app/nutrition/plans">
        Editar mis planes
      </Link>

      {renderContent(state, dayType, () => setRetryToken((token) => token + 1), consumption)}
    </div>
  );
}

function DayTypeSelector({
  value,
  onChange,
}: {
  readonly value: DayType;
  readonly onChange: (type: DayType) => void;
}) {
  return (
    <div className={styles.selector} role="radiogroup" aria-label="Tipo de día">
      {DAY_TYPES.map((option) => (
        <button
          key={option.key}
          type="button"
          role="radio"
          aria-checked={value === option.key}
          className={
            value === option.key
              ? `${styles.selectorButton} ${styles.selectorActive}`
              : styles.selectorButton
          }
          onClick={() => onChange(option.key)}
        >
          {option.label}
        </button>
      ))}
    </div>
  );
}

function renderContent(
  state: State,
  dayType: DayType,
  retry: () => void,
  consumption: DayConsumption | undefined,
) {
  if (state.status === 'loading') {
    return <LoadingState message="Cargando tu día de nutrición…" />;
  }

  if (state.status === 'error') {
    return (
      <ErrorState
        message="No se pudo cargar tu día de nutrición. Inténtalo de nuevo más tarde."
        onRetry={retry}
      />
    );
  }

  if (state.status === 'empty') {
    return <EmptyState title="No hay un plan de comidas para este tipo de día." />;
  }

  const { day } = state;
  const target = day.targets.calories;
  // What the plan's own meals come to, not what was eaten — that is the meal log's business, and
  // showing it twice on one page would be one number with two homes.
  const planned = day.totals.calories;
  const gap = target - planned;

  return (
    <>
      <section className={styles.summary} aria-label="Resumen de macronutrientes">
        <Card title="Calorías del plan" headingLevel={2}>
          {/* Both figures real: what the day aims for, and what its meals add up to. They can
              disagree, which is the whole reason the model keeps them apart. */}
          <p className={styles.calories}>
            <span className={styles.caloriesValue}>{planned}</span>
            <span className={styles.caloriesUnit}>
              {' / '}
              <span className={styles.caloriesTarget}>{target}</span> kcal
            </span>
          </p>
          <ProgressBar value={planned} max={target} label="Calorías que suma el plan" />
          <p className={styles.caloriesNote}>
            {gap > 0
              ? `${gap} kcal por debajo del objetivo`
              : gap < 0
                ? `${-gap} kcal por encima del objetivo`
                : 'Justo en el objetivo'}
          </p>
        </Card>
        <Card title="Distribución de macros" headingLevel={2}>
          <MacroRing
            proteinG={day.targets.proteinG}
            carbsG={day.targets.carbsG}
            fatG={day.targets.fatG}
          />
        </Card>
        <WaterTracker headingLevel={2} />
      </section>

      {dayType === 'running' && <RunningGuidance meals={day.meals} />}

      <div className={styles.mainSide}>
        <Card title="Comidas del día" headingLevel={2}>
          <ol className={styles.meals}>
            {day.meals.map((meal) => (
              <li key={`${meal.mealType}-${meal.preferredTime}`}>
                <MealCard meal={meal} />
              </li>
            ))}
          </ol>
        </Card>

        <KeyNutrientsCard nutrients={consumption?.keyNutrients} />
      </div>

      <RecoveryRecommendation meals={day.meals} />
    </>
  );
}

function MealCard({ meal }: { readonly meal: NutritionMeal }) {
  return (
    <Card>
      <div className={styles.mealHeader}>
        {/* Photo placeholder — no meal image data on the API. */}
        <span className={styles.mealPhoto} aria-hidden="true">
          <Icon name="nutrition" size={20} />
        </span>
        <div className={styles.mealHeaderText}>
          <p className={styles.mealTime}>{meal.preferredTime}</p>
          {/* FOR-112: <h3> under the <h2> "Comidas del día". */}
          <h3 className={styles.mealName}>{meal.name}</h3>
        </div>
        {meal.optional && <Badge tone="warning">Opcional</Badge>}
      </div>
      <ul className={styles.items}>
        {meal.items.map((item) => (
          <li key={item.food} className={styles.item}>
            <span className={styles.food}>{item.food}</span>
            <span className={styles.quantity}>{item.quantityG} g</span>
          </li>
        ))}
      </ul>
      {/* Real, and always were: the API has returned per-meal totals since FOR-105. */}
      <p className={styles.mealMacros}>
        <span>P {meal.totals.proteinG} g</span>
        <span>C {meal.totals.carbsG} g</span>
        <span>G {meal.totals.fatG} g</span>
        <span className={styles.mealKcal}>{meal.totals.calories} kcal</span>
      </p>
    </Card>
  );
}

/**
 * "Nutrientes clave" — fibre, sugars, sodium and saturated fat actually consumed
 * today (FOR-134).
 *
 * <p>No progress bars, and their absence is the honest part. Nothing in the app
 * sets a target for any of these four: the profile has none, the plan has none,
 * and the source documents never asked for one. A bar needs a maximum, and the
 * only maximum available would have been invented — which is what the number
 * beside it used to be.
 *
 * <p><b>A null is not a zero.</b> A day's total for one of these is null when any
 * single thing eaten has no figure for it, because summing the rest would report a
 * number lower than the truth and look like a measurement. Saying so is more use
 * than a dash: it tells you the gap is in the catalog, not in what you ate.
 */
function KeyNutrientsCard({ nutrients }: { readonly nutrients: KeyNutrients | undefined }) {
  const rows = [
    { label: 'Fibra', value: nutrients?.fiberG ?? null, unit: 'g' },
    { label: 'Azúcares', value: nutrients?.sugarsG ?? null, unit: 'g' },
    { label: 'Sodio', value: nutrients?.sodiumMg ?? null, unit: 'mg' },
    { label: 'Grasas saturadas', value: nutrients?.saturatedFatG ?? null, unit: 'g' },
  ];
  return (
    <Card title="Nutrientes clave" headingLevel={2}>
      <ul className={styles.nutrients}>
        {rows.map((row) => (
          <li key={row.label} className={styles.nutrient}>
            <div className={styles.nutrientHead}>
              <span>{row.label}</span>
              <span className={styles.nutrientValue}>
                {row.value === null ? 'Sin datos' : `${row.value} ${row.unit}`}
              </span>
            </div>
          </li>
        ))}
      </ul>
      <p className={styles.nutrientsNote}>
        Lo que llevas hoy. Un &laquo;sin datos&raquo; significa que algo de lo registrado no tiene
        ese valor en el catálogo, no que sea cero.
      </p>
    </Card>
  );
}

/**
 * Builds the running-day flow labels (FOR-34): the meals in preferred-time
 * order (as the API already returns them) with a "Correr" marker inserted
 * after the pre-run snack, matching the spec's "Breakfast → Lunch → Pre-run
 * snack → Run → Light recovery → Light dinner" narrative. The marker is a
 * purely presentational label, not derived nutrition data.
 */
function runningFlowLabels(meals: readonly NutritionMeal[]): string[] {
  const labels: string[] = [];
  meals.forEach((meal) => {
    labels.push(meal.name);
    if (meal.mealType === 'PRE_WORKOUT') {
      labels.push('Correr');
    }
  });
  return labels;
}

function RunningGuidance({ meals }: { readonly meals: readonly NutritionMeal[] }) {
  return (
    <Card title="Estrategia de día de carrera" headingLevel={2}>
      <p className={styles.explanation}>
        Los carbohidratos se concentran temprano; la cena es más ligera tras correr por la noche. La
        recuperación post-carrera es opcional: sáltala si ya has alcanzado tu proteína diaria.
      </p>
      <ol className={styles.flow} aria-label="Flujo de comidas del día de carrera">
        {runningFlowLabels(meals).map((label, index) => (
          <li key={`${label}-${index}`} className={styles.flowStep}>
            {index > 0 && (
              <span className={styles.flowArrow} aria-hidden="true">
                →
              </span>
            )}
            {label}
          </li>
        ))}
      </ol>
    </Card>
  );
}

function RecoveryRecommendation({ meals }: { readonly meals: readonly NutritionMeal[] }) {
  const recovery = meals.find((meal) => meal.optional);
  if (!recovery) {
    return null;
  }
  const items = recovery.items.map((item) => `${item.food} (${item.quantityG} g)`).join(', ');
  return (
    <Card title="Recomendación de recuperación" headingLevel={2}>
      <p className={styles.explanation}>
        {recovery.name} · {recovery.preferredTime}: {items}. Es opcional — sáltala si ya has
        alcanzado tu proteína diaria.
      </p>
    </Card>
  );
}
