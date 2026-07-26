import { Link } from 'react-router-dom';
import { PagePlaceholder } from '../components/PagePlaceholder';
import styles from './NotFoundPage.module.css';

export function NotFoundPage() {
  return (
    <div>
      <PagePlaceholder title="Página no encontrada" description="La ruta solicitada no existe." />
      <p style={{ marginTop: 'var(--space-4)' }}>
        <Link to="/app">Volver al Dashboard</Link>
      </p>
    </div>
  );
}

/**
 * FOR-185: the same not-found content for unknown routes *outside* `/app`.
 * Inside the app it renders through `AppShell`, which already provides the
 * `<main id="main-content">` landmark the skip link targets; at the public
 * level there is no shell between `RootLayout` and the page, so this variant
 * supplies that landmark itself.
 */
export function PublicNotFoundPage() {
  return (
    <main id="main-content" tabIndex={-1} className={styles.publicMain}>
      <NotFoundPage />
    </main>
  );
}
