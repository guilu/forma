import { Card } from '../components/Card';
import { IntegrationsSection } from './integrations/IntegrationsSection';
import { ProfileSection } from './settings/ProfileSection';
import { UnitsSection } from './settings/UnitsSection';
import { ObjectivesSection } from './settings/ObjectivesSection';
import { TrainingNutritionSection } from './settings/TrainingNutritionSection';
import { NotificationsSection } from './settings/NotificationsSection';
import { SecuritySection } from './settings/SecuritySection';
import { SupportSection } from './settings/SupportSection';
import { AboutSection } from './settings/AboutSection';
import styles from './SettingsPage.module.css';

/**
 * Configuración / Ajustes screen (FOR-58), reachable at `/ajustes`. Mockup:
 * `docs/8-configuracion.png`. Builds out the previously bare `PagePlaceholder`
 * into the section-based layout the spec asks for, grouped in the same order
 * as the spec's User/System Flow: Perfil y preferencias, Unidades, Conexiones
 * (FOR-57), Objetivos por defecto, Preferencias de entrenamiento y nutrición
 * (FOR-119), Notificaciones (FOR-63 preview), Seguridad y datos, Acerca de.
 * Soporte y ayuda (FOR-115) fills the one section FOR-58's own spec named but
 * did not build, mounted right before Acerca de to match
 * `docs/8-configuracion.png`'s relative order in both its desktop and mobile
 * layouts.
 *
 * <p>FOR-119: {@link ProfileSection} and {@link UnitsSection} now read/write
 * the real FOR-107 profile & preferences backend instead of the mock
 * fixtures FOR-58 shipped with; {@link TrainingNutritionSection} is a new,
 * distinct entry point resolving FOR-58's "folded into ObjectivesSection"
 * deferral. Every other section is either (a) a real, working feature already
 * backed by its own story — {@link IntegrationsSection} (FOR-57) — or (b)
 * read-only display / an inert "Próximamente" entry point; see each section's
 * own doc comment for the specific backend gap it is standing in for.
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
        <TrainingNutritionSection />
        <NotificationsSection />
        <SecuritySection />
        <SupportSection />
        <AboutSection />
      </div>
    </div>
  );
}
