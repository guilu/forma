import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import styles from './WidgetSection.module.css';

/**
 * Structural wrapper shared by every dashboard widget (FOR-51). Gives each widget a
 * labelled `<section>` with a heading (spec `specs/FOR-51/ui.md` accessibility
 * requirement) and an optional "Ver más" link to the widget's feature page, without
 * re-wrapping widget content in a nested `Card` (widgets already compose `Card`/
 * `MetricCard` internally for their own surfaces). Page-local — not part of the FOR-50
 * shared design system, since only the dashboard needs this exact heading+link header.
 */
interface WidgetSectionProps {
  readonly id: string;
  readonly title: string;
  readonly linkTo?: string;
  readonly linkLabel?: string;
  /**
   * When true the heading stays in the accessibility tree (so heading order
   * never skips a level) but is visually hidden — for mockup rows that group
   * tiles without a visible section title (FOR-164 metrics row).
   */
  readonly titleHidden?: boolean;
  /**
   * When true (default) the section renders as a bordered card surface, as in
   * the FOR-164 mockup where each dashboard panel is its own card. Set false
   * for rows that only group already-carded tiles (the metrics row), so the
   * card surface isn't doubled.
   */
  readonly surface?: boolean;
  readonly children: ReactNode;
}

export function WidgetSection({
  id,
  title,
  linkTo,
  linkLabel = 'Ver más',
  titleHidden = false,
  surface = true,
  children,
}: WidgetSectionProps) {
  return (
    <section
      className={[styles.widget, surface ? styles.surface : ''].filter(Boolean).join(' ')}
      aria-labelledby={id}
    >
      <div className={titleHidden ? styles.headerHidden : styles.header}>
        <h2 id={id} className={titleHidden ? styles.srOnly : styles.title}>
          {title}
        </h2>
        {linkTo && (
          <Link className={styles.link} to={linkTo}>
            {linkLabel}
          </Link>
        )}
      </div>
      {children}
    </section>
  );
}
