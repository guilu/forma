import { IntegrationsSection } from '../../integrations/IntegrationsSection';
import styles from './steps.module.css';

/**
 * Integration prompt step (FOR-59 FR: "optional connect (FOR-57)"). Reuses
 * {@link IntegrationsSection} wholesale rather than rebuilding a slimmer
 * connect prompt — that component already handles the connected/available
 * lists, safe error display and the documented "no backend yet" behavior
 * (FOR-57), so embedding it here keeps this step honest about what is and
 * isn't wired up, without duplicating that logic.
 */
export function IntegrationStep() {
  return (
    <div className={styles.field}>
      <p className={styles.intro}>
        Conecta un proveedor para sincronizar tus datos automáticamente. Es opcional y puedes
        hacerlo más tarde desde Ajustes.
      </p>
      <IntegrationsSection />
    </div>
  );
}
