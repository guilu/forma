import { Icon } from '../../components/Icon';
import { StatusPill } from '../../components/StatusPill';
import { WidgetSection } from './WidgetSection';
import styles from './SyncWidget.module.css';

/**
 * Integration sync status widget (FOR-51): "Withings · Conectado" summary from the
 * mockup. There is no backend integrations endpoint yet (no controller, no persisted
 * connection state — verified by inspecting `backend/src/main/java`), so this is a
 * static placeholder rather than a live check, matching the same static "Withings ·
 * Conectado" chip already shipped in `frontend/src/layout/Sidebar.tsx` (FOR-81). It is
 * intentionally not wired to any fetch — there is nothing to load, and no loading/
 * error state applies. Documented gap, see FOR-51 PR "Known limitations".
 *
 * <p>"Ver más" now points at `/ajustes/integraciones` (FOR-57's standalone
 * integrations screen) instead of the still-placeholder `/ajustes` — this
 * widget's own data stays the static FOR-51 chip; only the link target moved.
 */
export function SyncWidget() {
  return (
    <WidgetSection id="sync-widget-title" title="Integraciones" linkTo="/ajustes/integraciones">
      <div className={styles.card}>
        <Icon name="heart" size={20} />
        <span className={styles.name}>Withings</span>
        <StatusPill kind="connection" value="Conectado" />
      </div>
    </WidgetSection>
  );
}
