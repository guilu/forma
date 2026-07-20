import { NavLink } from 'react-router-dom';
import { NAV_ITEMS } from '../app/navigation';
import { Brand } from '../components/Brand';
import { Icon } from '../components/Icon';
import styles from './Sidebar.module.css';

/**
 * Desktop sidebar navigation (FOR-81). Renders from the centralized NAV_ITEMS
 * model; the active route is highlighted via NavLink. Hidden on small screens,
 * where MobileNav takes over.
 */
export function Sidebar() {
  return (
    <aside className={styles.sidebar}>
      <div className={styles.brand}>
        <Brand />
      </div>
      <nav className={styles.nav} aria-label="Navegación principal">
        {NAV_ITEMS.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            end={item.path === '/'}
            className={({ isActive }) =>
              [styles.link, isActive ? styles.active : ''].filter(Boolean).join(' ')
            }
          >
            <Icon name={item.icon} />
            <span>{item.label}</span>
          </NavLink>
        ))}
      </nav>
      <div className={styles.integration}>
        <div className={styles.integrationHeader}>
          <span className={styles.integrationLabel}>WITHINGS</span>
          {/* Static status dot, not a live indicator. A real "sincronizado hace
              X" timestamp needs the integrations sync backend, which does not
              exist yet — follow-up story, not part of this change. */}
          <span className={styles.integrationDot} aria-hidden="true" />
        </div>
        {/* "Conectado" reflects the current mock/static integration state only;
            do not read it as a live sync confirmation (see comment above). */}
        <p className={styles.integrationStatus}>Conectado</p>
      </div>
    </aside>
  );
}
