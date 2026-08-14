import type { ButtonHTMLAttributes, ReactNode } from 'react';
import styles from './Chip.module.css';

export type ChipSemantics = 'toggle' | 'tab' | 'radio';
export type ChipSize = 'sm' | 'md';

/**
 * Selection control: the pill that shows whether an option is *chosen*, as
 * opposed to {@link Button}, which shows how important an *action* is.
 *
 * <p>The distinction is the whole reason this exists as its own component. A
 * selected chip and `Button variant="accent"` look nearly identical — accent
 * fill, contrast label — but they mean opposite things: one reports state, the
 * other invites an action. Left to each page, that similarity is exactly what
 * made the category tabs, the chart ranges and the meal-shape picker get drawn
 * three times in three CSS modules with three different names.
 *
 * <p>`semantics` exists because the appearance is shared but the accessible
 * state is not interchangeable. A tablist announces `aria-selected`, a
 * radiogroup `aria-checked`, and a standalone filter `aria-pressed`; announcing
 * the wrong one (or two at once) misreports the widget to a screen reader. The
 * caller owns the grouping — it is the one rendering the `role="tablist"` or
 * `role="radiogroup"` wrapper and holding the state — so it names the semantics
 * here, and this component publishes exactly the one matching attribute.
 */
interface ChipProps
  extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'type' | 'role' | 'aria-pressed'> {
  readonly selected: boolean;
  readonly semantics?: ChipSemantics;
  readonly size?: ChipSize;
  readonly type?: 'button' | 'submit' | 'reset';
  readonly children: ReactNode;
}

export function Chip({
  selected,
  semantics = 'toggle',
  size = 'md',
  type = 'button',
  className,
  children,
  ...rest
}: ChipProps) {
  /*
   * Exactly one state attribute, never a union of them: a `role="tab"` that
   * also carries `aria-pressed` is reported as two conflicting widgets. The
   * toggle case takes no explicit role — a plain `<button>` with `aria-pressed`
   * already is the toggle-button pattern.
   */
  const semanticProps =
    semantics === 'tab'
      ? ({ role: 'tab', 'aria-selected': selected } as const)
      : semantics === 'radio'
        ? ({ role: 'radio', 'aria-checked': selected } as const)
        : ({ 'aria-pressed': selected } as const);

  return (
    <button
      type={type}
      className={[
        styles.chip,
        styles[size],
        selected ? styles.selected : styles.unselected,
        className,
      ]
        .filter(Boolean)
        .join(' ')}
      {...semanticProps}
      {...rest}
    >
      {children}
    </button>
  );
}
