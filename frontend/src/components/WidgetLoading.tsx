import styles from './WidgetLoading.module.css';

/**
 * Shared widget/content-area loading state (FOR-60). A shimmering skeleton —
 * not a spinner — because a fixed-size placeholder is what actually avoids
 * the layout jump between "loading" and "loaded" (spec `specs/FOR-60/ui.md`:
 * "skeleton for content areas, spinner for quick actions; no layout jump").
 * Reuses the shimmer animation `ChartContainer` already established for its
 * own loading frame, generalized into a standalone component so dashboard
 * widgets (`BodyWidget`, `TrainingWidget`, …) can use the same treatment
 * without going through `ChartContainer`'s chart-specific API.
 *
 * <p>`rows` controls how many skeleton bars render — callers pick a count
 * that roughly matches their real content's height, which is what actually
 * prevents the jump; the component does not try to measure content itself.
 */
interface WidgetLoadingProps {
  readonly label?: string;
  readonly rows?: number;
}

export function WidgetLoading({ label = 'Cargando…', rows = 3 }: WidgetLoadingProps) {
  return (
    <div className={styles.wrapper} role="status">
      {Array.from({ length: rows }, (_, index) => (
        <span key={index} className={styles.bar} aria-hidden="true" />
      ))}
      <span className={styles.srOnly}>{label}</span>
    </div>
  );
}
