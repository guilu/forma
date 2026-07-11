import type { ButtonHTMLAttributes } from 'react';
import styles from './Button.module.css';

export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'destructive';

/**
 * Base action primitive (FOR-50). Generalizes the accent/outline button pair
 * already used by {@link MeasurementForm} into the four variants the mockups
 * need: `primary` (accent, main action), `secondary` (outline), `ghost`
 * (borderless, low emphasis) and `destructive` (danger, e.g. delete/cancel a
 * plan). Token-driven only — no hardcoded colors.
 *
 * <p>`loading` implies `disabled` (a pending action must not be re-triggered)
 * and is announced via `aria-busy` rather than swapped text, so callers keep
 * full control over the label (ADR-006: no hidden business rules here).
 */
interface ButtonProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'type'> {
  readonly variant?: ButtonVariant;
  readonly type?: 'button' | 'submit' | 'reset';
  readonly loading?: boolean;
}

export function Button({
  variant = 'primary',
  type = 'button',
  loading = false,
  disabled,
  className,
  children,
  ...rest
}: ButtonProps) {
  return (
    <button
      type={type}
      className={[styles.button, styles[variant], className].filter(Boolean).join(' ')}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      {...rest}
    >
      {loading && <span className={styles.spinner} aria-hidden="true" />}
      {children}
    </button>
  );
}
