import { ErrorState } from '../../components/ErrorState';
import { WidgetLoading } from '../../components/WidgetLoading';
import type { TodayConsumptionState } from './todayNutrition';
import { WidgetSection } from './WidgetSection';
import { ProgressBar } from './ProgressBar';
import styles from './MacrosWidget.module.css';

/**
 * "Macronutrientes" widget (FOR-164 dashboard mockup): the FOR-54
 * today's protein/carb/fat consumption, with one consumed-vs-target bar per
 * macro. Both halves come from the date-based server read model.
 *
 * <p>With no plan for today the widget says so, in the same words as the other
 * two nutrition cards. It used to render nothing at all, which left the card
 * empty *and* pushed its title into the middle of it: the section's last child
 * takes the leftover height (`WidgetSection.module.css`), and with no content
 * that child was the header itself.
 */
/**
 * The three bars use the same semantic colour ordering as NutritionPage.
 */
const MACROS = [
  {
    key: 'proteinG',
    label: 'Proteínas',
    color: 'var(--color-accent)',
  },
  {
    key: 'carbsG',
    label: 'Carbohidratos',
    color: 'var(--color-warning-graphic)',
  },
  {
    key: 'fatG',
    label: 'Grasas',
    color: 'var(--color-text-muted)',
  },
] as const;

export function MacrosWidget({ state }: { readonly state: TodayConsumptionState }) {
  return (
    <WidgetSection id="macros-widget-title" title="Macronutrientes">
      {renderContent(state)}
    </WidgetSection>
  );
}

function renderContent(state: TodayConsumptionState) {
  if (state.status === 'loading') {
    return <WidgetLoading label="Cargando tus macronutrientes…" rows={2} />;
  }
  if (state.status === 'error') {
    return <ErrorState message="No se pudieron cargar tus macronutrientes." />;
  }
  const { consumed, target } = state.consumption;

  return (
    <div className={styles.card}>
      <ul className={styles.macros}>
        {MACROS.map((macro) => {
          const eaten = consumed[macro.key];
          const goal = target?.[macro.key] ?? null;
          return (
            <li key={macro.key} className={styles.macro}>
              <span className={styles.macroLabel}>{macro.label}</span>
              <span className={styles.macroValue}>
                {NUM.format(eaten)}
                {goal !== null ? ` / ${NUM.format(goal)} g` : ' g · Sin objetivo'}
              </span>
              {goal !== null && (
                <ProgressBar
                  value={eaten}
                  max={goal}
                  color={macro.color}
                  label={`${macro.label}: ${eaten} de ${goal} gramos`}
                  showPercent={false}
                />
              )}
            </li>
          );
        })}
      </ul>
    </div>
  );
}

const NUM = new Intl.NumberFormat('es-ES', { maximumFractionDigits: 1 });
