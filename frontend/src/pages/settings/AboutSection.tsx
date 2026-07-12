import { Card } from '../../components/Card';
import { SettingsRow } from './SettingsRow';
import { APP_VERSION } from './profileData';

/**
 * "Acerca de" (FOR-58 FR/ui.md: "version, terms, privacy"). Version is static
 * build metadata (mirrors `frontend/package.json`'s version at time of
 * writing) — genuinely read-only, not an unsupported flow, so it renders
 * without the "Próximamente" badge. Terms/privacy have no page in this
 * repository yet, so those two rows stay inert entry points.
 */
export function AboutSection() {
  return (
    <Card title="Acerca de FORMA">
      <SettingsRow label="Versión" value={APP_VERSION} />
      <SettingsRow label="Términos y condiciones" inert />
      <SettingsRow label="Política de privacidad" inert />
    </Card>
  );
}
