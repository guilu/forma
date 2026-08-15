import type { ButtonHTMLAttributes } from 'react';
import styles from './Button.module.css';

export type ButtonVariant =
  'primary' | 'accent' | 'soft' | 'surface' | 'secondary' | 'ghost' | 'destructive';

export type ButtonTone = 'default' | 'danger';

/**
 * Base action primitive (FOR-50). Generalizes the accent/outline button pair
 * already used by {@link MeasurementForm} into the variants the mockups need:
 * `primary` (the brand gradient, one main action per screen), `accent` (the
 * same accent as a flat fill), `soft` (a tinted accent wash), `secondary`
 * (outline), `ghost` (borderless, low emphasis) and `destructive` (danger, e.g.
 * delete/cancel a plan). Token-driven only — no hardcoded colors.
 *
 * <p>The variants are an emphasis ladder, and picking one is a question about
 * the screen rather than about the button: `primary` for the single action the
 * screen exists for, `accent` for a strong action that is not that one, `soft`
 * for an in-card action that should stay accent-coloured without pulling focus
 * from the card's data, then `secondary` and `ghost` as the neutral rungs.
 *
 * <p>`tone="danger"` recolours the quiet variants (`soft`, `surface`,
 * `secondary`, `ghost`) into the danger ramp, for a delete that lives in a row
 * or a detail panel. It is a separate axis from `variant` for the same reason
 * it is on {@link IconButton}: emphasis and danger are independent questions,
 * and folding them together needs a variant name per pair. `destructive` stays
 * its own variant rather than becoming `primary` + `danger`, because it is the
 * loud gradient reserved for the confirm of a destructive dialog.
 *
 * <p>This is the *action* family. A control that reports whether an option is
 * chosen is {@link Chip}, and an icon-only square is {@link IconButton} — a
 * selected chip looks close to `accent` on purpose, but the two are not
 * interchangeable and neither is spelled with this component.
 *
 * <p>`accent` exists because the gradient is loud enough that two of them on one
 * screen compete (FOR-189: the settings page had it on "Editar perfil" beside a
 * gradient CTA elsewhere). It is the same flat treatment the app's segmented
 * selectors already use for the chosen option, so a filled accent means the same
 * thing wherever it appears.
 *
 * <p>`loading` implies `disabled` (a pending action must not be re-triggered)
 * and is announced via `aria-busy` rather than swapped text, so callers keep
 * full control over the label (ADR-006: no hidden business rules here).
 */
interface ButtonProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'type'> {
  readonly variant?: ButtonVariant;
  readonly tone?: ButtonTone;
  readonly type?: 'button' | 'submit' | 'reset';
  readonly loading?: boolean;
}

export function Button({
  variant = 'primary',
  tone = 'default',
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
      className={[styles.button, styles[variant], styles[tone], className]
        .filter(Boolean)
        .join(' ')}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      {...rest}
    >
      {loading && <span className={styles.spinner} aria-hidden="true" />}
      {children}
    </button>
  );
}
