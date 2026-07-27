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
 *       the browser scrolls in place, and from `/login` or `/register` it
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
  const [accountOpen, setAccountOpen] = useState(false);

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
        {/*
         * Always the landing, never `/app`. The brand is the way back out of
         * the application to the public site; pointing it at the dashboard
         * while signed in would leave a signed-in user with no route to the
         * landing at all.
         */}
        <Link className={styles.brand} to="/" aria-label="FORMA, inicio">
          <Brand />
        </Link>

        {isAuthenticated ? (
          <div className={styles.actions}>
            {themeToggle}
            <button className={styles.iconButton} type="button" aria-label="Notificaciones">
              <Icon name="bell" />
            </button>
            {/*
             * The account collapses into a single avatar trigger plus a menu,
             * rather than laying the email and a logout button out in the bar.
             * On mobile that is the only shape that fits beside the theme and
             * notification controls; the avatar is also where a real profile
             * photo will go once there is one to show.
             *
             * `onBlur` on the wrapper closes the menu when focus leaves it
             * entirely — `relatedTarget` is the element receiving focus, so a
             * move *within* the menu (trigger -> item) is ignored. That covers
             * tab-away and, with the Escape handler, gives the menu the two
             * dismissals a keyboard user expects. A click on inert page area
             * does not close it, matching the "Más" disclosure below.
             */}
            <div
              className={styles.account}
              onBlur={(event) => {
                if (!event.currentTarget.contains(event.relatedTarget)) setAccountOpen(false);
              }}
              onKeyDown={(event) => {
                if (event.key === 'Escape') setAccountOpen(false);
              }}
            >
              <button
                className={styles.accountTrigger}
                type="button"
                aria-haspopup="menu"
                aria-expanded={accountOpen}
                aria-label={user?.email ? `Cuenta: ${user.email}` : 'Cuenta'}
                onClick={() => setAccountOpen((open) => !open)}
              >
                <span className={styles.avatar} aria-hidden="true">
                  <Icon name="user" size={18} />
                </span>
                <span className={styles.accountName} aria-hidden="true">
                  {user?.email}
                </span>
              </button>
              {accountOpen && (
                <div className={styles.accountMenu} role="menu" aria-label="Cuenta">
                  {/*
                   * "Ajustes" lives here rather than in the section navigation
                   * (see app/navigation.ts): it is account chrome, not a
                   * product section, so it belongs next to signing out.
                   */}
                  <Link
                    className={styles.accountMenuItem}
                    role="menuitem"
                    to="/app/settings"
                    onClick={() => setAccountOpen(false)}
                  >
                    <Icon name="settings" size={18} />
                    Ajustes
                  </Link>
                  <button
                    className={styles.accountMenuItem}
                    role="menuitem"
                    type="button"
                    disabled={logoutPending}
                    aria-busy={logoutPending || undefined}
                    onClick={() => void handleLogout()}
                  >
                    <Icon name="logout" size={18} />
                    {logoutPending
                      ? 'Cerrando sesión...'
                      : logoutError
                        ? 'Reintentar cierre de sesión'
                        : 'Cerrar sesión'}
                  </button>
                </div>
              )}
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
              <a className={styles.publicLink} href="/#training">
                Entrenamiento
              </a>
              <a className={styles.publicLink} href="/#nutrition">
                Nutrición
              </a>
              <a className={styles.publicLink} href="/#plans">
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
