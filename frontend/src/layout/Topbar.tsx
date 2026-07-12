import { Brand } from '../components/Brand';
import { Icon } from '../components/Icon';
import { useTheme } from '../theme/ThemeContext';
import styles from './Topbar.module.css';

/**
 * Top application bar (FOR-81 / FOR-49). Shows the brand on mobile (where the
 * sidebar is hidden), a quick light/dark theme toggle, a notifications
 * affordance and a static account area.
 *
 * <p>The theme toggle (FOR-62) is a compact icon button that flips between light
 * and dark; the full light/dark/system control lives in Ajustes. The account
 * area is a placeholder: FORMA is a single-user MVP (ADR-002) with no auth yet,
 * so it renders identity as static presentational content — not an interactive
 * menu — and is the seam a future auth story wires real session state into. The
 * notification bell is likewise a visual entry until FOR-63.
 */
export function Topbar() {
  const { resolvedTheme, setMode } = useTheme();
  const isDark = resolvedTheme === 'dark';

  return (
    <header className={styles.topbar}>
      <div className={styles.mobileBrand}>
        <Brand />
      </div>
      <div className={styles.actions}>
        <button
          className={styles.iconButton}
          type="button"
          aria-label={isDark ? 'Cambiar a tema claro' : 'Cambiar a tema oscuro'}
          onClick={() => setMode(isDark ? 'light' : 'dark')}
        >
          <Icon name={isDark ? 'sun' : 'moon'} />
        </button>
        <button className={styles.iconButton} type="button" aria-label="Notificaciones">
          <Icon name="bell" />
        </button>
        <div className={styles.account}>
          <span className={styles.avatar} aria-hidden="true">
            <Icon name="user" size={18} />
          </span>
          <span className={styles.accountName}>Diego</span>
        </div>
      </div>
    </header>
  );
}
