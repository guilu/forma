import { Card } from '../../components/Card';
import { NOTIFICATION_PREFERENCES } from './profileData';
import styles from './NotificationsSection.module.css';

/**
 * Notifications preview (FOR-58 FR: entry point to FOR-63). `ui.md` says this
 * section's real toggles belong to FOR-63 ("Notifications section → FOR-63
 * toggles") — so, like the rest of the unsupported flows here, the switches
 * are disabled (`aria-disabled`, native `disabled` on the underlying
 * checkbox) rather than wired to any local/persisted state. They keep the
 * mockup's default-on look purely as a preview of the eventual defaults, not
 * as a working preference (spec FOR-58 Common Pitfall: presenting unsupported
 * options as working).
 */
export function NotificationsSection() {
  return (
    <Card title="Notificaciones" headingLevel={2}>
      <p className={styles.hint}>Próximamente podrás personalizar tus notificaciones aquí.</p>
      <ul className={styles.list}>
        {NOTIFICATION_PREFERENCES.map((preference) => (
          <li key={preference.label} className={styles.row}>
            <div className={styles.text}>
              <span className={styles.label}>{preference.label}</span>
              <span className={styles.description}>{preference.description}</span>
            </div>
            <label className={styles.toggle}>
              <input
                type="checkbox"
                disabled
                defaultChecked={preference.enabledByDefault}
                aria-label={preference.label}
              />
              <span className={styles.track} aria-hidden="true" />
            </label>
          </li>
        ))}
      </ul>
    </Card>
  );
}
