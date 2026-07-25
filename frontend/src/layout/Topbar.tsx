import { useState } from 'react';
import { Brand } from '../components/Brand';
import { Icon } from '../components/Icon';
import { useTheme } from '../theme/ThemeContext';
import { useAuth } from '../auth/AuthContext';
import styles from './Topbar.module.css';

/**
 * Top application bar (FOR-81 / FOR-49). Shows the brand on mobile (where the
 * sidebar is hidden), a quick light/dark theme toggle, a notifications
 * affordance and the authenticated account controls.
 *
 * <p>The theme toggle (FOR-62) is a compact icon button that flips between light
 * and dark; the full light/dark/system control lives in Ajustes. The account
 * area reads the server-backed session from the FOR-145d auth boundary and
 * exposes a keyboard-operable logout action. The notification bell remains a
 * visual entry until its owning feature provides a destination.
 */
export function Topbar() {
  const { resolvedTheme, setMode } = useTheme();
  const { user, logout } = useAuth();
  const isDark = resolvedTheme === 'dark';
  const [logoutPending, setLogoutPending] = useState(false);
  const [logoutError, setLogoutError] = useState(false);

  async function handleLogout() {
    setLogoutPending(true);
    setLogoutError(false);
    try {
      await logout();
    } catch {
      setLogoutError(true);
    } finally {
      setLogoutPending(false);
    }
  }

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
          <span className={styles.accountName}>{user?.email}</span>
          <button
            className={styles.logoutButton}
            type="button"
            disabled={logoutPending}
            aria-busy={logoutPending || undefined}
            onClick={() => void handleLogout()}
          >
            {logoutPending
              ? 'Cerrando sesión...'
              : logoutError
                ? 'Reintentar cierre de sesión'
                : 'Cerrar sesión'}
          </button>
        </div>
        {logoutError && (
          <p className={styles.logoutError} role="alert">
            No se pudo cerrar la sesión. Inténtalo de nuevo.
          </p>
        )}
      </div>
    </header>
  );
}
