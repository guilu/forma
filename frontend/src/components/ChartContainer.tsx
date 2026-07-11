import type { ReactNode } from 'react';
import { Card } from './Card';
import styles from './ChartContainer.module.css';

export type ChartContainerState = 'ready' | 'loading' | 'empty';

const DEFAULT_EMPTY_MESSAGE = 'Todavía no hay datos suficientes.';

/**
 * Consistent framing around chart primitives like {@link LineChart} (FOR-50).
 * Reuses {@link Card}'s title+action header (e.g. "EVOLUCIÓN DE PESO") and adds
 * the loading/empty frames the mockups need, so feature pages don't each
 * reimplement a skeleton/empty state for their chart card.
 */
interface ChartContainerProps {
  readonly title: string;
  readonly action?: ReactNode;
  readonly state?: ChartContainerState;
  readonly emptyMessage?: string;
  /** Typically a {@link LineChart}; only rendered in the `ready` state. */
  readonly children: ReactNode;
}

export function ChartContainer({
  title,
  action,
  state = 'ready',
  emptyMessage = DEFAULT_EMPTY_MESSAGE,
  children,
}: ChartContainerProps) {
  return (
    <Card title={title} action={action}>
      {state === 'loading' && (
        <div className={styles.frame} role="status">
          <span className={styles.skeleton} aria-hidden="true" />
          <span className={styles.srOnly}>Cargando…</span>
        </div>
      )}
      {state === 'empty' && <p className={styles.frame}>{emptyMessage}</p>}
      {state === 'ready' && <div className={styles.chart}>{children}</div>}
    </Card>
  );
}
