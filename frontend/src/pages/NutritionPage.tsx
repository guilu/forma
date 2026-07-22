import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Badge } from '../components/Badge';
import { Card } from '../components/Card';
import { EmptyState } from '../components/EmptyState';
import { ErrorState } from '../components/ErrorState';
import { Icon } from '../components/Icon';
import { LoadingState } from '../components/LoadingState';
import { MacroRing } from '../components/MacroRing';
import { WaterTracker } from '../components/WaterTracker';
import { getNutritionDay, type NutritionDay, type NutritionMeal } from '../api/nutrition';
import { getShoppingList } from '../api/shopping';
import { ProgressBar } from './dashboard/ProgressBar';
import styles from './NutritionPage.module.css';

/**
 * Nutrition page (FOR-33/34, built out to the mockup by FOR-54):
 * `docs/4-nutricion.png` — day-type selector, the daily meal plan, a macro
 * summary and the FOR-34 running-day guidance, reading only from the
 * `GET /api/v1/nutrition/days/{type}` read model (ADR-006 — no calculations
 * here, ADR-001).
 *
 * <p>Mockup elements not backed by the API today (documented gap, not
 * invented — AGENTS.md "repository state has priority"):
 * <ul>
 *   <li>Per-meal macro chips (P/C/G) and kcal — {@link NutritionMeal} only
 *       carries {@code items[].food}/{@code quantityG} (a resolved food name
 *       plus grams); no per-item or per-meal macro/kcal figures are returned,
 *       so meal cards show name, time and items only.
 *   <li>"Objetivo vs actual" — the API returns only the day's *targets*; there
 *       is no consumption-logging endpoint, so there is no "actual" to
 *       compare against (same gap as the FOR-51 {@code NutritionWidget}).
 *       The macro ring below therefore shows the target distribution only.
 *   <li>Meal logging ("Registrar comida" + done checkmarks), water/hydration
 *       tracking ("Añadir agua") and "Nutrientes clave" (fibra/azúcares/sodio/
 *       grasas saturadas) — none of these are modeled or persisted anywhere in
 *       the backend, so they are omitted entirely rather than shown inactive.
 *   <li>Meal tap → detail view — every field the API returns for a meal
 *       (name, time, items) is already shown on its card, so a separate detail
 *       screen would repeat the same content; omitted.
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
 * FOR-164 hybrid placeholders (`docs/4-nutricion.png`). None is backed: there
 * is no consumption-logging endpoint (so no "consumed" calories, no per-meal
 * macro/kcal figures), and fibre/sugar/sodium/saturated-fat are not modeled at
 * all. Kept isolated and clearly labelled so they're obvious and easy to remove
 * once those endpoints exist. Per the FOR-164 decision these are *numeric*
 * placeholders only — no fabricated user behaviour (no "meal completed" checks,
 * no non-functional "Registrar comida" action). Macro/calorie TARGETS and the
 * meals themselves are real, read straight from the API.
 */
const PLACEHOLDER = {
  /** Fraction of the calorie target shown as "consumed" (visual only). */
  consumedRatio: 0.917,
  /** Per-meal macro/kcal chips, cycled by meal index. */
  mealMacros: [
    { p: 32, c: 58, g: 14, kcal: 480 },
    { p: 18, c: 25, g: 10, kcal: 240 },
    { p: 48, c: 62, g: 16, kcal: 620 },
    { p: 30, c: 35, g: 8, kcal: 320 },
    { p: 34, c: 56, g: 20, kcal: 450 },
  ],
  keyNutrients: [
    { label: 'Fibra', current: 28, target: 30, unit: 'g' },
    { label: 'Azúcares', current: 38, target: 50, unit: 'g' },
    { label: 'Sodio', current: 1620, target: 2300, unit: 'mg' },
    { label: 'Grasas saturadas', current: 18, target: 23, unit: 'g' },
  ],
} as const;

/** Static date label for the visual-only navigator (no date-parameterised API). */
const TODAY_LABEL = new Intl.DateTimeFormat('es-ES', {
  weekday: 'long',
  day: 'numeric',
  month: 'long',
  year: 'numeric',
}).format(new Date());

function capitalize(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1);
}

