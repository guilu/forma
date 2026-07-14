import { Card } from '../../components/Card';
import { SettingsRow } from './SettingsRow';

/**
 * "Soporte y ayuda" (FOR-115). FOR-58's own spec listed this section in its
 * User/System Flow but did not build it (`docs/8-configuracion.png` shows
 * it; FOR-58's implementation stopped at "Acerca de"). This fills that
 * specific gap using the same static/link-only pattern as {@link
 * AboutSection}, its direct sibling.
 *
 * <p>Mockup entries: "Centro de ayuda", "Contactar con soporte", "Enviar
 * sugerencia". None of them has a backing page, support channel or feedback
 * endpoint in this repository yet, so every row renders `inert` — same
 * precedent as `AboutSection`'s terms/privacy rows (AGENTS.md: "never shows
 * unsupported options as active").
 */
export function SupportSection() {
  return (
    <Card title="Soporte y ayuda" headingLevel={2}>
      <SettingsRow label="Centro de ayuda" inert />
      <SettingsRow label="Contactar con soporte" inert />
      <SettingsRow label="Enviar sugerencia" inert />
    </Card>
  );
}
