import { Outlet } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { Topbar } from './Topbar';
import { MobileNav } from './MobileNav';
import styles from './AppShell.module.css';

/**
 * Application shell (FOR-81): the persistent frame — sidebar, top bar and mobile
 * nav — around the routed content rendered through <Outlet />. This is the
 * layout skeleton later feature stories plug their pages into.
 */
export function AppShell() {
  return (
    <div className={styles.shell}>
      <Sidebar />
      <Topbar />
      <main className={styles.content}>
        <Outlet />
      </main>
      <MobileNav />
    </div>
  );
}