export function NutritionPage() {
  const [dayType, setDayType] = useState<DayType>('running');
  const [retryToken, setRetryToken] = useState(0);
  const [state, setState] = useState<State>({ status: 'loading' });
  const [shoppingCount, setShoppingCount] = useState<number | undefined>(undefined);

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

  useEffect(() => {
    let active = true;
    getShoppingList()
      .then((list) => {
        if (active) {
          setShoppingCount(list.items.length);
        }
      })
      .catch(() => {
        // The shopping shortcut degrades to a plain link when the count can't load;
        // it must never block the nutrition page itself.
      });
    return () => {
      active = false;
    };
  }, []);

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
          <span className={styles.dateLabel}>{capitalize(TODAY_LABEL)}</span>
          <span className={styles.dateArrow}>
            <Icon name="chevron" size={16} />
          </span>
        </div>
      </header>

      <DayTypeSelector value={dayType} onChange={setDayType} />

      {renderContent(state, dayType, () => setRetryToken((token) => token + 1))}

      <ShoppingShortcut count={shoppingCount} />
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

function renderContent(state: State, dayType: DayType, retry: () => void) {
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
  const consumed = Math.round(target * PLACEHOLDER.consumedRatio);
  const restantes = Math.max(target - consumed, 0);

  return (
    <>
      <section className={styles.summary} aria-label="Resumen de macronutrientes">
        <Card title="Calorías" headingLevel={2}>
          {/* Consumed is placeholder (no logging endpoint); the target is real.
              Target stays a standalone node so it reads cleanly. */}
          <p className={styles.calories}>
            <span className={styles.caloriesValue}>{consumed}</span>
            <span className={styles.caloriesUnit}>
              {' / '}
              <span className={styles.caloriesTarget}>{target}</span> kcal
            </span>
          </p>
          <ProgressBar value={consumed} max={target} label="Calorías consumidas" />
          <p className={styles.caloriesNote}>{restantes} kcal restantes</p>
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
            {day.meals.map((meal, index) => (
              <li key={`${meal.mealType}-${meal.preferredTime}`}>
                <MealCard meal={meal} index={index} />
              </li>
            ))}
          </ol>
        </Card>

        <KeyNutrientsCard />
      </div>

      <RecoveryRecommendation meals={day.meals} />
    </>
  );
}

function MealCard({ meal, index }: { readonly meal: NutritionMeal; readonly index: number }) {
  const macros = PLACEHOLDER.mealMacros[index % PLACEHOLDER.mealMacros.length];
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
      {/* Per-meal macros + kcal are placeholder (not returned by the API). */}
      <p className={styles.mealMacros}>
        <span>P {macros.p} g</span>
        <span>C {macros.c} g</span>
        <span>G {macros.g} g</span>
        <span className={styles.mealKcal}>{macros.kcal} kcal</span>
      </p>
    </Card>
  );
}

/**
 * "Nutrientes clave" card — entirely placeholder (fibre/sugar/sodium/saturated
 * fat are not modeled on the API). See {@link PLACEHOLDER}.
 */
function KeyNutrientsCard() {
  return (
    <Card title="Nutrientes clave" headingLevel={2}>
      <ul className={styles.nutrients}>
        {PLACEHOLDER.keyNutrients.map((n) => (
          <li key={n.label} className={styles.nutrient}>
            <div className={styles.nutrientHead}>
              <span>{n.label}</span>
              <span className={styles.nutrientValue}>
                {n.current} / {n.target} {n.unit}
              </span>
            </div>
            <ProgressBar value={n.current} max={n.target} label={n.label} />
          </li>
        ))}
      </ul>
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

function ShoppingShortcut({ count }: { readonly count: number | undefined }) {
  return (
    <Card title="Lista de la compra" headingLevel={2}>
      <div className={styles.shoppingRow}>
        <p className={styles.shoppingCount}>
          {count === undefined
            ? 'Generada según tu plan nutricional'
            : `${count} producto${count === 1 ? '' : 's'}`}
        </p>
        <Link className={styles.shoppingLink} to="/lista-compra">
          Ver lista de la compra
        </Link>
      </div>
    </Card>
  );
}
