import { Card } from '../../components/Card';
import { SettingsRow } from './SettingsRow';
import { SECURITY_ACTIONS } from './profileData';

/**
 * Security & data (FOR-58 FR: "change password, 2FA, delete account" +
 * "data export/import entry points"). `ui.md` groups these under one
 * "Security & data" component, so this section covers both — no backend
 * exists for authentication changes, 2FA or data export/import (ADR-002 is
 * about authN/authZ *existing*, not about these specific self-service flows
 * being implemented yet), so every row here is an inert entry point.
 */
export function SecuritySection() {
  return (
    <Card title="Seguridad y datos">
      {SECURITY_ACTIONS.map((action) => (
        <SettingsRow
          key={action.label}
          label={action.label}
          description={action.description}
          inert
        />
      ))}
    </Card>
  );
}
