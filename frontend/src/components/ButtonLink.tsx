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
 * <p>Pages that need to *resize* the button pass a `className` and double the
 * selector in their own module (`.cta.cta { … }`). Doubling is the part that
 * matters: a plain `.cta` sits at the same (0,1,0) as the `.button` it is
 * overriding, so the winner comes down to which module lands later in the
 * bundle.
 *
 * <p>Do NOT reach for `composes` to do this. It looks like the tidier option
 * and it does not order anything — it adds `.button` alongside the composing
 * class rather than ranking that class above it, leaving the same (0,1,0) tie.
 * The topbar's login action did exactly that and silently lost every one of its
 * overrides, rendering ten pixels taller than the control it pairs with, while
 * the identical arrangement elsewhere happened to win. `composes` is also only
 * valid on a simple selector, so a composing class cannot be doubled to fix it.
 * Use `composes` only where nothing has to beat the base — an external `<a>`
 * that cannot be a `ButtonLink` at all — and put any overrides in a second,
 * doubled class applied alongside it.
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
