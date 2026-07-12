import { Button } from './Button';
import { Icon } from './Icon';
import styles from './ErrorState.module.css';

/**
 * Shared recoverable-API-error state (FOR-60): standardizes the "couldn't
 * load, try again" pattern every feature page rendered ad hoc
 * (`MeasurementsPage`, `TrainingPage`, `NutritionPage`, `ShoppingPage`,
 * `IntegrationsSection`, …) — calm message + a labelled, keyboard-operable
 * retry button (spec `specs/FOR-60/ui.md` accessibility requirement).
 *
 * <p>`message` must always be a calm, domain-aware sentence supplied by the
 * caller — this component never renders the raw error/exception itself
 * (AGENTS.md "no known security regression", spec FOR-60 "no stack/technical
 * details"). The optional `detail` prop exists only for local debugging and
 * is only rendered when the caller explicitly passes `showDetail`, which
 * callers should derive from a dev flag (e.g. Vite's `import.meta.env.DEV`)
 * rather than the component reading environment state itself — this keeps
 * the "never in production" guarantee explicit and independently testable.
 *
 * <p>`role="alert"` so the failure interrupts and is announced immediately
 * (unlike the passive `role="status"` used by loading/empty), matching every
 * existing page's error paragraph today.
 */
interface ErrorStateProps {
  readonly title?: string;
  readonly message: string;
  readonly onRetry?: () => void;
  readonly retryLabel?: string;
  /** Dev-only diagnostic detail (e.g. `error.message`); only shown when `showDetail` is true. */
  readonly detail?: string;
  /** Caller-controlled dev flag gating `detail`. Defaults to false (never shown). */
  readonly showDetail?: boolean;
}

export function ErrorState({
  title,
  message,
  onRetry,
  retryLabel = 'Reintentar',
  detail,
  showDetail = false,
}: ErrorStateProps) {
  return (
    <div className={styles.wrapper} role="alert">
      <Icon name="alertTriangle" size={28} className={styles.icon} />
      {title && <p className={styles.title}>{title}</p>}
      <p className={styles.message}>{message}</p>
      {onRetry && (
        <Button variant="secondary" type="button" onClick={onRetry} className={styles.retry}>
          {retryLabel}
        </Button>
      )}
      {showDetail && detail && <pre className={styles.detail}>{detail}</pre>}
    </div>
  );
}
