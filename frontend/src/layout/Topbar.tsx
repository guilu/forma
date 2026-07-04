import { Brand } from '../components/Brand';
import { Icon } from '../components/Icon';
import styles from './Topbar.module.css';

/**
 * Top application bar (FOR-81). Shows the brand on mobile (where the sidebar is
 * hidden) and a notifications affordance. It intentionally renders no user
 * identity or data — account/session handling belongs to later stories.
 */
export function Topbar() {
  return (
    <header className={styles.topbar}>
      <div className={styles.mobileBrand}>
        <Brand />
      </div>
      <div className={styles.actions}>
        <button className={styles.iconButton} type="button" aria-label="Notificaciones">
          <Icon name="bell" />
        </button>
      </div>
    </header>
  );
}
