import type { ButtonHTMLAttributes, ReactNode } from 'react';
import styles from './IconButton.module.css';

export type IconButtonVariant = 'surface' | 'soft' | 'ghost';
export type IconButtonTone = 'default' | 'danger';
export type IconButtonSize = 'sm' | 'md' | 'lg';

/**
 * Icon-only action primitive: the square control the topbar, the modals, the
 * table rows, the date steppers and the quantity steppers had each been
 * re-declaring in their own CSS module — at six different sizes and three
 * different surfaces, for what is the same control.
 *
 * <p>It is a sibling of {@link Button}, not a variant of it. `Button` sizes
 * itself around a label and carries emphasis in its fill; this one is a fixed
 * square whose whole job is to hold a glyph, so the two share no measurements
 * and would only have collided in one component's props. The `variant` names
 * are deliberately the same words as `Button`'s, though: `soft` here is the
 * same accent wash it is there, so the vocabulary transfers between them.
 *
 * <p>`label` is required rather than optional: an icon-only control has no text
 * to name it, so without an `aria-label` it reaches the accessibility tree
 * unnamed. Making it part of the type is what stops that from being something
 * each caller has to remember. The glyph itself stays `aria-hidden` (see
 * {@link Icon}), so the label is the only accessible name.
 *
 * <p>`loading` implies `disabled`, as it does on {@link Button}: a pending
 * action must not be re-triggered. Where it differs is what it does with the
 * glyph — it *replaces* it rather than sitting beside it, because a square this
 * size has no "beside". The label is untouched, so the control keeps its
 * accessible name while it is busy instead of going anonymous mid-action, and
 * `aria-busy` says why it stopped responding.
 *
 * <p>`tone="danger"` is presentation only — it tints a row deletion so the
 * destructive control does not read identically to "edit" beside it. It changes
 * no behaviour: a destructive action still needs its own confirmation (see
 * {@link ConfirmDialog}).
 */
interface IconButtonProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'type'> {
  readonly label: string;
  readonly variant?: IconButtonVariant;
  readonly tone?: IconButtonTone;
  readonly size?: IconButtonSize;
  readonly type?: 'button' | 'submit' | 'reset';
  readonly loading?: boolean;
  readonly children: ReactNode;
}

export function IconButton({
  label,
  variant = 'surface',
  tone = 'default',
  size = 'md',
  type = 'button',
  loading = false,
  disabled,
  className,
  children,
  ...rest
}: IconButtonProps) {
  return (
    <button
      type={type}
      aria-label={label}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      className={[styles.iconButton, styles[variant], styles[tone], styles[size], className]
        .filter(Boolean)
        .join(' ')}
      {...rest}
    >
      {loading ? <span className={styles.spinner} aria-hidden="true" /> : children}
    </button>
  );
}
