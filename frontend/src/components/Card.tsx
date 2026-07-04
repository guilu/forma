import type { ReactNode } from 'react';
import styles from './Card.module.css';

/**
 * Reusable surface primitive (FOR-81). ADR-006 calls for reusable cards as a
 * first-class building block; feature stories compose their content inside this
 * instead of restyling surfaces per page. Carries no domain data itself.
 */
interface CardProps {
  readonly title?: string;
  readonly children: ReactNode;
  readonly className?: string;
}

export function Card({ title, children, className }: CardProps) {
  return (
    <section className={[styles.card, className].filter(Boolean).join(' ')}>
      {title && <h3 className={styles.title}>{title}</h3>}
      {children}
    </section>
  );
}
