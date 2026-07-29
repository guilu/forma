import { useEffect, useState } from 'react';
import { EmptyState } from '../../components/EmptyState';
import { ErrorState } from '../../components/ErrorState';
import { MacroRing } from '../../components/MacroRing';
import { WidgetLoading } from '../../components/WidgetLoading';
import { getNutritionDay, type NutritionDay } from '../../api/nutrition';
import { WidgetSection } from './WidgetSection';
import { ProgressBar } from './ProgressBar';
import styles from './MacrosWidget.module.css';

/**
 * "Macronutrientes" widget (FOR-164 dashboard mockup): the FOR-54
 * {@link MacroRing} over today's protein/carb/fat split, with one
 * consumed-vs-target bar per macro underneath.
 *
 * <p><b>Hybrid data.</b> The targets (grams) are real, from the FOR-33
 * nutrition day. The consumed halves are the same kind of clearly-labelled
 * placeholder {@link CaloriesWidget} and {@link NutritionWidget} use
 * ({@link PLACEHOLDER_CONSUMED_G}) — there is no consumption-logging endpoint,
 * so intake isn't backed. Kept obvious and in one constant so all three are
 * removed together once that API exists. Day type is hardcoded to `running`,
 * matching NutritionPage.
 *
 * <p>With no plan for today the widget says so, in the same words as the other
 * two nutrition cards. It used to render nothing at all, which left the card
 * empty *and* pushed its title into the middle of it: the section's last child
 * takes the leftover height (`WidgetSection.module.css`), and with no content
 * that child was the header itself.
 */
type State =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | { readonly status: 'empty' }
  | { readonly status: 'ready'; readonly day: NutritionDay };

/** Placeholder "consumed so far" grams per macro — see the file doc comment. */
const PLACEHOLDER_CONSUMED_G = { protein: 162, carbs: 236, fat: 68 } as const;

/**
 * The three bars, in the ring's own order so a colour reads the same in both.
 * `color` matches the corresponding slice of {@link MacroRing}'s gradient.
 */
const MACROS = [
  {
    key: 'protein',
    label: 'Proteínas',
    color: 'var(--color-accent)',
    target: (t: NutritionDay['targets']) => t.proteinG,
  },
  {
    key: 'carbs',
    label: 'Carbohidratos',
    color: 'var(--color-warning)',
    target: (t: NutritionDay['targets']) => t.carbsG,
  },
  {
    key: 'fat',
    label: 'Grasas',
    color: 'var(--color-text-muted)',
    target: (t: NutritionDay['targets']) => t.fatG,
  },
] as const;

export function MacrosWidget() {
  const [state, setState] = useState<State>({ status: 'loading' });

  useEffect(() => {
    let active = true;
    getNutritionDay('running')
      .then((day) => {
        if (!active) return;
        setState(day.meals.length === 0 ? { status: 'empty' } : { status: 'ready', day });
      })
      .catch(() => {
        if (active) setState({ status: 'error' });
      });
    return () => {
      active = false;
    };
  }, []);

  return (
    <WidgetSection id="macros-widget-title" title="Macronutrientes">
      {renderContent(state)}
    </WidgetSection>
  );
}

function renderContent(state: State) {
  if (state.status === 'loading') {
    return <WidgetLoading label="Cargando tus macronutrientes…" rows={2} />;
  }
  if (state.status === 'error') {
    return <ErrorState message="No se pudieron cargar tus macronutrientes." />;
  }
  if (state.status === 'empty') {
    return <EmptyState variant="filtered" title="No hay un plan de comidas para hoy todavía." />;
  }

  const { targets } = state.day;

  return (
    <div className={styles.card}>
      {/* Ring on top, bars underneath: the bars already name and quantify each
          macro, so the ring's own legend would repeat them. */}
      <MacroRing
        proteinG={targets.proteinG}
        carbsG={targets.carbsG}
        fatG={targets.fatG}
        showLegend={false}
      />
      <ul className={styles.macros}>
        {MACROS.map((macro) => {
          const target = macro.target(targets);
          const consumed = PLACEHOLDER_CONSUMED_G[macro.key];
          return (
            <li key={macro.key} className={styles.macro}>
              <span className={styles.macroLabel}>{macro.label}</span>
              {/* Consumed figure is placeholder; the target is real. */}
              <span className={styles.macroValue}>
                {consumed} / {target} g
              </span>
              <ProgressBar
                value={consumed}
                max={target}
                color={macro.color}
                label={`${macro.label}: ${consumed} de ${target} gramos`}
                showPercent={false}
              />
            </li>
          );
        })}
      </ul>
    </div>
  );
}
