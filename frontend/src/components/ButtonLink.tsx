import { Link, type LinkProps } from 'react-router-dom';
import type { ButtonVariant } from './Button';
import styles from './Button.module.css';

/**
 * A navigation that looks like a {@link Button}.
 *
 * <p>It stays a real `<Link>` rather than a `<button>` with an `onClick`
 * navigate: a link is what the browser can middle-click, open in a new tab,
 * copy the target of and prefetch, and it is what a screen reader announces as
 * a destination rather than an action. Swapping the element to match the paint
 * would trade all of that for nothing.
 *
 * <p>It pulls its classes straight from `Button.module.css` — the same source,
 * not a copy — so the emphasis ladder means the same thing whether the control
 * navigates or acts, and a change to a variant lands on both at once.
 *
 * <p>Pages that need to *resize* the button (the landing's display-size CTAs)
 * should keep using `composes: button primary from '…/Button.module.css'` in
 * their own module instead. `composes` guarantees the base rules are ordered
 * before the overriding ones; a `className` passed here is a separate class of
 * equal specificity, so which one wins would depend on bundle order.
 */
interface ButtonLinkProps extends LinkProps {
  readonly variant?: ButtonVariant;
}

export function ButtonLink({ variant = 'primary', className, children, ...rest }: ButtonLinkProps) {
  return (
    <Link
      className={[styles.button, styles[variant], className].filter(Boolean).join(' ')}
      {...rest}
    >
      {children}
    </Link>
  );
}
