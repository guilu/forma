import { useEffect, useState } from 'react';
import { Card } from '../../components/Card';
import { ErrorState } from '../../components/ErrorState';
import { ProgressRing } from '../../components/ProgressRing';
import { WidgetLoading } from '../../components/WidgetLoading';
import { getNutritionDay, type NutritionDay } from '../../api/nutrition';
import styles from './CaloriesWidget.module.css';

/**
 * "Calorías hoy" metrics tile (FOR-164 dashboard mockup). Shows today's calorie
 * *target* from the FOR-33 nutrition day (`GET /nutrition/days/{type}`) and a
 * consumed-vs-target ring.
 *
 * <p><b>Hybrid data.</b> The target (and the "Objetivo" caption) is real; the
 * consumed figure and the ring percentage are the SAME isolated placeholder the
 * "Menú de hoy" widget uses ({@link PLACEHOLDER_CONSUMED}) — there is no
 * calorie-logging endpoint, so consumption isn't backed. Kept obvious and
 * consistent so both are removed together once a consumption API exists. Day
 * type is hardcoded to `running`, matching NutritionPage.
 */
type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'ready'; readonly day: NutritionDay };

/** Placeholder "consumed so far" kcal — see the file doc comment. */
const PLACEHOLDER_CONSUMED = 2120;

const KCAL = new Intl.NumberFormat('es-ES');

export function CaloriesWidget() {
  const [state, setState] = useState<State>({ status: 'loading' });

  useEffect(() => {
    let active = true;
    getNutritionDay('running')
      .then((day) => {
        if (active) setState({ status: 'ready', day });
      })
      .catch(() => {
        if (active) setState({ status: 'error' });
      });
    return () => {
      active = false;
    };
  }, []);

  if (state.status === 'loading') {
    return <WidgetLoading label="Cargando tus calorías de hoy…" rows={1} />;
  }
  if (state.status === 'error') {
    return <ErrorState message="No se pudieron cargar tus calorías de hoy." />;
  }

  const target = state.day.targets.calories;
  const percent = target > 0 ? Math.round((PLACEHOLDER_CONSUMED / target) * 100) : 0;

  return (
    <Card title="Calorías hoy">
      <div className={styles.body}>
        <div className={styles.text}>
          <p className={styles.value}>
            {KCAL.format(PLACEHOLDER_CONSUMED)}
            <span className={styles.unit}> kcal</span>
          </p>
          <p className={styles.caption}>Objetivo: {KCAL.format(target)} kcal</p>
        </div>
        <ProgressRing
          value={PLACEHOLDER_CONSUMED}
          max={target}
          label={`Calorías consumidas: ${percent}% del objetivo`}
          size={72}
        >
          <span className={styles.ringText}>{percent}%</span>
        </ProgressRing>
      </div>
    </Card>
  );
}
