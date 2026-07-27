import { Outlet } from 'react-router-dom';
import { Topbar } from './Topbar';
import styles from './RootLayout.module.css';

/**
 * Global page frame (FOR-185): the navigation bar on top, the routed view
 * underneath. Every user-facing route renders through here — the public
 * landing, the auth pages and the application itself — so the bar is a single
 * persistent element instead of something each area re-declares.
 *
 * <p>Before FOR-185 the top bar was an app-only chrome element that sat
 * *beside* the sidebar (`AppShell` spanned the sidebar across both grid rows),
 * which left `/login` and `/register` with no navigation at all. The frame is
 * now two-tier: this layout owns the bar, and {@link AppShell} — mounted as a
 * child route — owns the second tier below it (sidebar + main, or a fixed
 * bottom bar on small screens).
 *
 * <p>The skip link lives here rather than per page so it is the first
 * focusable element on every route (FOR-61); each routed view provides the
 * `#main-content` landmark it targets.
 *
 * <p>`/onboarding` and `/auth` deliberately stay outside this layout: they are
 * mid-flow screens where navigation would let a user wander off (see
 * `app/routes.tsx`).
 */
export function RootLayout() {
  return (
    <div className={styles.root}>
      <a className={styles.skipLink} href="#main-content">
        Saltar al contenido principal
      </a>
      <Topbar />
      <Outlet />
    </div>
  );
}
