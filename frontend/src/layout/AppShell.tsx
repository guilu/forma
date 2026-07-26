import { useEffect, useRef } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { MobileNav } from './MobileNav';
import styles from './AppShell.module.css';

/**
 * Application frame (FOR-81): the second tier of the layout, mounted under the
 * global navigation bar owned by {@link RootLayout}.
 *
 * <p>FOR-185 reshaped this. It used to own the top bar too, spanning the
 * sidebar across both grid rows so the bar sat *beside* the navigation; the
 * frame is now strictly "bar on top, content below", and this component is
 * only the content half: the sidebar aside and the routed `<main>` (on small
 * screens the aside is replaced by the fixed bottom {@link MobileNav}). The
 * skip link moved up to `RootLayout` for the same reason — one per document,
 * not one per area.
 *
 * <p>FOR-61 accessibility hardening: the `<main>` landmark receives focus on
 * every client-side route change (not on first mount — see the
 * `isFirstRender` guard). React Router does not move focus on navigation by
 * itself, so without this a keyboard/screen-reader user's focus would silently
 * stay on whatever sidebar link they just activated while the page content
 * changed underneath them (spec edge case: "focus management on route
 * changes").
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
      <Sidebar />
      <main id="main-content" ref={mainRef} tabIndex={-1} className={styles.content}>
        <Outlet />
      </main>
      <MobileNav />
    </div>
  );
}
