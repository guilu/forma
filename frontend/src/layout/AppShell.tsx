import { useEffect, useRef } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { Topbar } from './Topbar';
import { MobileNav } from './MobileNav';
import styles from './AppShell.module.css';

/**
 * Application shell (FOR-81): the persistent frame — sidebar, top bar and mobile
 * nav — around the routed content rendered through <Outlet />. This is the
 * layout skeleton later feature stories plug their pages into.
 *
 * <p>FOR-61 accessibility hardening:
 * <ul>
 *   <li>A "Saltar al contenido principal" skip link is the first focusable
 *       element on every page, so keyboard users can bypass the repeated
 *       sidebar/topbar navigation instead of tabbing through it on every
 *       route (spec `specs/FOR-61/spec.md`: "keyboard navigation for core
 *       flows … without a mouse").
 *   <li>The `<main>` landmark receives focus on every client-side route
 *       change (not on first mount — see the `isFirstRender` guard). React
 *       Router does not move focus on navigation by itself, so without this
 *       a keyboard/screen-reader user's focus would silently stay on
 *       whatever sidebar link they just activated while the page content
 *       changed underneath them (spec edge case: "focus management on route
 *       changes").
 * </ul>
 */
export function AppShell() {
  const mainRef = useRef<HTMLElement>(null);
  const isFirstRender = useRef(true);
  const location = useLocation();

  useEffect(() => {
    if (isFirstRender.current) {
      // Skip the very first mount: initial page load should keep the
      // browser's normal landing focus, not immediately steal it into <main>.
      isFirstRender.current = false;
      return;
    }
    mainRef.current?.focus();
  }, [location.pathname]);

  return (
    <div className={styles.shell}>
      <a className={styles.skipLink} href="#main-content">
        Saltar al contenido principal
      </a>
      <Sidebar />
      <Topbar />
      <main id="main-content" ref={mainRef} tabIndex={-1} className={styles.content}>
        <Outlet />
      </main>
      <MobileNav />
    </div>
  );
}
