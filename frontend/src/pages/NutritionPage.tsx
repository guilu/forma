import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Badge } from '../components/Badge';
import { Card } from '../components/Card';
import { EmptyState } from '../components/EmptyState';
import { ErrorState } from '../components/ErrorState';
import { LoadingState } from '../components/LoadingState';
import { MacroRing } from '../components/MacroRing';
import { getNutritionDay, type NutritionDay, type NutritionMeal } from '../api/nutrition';
import { getShoppingList } from '../api/shopping';
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
        <h1 className={styles.title}>Nutrición</h1>
        <p className={styles.subtitle}>Alimenta tu cuerpo, alcanza tus objetivos.</p>
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

  return (
    <>
      <section className={styles.summary} aria-label="Resumen de macronutrientes">
        <Card title="Calorías" headingLevel={2}>
          <p className={styles.calories}>
            <span className={styles.caloriesValue}>{day.targets.calories}</span>
            <span className={styles.caloriesUnit}> kcal objetivo</span>
          </p>
        </Card>
        <Card title="Distribución de macros" headingLevel={2}>
          <MacroRing
            proteinG={day.targets.proteinG}
            carbsG={day.targets.carbsG}
            fatG={day.targets.fatG}
          />
        </Card>
      </section>

      {dayType === 'running' && <RunningGuidance meals={day.meals} />}

      <Card title="Comidas del día" headingLevel={2}>
        <ol className={styles.meals}>
          {day.meals.map((meal) => (
            <li key={`${meal.mealType}-${meal.preferredTime}`}>
              <MealCard meal={meal} />
            </li>
          ))}
        </ol>
      </Card>

      <RecoveryRecommendation meals={day.meals} />
    </>
  );
}

function MealCard({ meal }: { readonly meal: NutritionMeal }) {
  return (
    <Card>
      <div className={styles.mealHeader}>
        <div>
          <p className={styles.mealTime}>{meal.preferredTime}</p>
          {/* FOR-112: was a hardcoded <h4>; "Comidas del día" above is now an
              <h2>, so this must be an <h3> to avoid skipping a level. */}
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
