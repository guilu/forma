import { Card } from './Card';
import styles from './MetricCard.module.css';

/**
 * Single dashboard metric tile (FOR-19). Reuses {@link Card} for the surface and
 * its uppercase muted label, and renders a large, readable numeric value with an
 * optional unit — matching the metric cards in docs/1-dashboard.png /
 * docs/2-mediciones.png. Purely presentational: it displays already-formatted
 * values and never computes anything (ADR-006).
 */
interface MetricCardProps {
  readonly label: string;
  readonly value: string;
  readonly unit?: string;
}

export function MetricCard({ label, value, unit }: MetricCardProps) {
  return (
    <Card title={label}>
      <p className={styles.value}>
        {value}
        {unit && <span className={styles.unit}> {unit}</span>}
      </p>
    </Card>
  );
}
