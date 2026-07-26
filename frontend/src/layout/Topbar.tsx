import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Brand } from '../components/Brand';
import { Icon } from '../components/Icon';
import { useTheme } from '../theme/ThemeContext';
import { useAuth } from '../auth/AuthContext';
import styles from './Topbar.module.css';

/**
 * Global navigation bar (FOR-81 / FOR-49, reshaped by FOR-185).
 *
 * <p>FOR-185 promoted this from app-only chrome to the single bar rendered
 * above every route by {@link RootLayout}, so it now has two faces driven by
 * the session:
 * <ul>
 *   <li><b>Anonymous</b> — the public bar from `docs/0-landing.html`: the
 *       brand, the section anchors and an outlined "Iniciar sesión" action.
 *       The anchors are plain `<a href="/#...">` rather than router links
 *       because they target fragments of the landing page: from the landing
 *       the browser scrolls in place, and from `/login` or `/registro` it
 *       navigates to the landing and lands on the section.
 *   <li><b>Authenticated</b> — the account controls it already had: the
 *       theme toggle, the notifications affordance and the logout action.
 * </ul>
 * The theme toggle (FOR-62) is shown in both: it is a display preference, not
 * an account feature, and the public landing offered one before FOR-185 too.
 *
 * <p>Below the `md` breakpoint the public anchors collapse behind a menu
 * disclosure, matching the template's `md:hidden` hamburger.
 */
export function Topbar() {
  const { resolvedTheme, setMode } = useTheme();
  const { status, user, logout } = useAuth();
  const isDark = resolvedTheme === 'dark';
  const isAuthenticated = status === 'authenticated';
  const [logoutPending, setLogoutPending] = useState(false);
  const [logoutError, setLogoutError] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);

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

  const themeToggle = (
    <button
      className={styles.iconButton}
      type="button"
      aria-label={isDark ? 'Cambiar a tema claro' : 'Cambiar a tema oscuro'}
      onClick={() => setMode(isDark ? 'light' : 'dark')}
    >
      <Icon name={isDark ? 'sun' : 'moon'} />
    </button>
  );

  return (
    <header className={styles.topbar}>
      {/*
       * The public bar centres its content in the template's `max-w-7xl`
       * measure; the application bar spans the full width so it stays aligned
       * with the sidebar edge below it.
       */}
      <div
        className={[styles.inner, isAuthenticated ? styles.innerApp : styles.innerPublic].join(' ')}
      >
        <Link
          className={styles.brand}
          to={isAuthenticated ? '/app' : '/'}
          aria-label="FORMA, inicio"
        >
          <Brand />
        </Link>

        {isAuthenticated ? (
          <div className={styles.actions}>
            {themeToggle}
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
        ) : (
          <div className={styles.publicSide}>
            <nav
              className={[styles.publicNav, menuOpen ? styles.publicNavOpen : '']
                .filter(Boolean)
                .join(' ')}
              aria-label="Navegación pública"
            >
              <a className={styles.publicLink} href="/#entrenamiento">
                Entrenamiento
              </a>
              <a className={styles.publicLink} href="/#nutricion">
                Nutrición
              </a>
              <a className={styles.publicLink} href="/#planes">
                Planes
              </a>
              <Link className={styles.loginLink} to="/login">
                Iniciar Sesión
              </Link>
            </nav>
            {themeToggle}
            <button
              className={[styles.iconButton, styles.menuButton].join(' ')}
              type="button"
              aria-label="Abrir menú"
              aria-expanded={menuOpen}
              onClick={() => setMenuOpen((open) => !open)}
            >
              <Icon name="menu" />
            </button>
          </div>
        )}
      </div>
    </header>
  );
}
