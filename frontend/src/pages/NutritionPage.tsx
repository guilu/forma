import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Button } from '../components/Button';
import { Card } from '../components/Card';
import { NutritionRings, RING_ARCS } from '../components/NutritionRings';
import { measure } from '../format/measures';
import { NoPlanEmptyState } from '../components/NoPlanEmptyState';
import { ErrorState } from '../components/ErrorState';
import { Icon } from '../components/Icon';
import { LoadingState } from '../components/LoadingState';
import { Modal } from '../components/Modal';
import { useNotify } from '../components/NotificationProvider';
import {
  getDayConsumption,
  getNutritionDay,
  type DayConsumption,
  type NutritionDay,
  type NutritionMeal,
  type PlannedMealState,
} from '../api/nutrition';
import { LogMealForm } from './nutrition/LogMealForm';
import { formatShortDate } from './dateLabel';
import { localIsoDate } from './localIsoDate';
import { usePlannedMealToggle } from './usePlannedMealToggle';
import styles from './NutritionPage.module.css';

/**
 * Nutrition, for today.
 *
 * <p>The page used to ask two questions at once and let the answers sit side by side: a "what have
 * you eaten" card fed by the date-based consumption endpoint, and a day-type selector
 * (Carrera/Fuerza/Descanso) feeding a separate view of the plan. They were free to disagree, and
 * did — the card showed today's target while the block below it swore there was no plan.
 *
 * <p>Now there is one question — <b>today</b> — and the two endpoints answer different halves of
 * it. The consumption read model already knows which KIND of day today is (it resolves the date
 * through the shared training-day policy), so the plan side is asked for that kind instead of for
 * whatever a selector happened to point at. The selector is gone: it let you read Tuesday's food on
 * a Thursday, which is not something a screen called "today" should offer.
 *
 * <ul>
 *   <li>consumption — calories and macros EATEN, the target they are measured against, and the
 *       state of each planned meal;
 *   <li>the plan's day — the meals themselves and what each one comes to.
 * </ul>
 *
 * <p><b>No meal photographs.</b> The mockup shows one per meal and no endpoint carries image data —
 * not the plan, not the food catalog, not recipes. The frame stays with the nutrition glyph in it
 * rather than filled with a stock photo of a dish nobody cooked.
 */
type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'empty' }
  | { readonly status: 'ready'; readonly day: NutritionDay };

const MEAL_LABELS: Record<string, string> = {
  BREAKFAST: 'Desayuno',
  MID_MORNING: 'Media mañana',
  LUNCH: 'Comida',
  SNACK: 'Merienda',
  PRE_WORKOUT: 'Pre-entreno',
  POST_WORKOUT: 'Post-entreno',
  DINNER: 'Cena',
};

