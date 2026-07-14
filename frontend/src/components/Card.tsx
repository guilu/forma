import type { ReactNode } from 'react';
import styles from './Card.module.css';

/**
 * Reusable surface primitive (FOR-81, extended FOR-50). ADR-006 calls for
 * reusable cards as a first-class building block; feature stories compose their
 * content inside this instead of restyling surfaces per page. Carries no domain
 * data itself.
 *
 * <p>{@link CardProps.action} adds the "title + optional action" section-header
 * pattern used by the mockups (e.g. "EVOLUCIÓN DE PESO", "COMIDAS DEL DÍA"), so
 * consumers like {@link ChartContainer} don't have to reimplement the header row.
 *
 * <p>{@link CardProps.headingLevel} (FOR-112) lets a call site pick the
 * semantic heading tag for `title` so the page's overall heading order stays
 * non-skipping regardless of how deep the Card is nested. Defaults to `3`,
 * matching the tag every Card has always rendered, so existing call sites are
 * unaffected unless they opt into an explicit level.
 */
export type HeadingLevel = 2 | 3 | 4 | 5 | 6;

const HEADING_TAGS: Record<HeadingLevel, 'h2' | 'h3' | 'h4' | 'h5' | 'h6'> = {
  2: 'h2',
  3: 'h3',
  4: 'h4',
  5: 'h5',
  6: 'h6',
};

interface CardProps {
  readonly title?: string;
  readonly headingLevel?: HeadingLevel;
  readonly action?: ReactNode;
  readonly children: ReactNode;
  readonly className?: string;
}

export function Card({ title, headingLevel = 3, action, children, className }: CardProps) {
  const HeadingTag = HEADING_TAGS[headingLevel];
  return (
    <section className={[styles.card, className].filter(Boolean).join(' ')}>
      {(title || action) && (
        <header className={styles.header}>
          {title && <HeadingTag className={styles.title}>{title}</HeadingTag>}
          {action}
        </header>
      )}
      {children}
    </section>
  );
}
