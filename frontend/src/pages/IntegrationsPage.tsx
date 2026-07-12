import { IntegrationsSection } from './integrations/IntegrationsSection';
import styles from './IntegrationsPage.module.css';

/**
 * Integrations management screen (FOR-57), reachable at `/ajustes/integraciones`.
 *
 * <p>The spec's Open Question ("integrations live inside Ajustes (FOR-58) or a
 * dedicated route") is resolved here: FOR-58's Ajustes shell is still a
 * `PagePlaceholder` (`frontend/src/pages/SettingsPage.tsx`), so this screen
 * cannot live *inside* it yet without building FOR-58 early (out of scope).
 * Instead it is a standalone sub-route under `/ajustes/*`, matching the
 * mockup's placement conceptually while staying reachable today; the
 * dashboard's `SyncWidget` "Ver más" link points here. When FOR-58 implements
 * the real Ajustes shell, {@link IntegrationsSection} can be composed directly
 * inside it — it takes no route-specific props.
 */
export function IntegrationsPage() {
  return (
    <div className={styles.wrapper}>
      <header className={styles.header}>
        <h1 className={styles.title}>Integraciones</h1>
        <p className={styles.subtitle}>Gestiona tus conexiones con proveedores externos.</p>
      </header>
      <IntegrationsSection />
    </div>
  );
}
