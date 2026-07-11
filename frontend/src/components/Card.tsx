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
 */
interface CardProps {
  readonly title?: string;
  readonly action?: ReactNode;
  readonly children: ReactNode;
  readonly className?: string;
}

export function Card({ title, action, children, className }: CardProps) {
  return (
    <section className={[styles.card, className].filter(Boolean).join(' ')}>
      {(title || action) && (
        <header className={styles.header}>
          {title && <h3 className={styles.title}>{title}</h3>}
          {action}
        </header>
      )}
      {children}
    </section>
  );
}
