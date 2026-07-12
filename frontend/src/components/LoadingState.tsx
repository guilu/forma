import styles from './LoadingState.module.css';

/**
 * Shared page-level loading state (FOR-60). Standardizes the "Cargando…" text
 * every feature page rendered ad hoc (`MeasurementsPage`, `TrainingPage`,
 * `NutritionPage`, `ShoppingPage`, `ProgressPage`, …) behind one component so
 * copy and the `role="status"` announcement stay consistent. Feature pages
 * should still choose their own domain-specific `message` (e.g. "Cargando tus
 * mediciones…") — this component only standardizes the layout and a11y
 * wiring, never invents the copy.
 *
 * <p>Announced via `role="status"` so assistive tech hears the loading change
 * without interrupting the user (feeds FOR-61). For content areas prefer
 * {@link WidgetLoading}'s skeleton instead, which avoids the layout jump a
 * full-page spinner causes when content finally renders.
 */
interface LoadingStateProps {
  readonly message?: string;
}

export function LoadingState({ message = 'Cargando…' }: LoadingStateProps) {
  return (
    <div className={styles.wrapper} role="status">
      <span className={styles.spinner} aria-hidden="true" />
      <p className={styles.message}>{message}</p>
    </div>
  );
}
