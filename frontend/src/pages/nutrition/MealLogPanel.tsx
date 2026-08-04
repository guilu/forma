import { useCallback, useEffect, useState } from 'react';
import { Badge } from '../../components/Badge';
import { Button } from '../../components/Button';
import { Card } from '../../components/Card';
import { ErrorState } from '../../components/ErrorState';
import { Icon } from '../../components/Icon';
import { LoadingState } from '../../components/LoadingState';
import { Modal } from '../../components/Modal';
import { getDayConsumption, type DayConsumption, type PlannedMealState } from '../../api/nutrition';
import { LogMealForm } from './LogMealForm';
import styles from './MealLogPanel.module.css';

/**
 * What was actually eaten today, beside what the plan asked for (FOR-127/FOR-134,
 * plan adherence V55).
 *
 * <p>The endpoints behind this have existed since FOR-127 and no screen had ever
 * called them: the app could compute a day's consumption, compare it to the plan
 * and say which planned meals were still pending, and there was nowhere to log a
 * single thing. This is that screen.
 *
 * <p>Nothing here adds up anything. The day's totals, the comparison against the
 * target and each planned meal's state all arrive computed — the last of them
 * derived on every read rather than stored, so a meal still pending at six
 * o'clock reads as skipped tomorrow without anybody writing anything.
 */
interface MealLogPanelProps {
  /** The day being shown, ISO. */
  readonly date: string;
}

const STATE_LABELS: Record<PlannedMealState['state'], string> = {
  EATEN: 'Hecha',
  PENDING: 'Pendiente',
  SKIPPED: 'Sin registrar',
};

const STATE_TONES: Record<PlannedMealState['state'], 'accent' | 'neutral' | 'warning'> = {
  EATEN: 'accent',
  PENDING: 'neutral',
  SKIPPED: 'warning',
};

const MEAL_LABELS: Record<string, string> = {
  BREAKFAST: 'Desayuno',
  MID_MORNING: 'Media mañana',
  LUNCH: 'Comida',
  SNACK: 'Merienda',
  PRE_WORKOUT: 'Pre-entreno',
  POST_WORKOUT: 'Post-entreno',
  DINNER: 'Cena',
};

type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly day: DayConsumption };

export function MealLogPanel({ date }: MealLogPanelProps) {
  const [state, setState] = useState<State>({ status: 'loading' });
  const [logging, setLogging] = useState(false);
  const [answering, setAnswering] = useState<string | undefined>(undefined);

  const reload = useCallback(() => {
    setState({ status: 'loading' });
    getDayConsumption(date)
      .then((day) => setState({ status: 'ready', day }))
      .catch(() => setState({ status: 'error' }));
  }, [date]);

  useEffect(reload, [reload]);

  if (state.status === 'loading') {
    return <LoadingState message="Cargando lo que has comido…" />;
  }
  if (state.status === 'error') {
    return <ErrorState message="No se pudo cargar el registro del día." onRetry={reload} />;
  }

  const { day } = state;
  const open = logging || answering !== undefined;

  return (
    <Card>
      <div className={styles.head}>
        <div>
          <h2 className={styles.title}>Lo que has comido</h2>
          <p className={styles.total}>
            {day.consumed.kcal} kcal
            {/* The target is only ever sent whole: it and the comparison stand or fall together. */}
            {day.target && <span className={styles.target}> de {day.target.kcal}</span>}
          </p>
        </div>
        <Button variant="accent" type="button" onClick={() => setLogging(true)}>
          + Registrar
        </Button>
      </div>

      <dl className={styles.macros}>
        <div>
          <dt>Proteínas</dt>
          <dd>{day.consumed.proteinG} g</dd>
        </div>
        <div>
          <dt>Hidratos</dt>
          <dd>{day.consumed.carbsG} g</dd>
        </div>
        <div>
          <dt>Grasas</dt>
          <dd>{day.consumed.fatG} g</dd>
        </div>
      </dl>

      {day.plannedMeals.length > 0 && (
        <section className={styles.section}>
          <h3 className={styles.sectionTitle}>Lo que pedía el plan</h3>
          <ul className={styles.planned}>
            {day.plannedMeals.map((meal) => (
              <li key={meal.id} className={styles.plannedItem}>
                <span className={styles.plannedName}>{meal.name}</span>
                <Badge tone={STATE_TONES[meal.state]}>{STATE_LABELS[meal.state]}</Badge>
                {meal.state !== 'EATEN' && (
                  <button
                    type="button"
                    className={styles.answer}
                    onClick={() => setAnswering(meal.id)}
                  >
                    Registrar esta
                  </button>
                )}
              </li>
            ))}
          </ul>
        </section>
      )}

      <section className={styles.section}>
        <h3 className={styles.sectionTitle}>Registrado hoy</h3>
        {day.entries.length === 0 ? (
          <p className={styles.empty}>
            Todavía no has registrado nada. Lo que apuntes aquí se suma solo y se compara con lo que
            pedía el plan.
          </p>
        ) : (
          <ul className={styles.entries}>
            {day.entries.map((entry) => (
              <li key={entry.id} className={styles.entry}>
                <Icon name="check" size={16} />
                <span className={styles.entryName}>{entry.name}</span>
                <span className={styles.entryMeal}>
                  {MEAL_LABELS[entry.mealType] ?? entry.mealType}
                </span>
                <span className={styles.entryKcal}>{entry.kcal} kcal</span>
              </li>
            ))}
          </ul>
        )}
      </section>

      {open && (
        <Modal
          title="Registrar comida"
          onClose={() => {
            setLogging(false);
            setAnswering(undefined);
          }}
        >
          <LogMealForm
            date={date}
            plannedMeals={day.plannedMeals}
            plannedMealId={answering}
            onCancel={() => {
              setLogging(false);
              setAnswering(undefined);
            }}
            onLogged={() => {
              setLogging(false);
              setAnswering(undefined);
              reload();
            }}
          />
        </Modal>
      )}
    </Card>
  );
}
