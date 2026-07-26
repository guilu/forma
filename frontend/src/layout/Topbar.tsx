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

  /**
   * The login action changes place across the breakpoint: on desktop it sits
   * in the bar after the theme toggle, on mobile it belongs inside the
   * disclosure sheet with the section anchors, leaving only the toggle and the
   * hamburger in the bar. Those are two different subtrees, so it is rendered
   * in both and each copy is `display: none` at the other width — exactly one
   * is ever laid out, and `display: none` also removes the other from the
   * accessibility tree, so nothing is announced twice.
   */
  const loginLink = (placement: string) => (
    <Link className={[styles.loginLink, placement].join(' ')} to="/login">
      Iniciar Sesión
    </Link>
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
          <>
            {/*
             * A direct child of `.innerPublic`, not of the actions group: the
             * bar is a three-column grid there, so the anchors centre against
             * the bar itself rather than against whatever the brand and the
             * actions happen to measure.
             */}
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
              {loginLink(styles.loginLinkSheet)}
            </nav>
            <div className={styles.publicSide}>
              {themeToggle}
              {loginLink(styles.loginLinkBar)}
              <button
                className={[styles.iconButton, styles.menuButton].join(' ')}
                type="button"
                aria-label={menuOpen ? 'Cerrar menú' : 'Abrir menú'}
                aria-expanded={menuOpen}
                onClick={() => setMenuOpen((open) => !open)}
              >
                {/*
                 * Three bars rather than swapping a menu/close icon, so the
                 * hamburger morphs into the X instead of cutting to it: the
                 * outer two rotate onto the centre line, the middle one
                 * collapses to zero width. Same construction as the Akadem.ia
                 * navbar. Decorative — the button's aria-label carries the
                 * state.
                 */}
                <span
                  className={[styles.burger, menuOpen ? styles.burgerOpen : '']
                    .filter(Boolean)
                    .join(' ')}
                  aria-hidden="true"
                >
                  <span className={styles.burgerBar} />
                  <span className={styles.burgerBar} />
                  <span className={styles.burgerBar} />
                </span>
              </button>
            </div>
          </>
        )}
      </div>
    </header>
  );
}