export function NutritionPage() {
  const notify = useNotify();
  // Read once per mounted page: both endpoints and the label share a date, without freezing
  // "today" at module-import time for the lifetime of the browser tab.
  const [today] = useState(() => new Date());
  const todayIso = localIsoDate(today);
  const todayLabel = formatShortDate(today);
  const [retryToken, setRetryToken] = useState(0);
  const [state, setState] = useState<State>({ status: 'loading' });
  const [consumption, setConsumption] = useState<DayConsumption | undefined>(undefined);
  const [logging, setLogging] = useState(false);

  const reloadConsumption = useCallback(() => {
    return getDayConsumption(todayIso).then((day) => {
      setConsumption(day);
      return day;
    });
  }, [todayIso]);

  const { marking, toggle: toggleEaten } = usePlannedMealToggle(todayIso, reloadConsumption);

  /*
   * El plan se pide para el tipo de día que dice el servidor, así que las dos mitades hablan del
   * mismo día. Encadenado y no en paralelo por eso mismo: la segunda petición necesita la respuesta
   * de la primera, y adivinarla aquí metería la política de tipos de día en el navegador.
   */
  useEffect(() => {
    let active = true;
    setState({ status: 'loading' });
    reloadConsumption()
      .then((day) => {
        if (!active) {
          return undefined;
        }
        if (!day?.dayType) {
          setState({ status: 'empty' });
          return undefined;
        }
        return getNutritionDay(day.dayType.toLowerCase());
      })
      .then((day) => {
        if (active && day) {
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
  }, [reloadConsumption, retryToken]);

  return (
    <div className={styles.wrapper}>
      <header className={styles.header}>
        <div className={styles.titles}>
          <h1 className={styles.title}>Tu Nutrición de Hoy</h1>
          <p className={styles.subtitle}>{todayLabel} · Sigue el plan para alcanzar tu objetivo.</p>
        </div>
        <Button variant="accent" type="button" onClick={() => setLogging(true)}>
          + Registrar
        </Button>
      </header>

      {renderContent(state, consumption, () => setRetryToken((token) => token + 1), {
        marking,
        onToggle: toggleEaten,
      })}

      {/* El único acceso a los planes: no está en la navegación lateral, así que quitarlo de aquí
          los dejaría inalcanzables. */}
      <Link className={styles.plansLink} to="/app/nutrition/plans">
        Editar mis planes
      </Link>

      {logging && (
        <Modal title="Registrar comida" onClose={() => setLogging(false)}>
          <LogMealForm
            date={todayIso}
            plannedMeals={consumption?.plannedMeals ?? []}
            onCancel={() => setLogging(false)}
            onLogged={() => {
              setLogging(false);
              void reloadConsumption().catch(() =>
                notify.error('No se pudo actualizar el resumen del día.'),
              );
            }}
          />
        </Modal>
      )}
    </div>
  );
}

/** Sin tildes, sin mayúsculas y sin espacios de sobra, para comparar «Media mañana» con «MEDIA MAÑANA». */
function plain(text: string): string {
  return text.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim();
}

/**
 * Qué titula una comida.
 *
 * <p>El plan transcrito del Excel llama a cada comida por su tipo: la del desayuno se llama
 * «Desayuno». Puesta bajo la etiqueta del tipo salía dos veces la misma palabra y ninguna decía qué
 * se come. Cuando el nombre no añade nada al tipo, titulan los alimentos, que es lo que alguien
 * mira para saber si le toca cocinar.
 *
 * <p>Un plan que SÍ nombre sus comidas —«Bowl de yogur proteico y fruta»— conserva su nombre: lo
 * escribió alguien y dice más que la lista de la compra de ese plato.
 */
function headlineOf(meal: NutritionMeal): string {
  const namesTheType = plain(meal.name) === plain(MEAL_LABELS[meal.mealType] ?? meal.mealType);
  if (!namesTheType || meal.items.length === 0) {
    return meal.name;
  }
  return meal.items.map((item) => item.food).join(', ');
}

interface MealActions {
  /** The meal whose request is in flight, so only its own control shows the wait. */
  readonly marking: ReadonlySet<string>;
  readonly onToggle: (meal: NutritionMeal, eaten: boolean) => void;
}

function renderContent(
  state: State,
  consumption: DayConsumption | undefined,
  retry: () => void,
  actions: MealActions,
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
    return <NoPlanEmptyState />;
  }

  const consumed = consumption?.consumed;
  const target = consumption?.target ?? null;
  const states = new Map(consumption?.plannedMeals.map((meal) => [meal.id, meal.state]) ?? []);
  const eaten = state.day.meals.filter((meal) => states.get(meal.id) === 'EATEN').length;

  return (
    <>
      <section className={styles.summary} aria-label="Resumen del día">
        {/* One card, because one set of rings answers both halves: the outer ring is the day's
            calories and the three inside are the macros. Two cards meant drawing the same day
            twice and letting the two drawings drift apart. */}
        <Card title="Calorías y macros" headingLevel={2}>
          <DayProgress consumed={consumed} target={target} />
        </Card>
      </section>

      <section className={styles.meals} aria-label="Comidas de hoy">
        <div className={styles.mealsHead}>
          <h2 className={styles.mealsTitle}>Comidas de Hoy</h2>
          <p className={styles.mealsCount}>
            {eaten} de {state.day.meals.length} completadas
          </p>
        </div>
        <ol className={styles.mealList}>
          {state.day.meals.map((meal) => (
            <li key={meal.id}>
              <MealCard
                meal={meal}
                state={states.get(meal.id)}
                marking={actions.marking.has(meal.id)}
                onMark={() => actions.onToggle(meal, states.get(meal.id) === 'EATEN')}
              />
            </li>
          ))}
        </ol>
      </section>
    </>
  );
}

/** Nothing eaten yet reads as zeroes, not as a missing card: the day starts at zero. */
const NOTHING_EATEN = { kcal: 0, proteinG: 0, carbsG: 0, fatG: 0 };

/**
 * Today's calories and macros, eaten against target.
 *
 * <p>Both figures are the server's. A target of `null` means the plan sets none, and then the ring
 * stays empty rather than being drawn against an invented maximum — a ring needs a ceiling, and
 * the only one available would have been made up here (FOR-134).
 *
 * <p>The colour beside each name is the colour of its ring, and it only ever repeats what the
 * label already says: the name in text is what holds when colour does not arrive (colour
 * blindness, high contrast, print).
 */
function DayProgress({
  consumed,
  target,
}: {
  readonly consumed: DayConsumption['consumed'] | undefined;
  readonly target: DayConsumption['target'];
}) {
  const eaten = consumed ?? NOTHING_EATEN;
  const remaining = target === null ? null : Math.max(target.kcal - eaten.kcal, 0);

  return (
    <div className={styles.dayProgress}>
      <NutritionRings consumed={eaten} target={target} size="11rem" />
      <ul className={styles.macros}>
        {RING_ARCS.map((arc) => {
          const goal = target?.[arc.key] ?? null;
          const unit = arc.key === 'kcal' ? 'kcal' : 'g';
          return (
            <li key={arc.key} className={styles.macro}>
              <span className={styles.macroName}>
                <span
                  className={styles.macroDot}
                  style={{ background: arc.color }}
                  aria-hidden="true"
                />
                {arc.key === 'kcal' ? 'Calorías' : arc.label}
              </span>
              <span className={styles.macroValue}>
                {measure(eaten[arc.key])} {unit}
                {goal !== null && (
                  <span className={styles.macroGoal}>
                    {' '}
                    / {measure(goal)} {unit}
                  </span>
                )}
              </span>
              {/* Lo que falta, no lo que suma el plan. Un guion cuando nadie ha fijado objetivo,
                  que no es lo mismo que no quedar nada. */}
              {arc.key === 'kcal' && (
                <span className={styles.macroNote}>
                  {remaining === null ? 'Sin objetivo' : `Te quedan ${measure(remaining)} kcal`}
                </span>
              )}
            </li>
          );
        })}
      </ul>
    </div>
  );
}

/**
 * One meal of the plan, with the answer to "have you had it?".
 *
 * <p>A real checkbox and not a styled div: it says what it is to a screen reader and takes a
 * keyboard without any of that being rebuilt worse here. Ticking logs the planned meal; unticking
 * removes the owner/date-scoped persisted answer and reloads the server read model.
 */
function MealCard({
  meal,
  state,
  marking,
  onMark,
}: {
  readonly meal: NutritionMeal;
  readonly state: PlannedMealState['state'] | undefined;
  readonly marking: boolean;
  readonly onMark: () => void;
}) {
  const eaten = state === 'EATEN';
  return (
    <article className={eaten ? `${styles.mealCard} ${styles.mealDone}` : styles.mealCard}>
      {/* Sin foto: ningún endpoint la tiene. El marco se queda con el glifo. */}
      <span className={styles.mealPhoto} aria-hidden="true">
        <Icon name="nutrition" size={22} />
      </span>
      <div className={styles.mealBody}>
        <p className={styles.mealType}>
          {MEAL_LABELS[meal.mealType] ?? meal.mealType}
          {meal.optional && <span className={styles.mealOptional}> · opcional</span>}
        </p>
        <h3 className={styles.mealName}>{headlineOf(meal)}</h3>
        <p className={styles.chips}>
          <span className={styles.chipKcal}>{meal.totals.calories} kcal</span>
          <span className={`${styles.chip} ${styles.chipProtein}`}>{meal.totals.proteinG}g P</span>
          <span className={`${styles.chip} ${styles.chipCarbs}`}>{meal.totals.carbsG}g C</span>
          <span className={`${styles.chip} ${styles.chipFat}`}>{meal.totals.fatG}g G</span>
        </p>
      </div>
      <label className={styles.check}>
        <input
          type="checkbox"
          className={styles.checkInput}
          checked={eaten}
          disabled={marking}
          onChange={onMark}
        />
        <span className={styles.checkMark} aria-hidden="true">
          {eaten && <Icon name="check" size={16} />}
        </span>
        <span className={styles.checkLabel}>
          {marking
            ? `Actualizando ${meal.name}`
            : eaten
              ? `Desmarcar ${meal.name} como hecha`
              : `Marcar ${meal.name} como hecha`}
        </span>
      </label>
    </article>
  );
}
