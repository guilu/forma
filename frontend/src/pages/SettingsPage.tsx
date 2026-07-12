import { Card } from '../components/Card';
import { IntegrationsSection } from './integrations/IntegrationsSection';
import { ProfileSection } from './settings/ProfileSection';
import { UnitsSection } from './settings/UnitsSection';
import { ObjectivesSection } from './settings/ObjectivesSection';
import { NotificationsSection } from './settings/NotificationsSection';
import { SecuritySection } from './settings/SecuritySection';
import { AboutSection } from './settings/AboutSection';
import styles from './SettingsPage.module.css';

/**
 * Configuración / Ajustes screen (FOR-58), reachable at `/ajustes`. Mockup:
 * `docs/8-configuracion.png`. Builds out the previously bare `PagePlaceholder`
 * into the section-based layout the spec asks for, grouped in the same order
 * as the spec's User/System Flow: Perfil y preferencias, Unidades, Conexiones
 * (FOR-57), Objetivos por defecto, Notificaciones (FOR-63 preview), Seguridad
 * y datos, Acerca de.
 *
 * <p><b>No user/profile/preferences backend exists yet</b> (ADR-002,
 * single-user MVP — verified against `backend/src/main/java/.../delivery/**`).
 * Every section here is either (a) a real, working feature already backed by
 * its own story — only {@link IntegrationsSection} (FOR-57) qualifies, reused
 * unmodified and composed directly since it "takes no route-specific props"
 * — or (b) read-only display / an inert "Próximamente" entry point. Nothing
 * in this screen invents profile persistence, password changes, 2FA, or
 * data export/import; see each section's own doc comment for the specific
 * backend gap it is standing in for.
 *
 * <p>Responsive behavior follows the same CSS-grid, mobile-first pattern as
 * `DashboardPage`/`ProgressPage`: sections stack into a single scrollable
 * column on narrow viewports and flow into multiple columns on wider ones —
 * satisfying the spec's "mobile: sections collapse into a scrollable list"
 * without a second, drill-down navigation layer (out of scope for FOR-58: the
 * spec's UI notes ask for a scrollable list, not a distinct mobile
 * information architecture).
 */
export function SettingsPage() {
  return (
    <div className={styles.wrapper}>
      <header className={styles.header}>
        <h1 className={styles.title}>Configuración</h1>
        <p className={styles.subtitle}>Personaliza tu experiencia en FORMA.</p>
      </header>

      <div className={styles.grid}>
        <ProfileSection />
        <UnitsSection />
        <Card>
          <IntegrationsSection />
        </Card>
        <ObjectivesSection />
        <NotificationsSection />
        <SecuritySection />
        <AboutSection />
      </div>
    </div>
  );
}
