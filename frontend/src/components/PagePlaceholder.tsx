import { Card } from './Card';
import styles from './PagePlaceholder.module.css';

/**
 * Standard "not built yet" placeholder (FOR-81). Every routed section renders
 * this until its owning story implements real content. It deliberately shows no
 * metrics or user data — the skeleton must not fake business logic.
 */
interface PagePlaceholderProps {
  readonly title: string;
  readonly description?: string;
}

export function PagePlaceholder({ title, description }: PagePlaceholderProps) {
  return (
    <div className={styles.wrapper}>
      <header className={styles.header}>
        <h1 className={styles.title}>{title}</h1>
      </header>
      <Card>
        <p className={styles.description}>
          {description ?? 'Esta sección se implementará en una historia posterior.'}
        </p>
        <p className={styles.hint}>
          El esqueleto de la aplicación (FOR-81) provee navegación, layout y tema. El contenido de
          esta pantalla aún no está disponible.
        </p>
      </Card>
    </div>
  );
}
