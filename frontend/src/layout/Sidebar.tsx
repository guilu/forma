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
    </aside>
  );
}
