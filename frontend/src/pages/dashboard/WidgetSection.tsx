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
  readonly children: ReactNode;
}

export function WidgetSection({
  id,
  title,
  linkTo,
  linkLabel = 'Ver más',
  children,
}: WidgetSectionProps) {
  return (
    <section className={styles.widget} aria-labelledby={id}>
      <div className={styles.header}>
        <h2 id={id} className={styles.title}>
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
