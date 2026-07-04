import { NavLink } from 'react-router-dom';
import { NAV_ITEMS } from '../app/navigation';
import { Icon } from '../components/Icon';
import styles from './MobileNav.module.css';

/**
 * Compact bottom navigation for small screens (FOR-81), mirroring the mobile
 * frame in docs/mockup.png. Shows only the primary sections from NAV_ITEMS so
 * the bar stays readable; the rest remain reachable by URL until a later story
 * adds a "more" menu.
 */
export function MobileNav() {
  const primary = NAV_ITEMS.filter((item) => item.primary);

  return (
    <nav className={styles.mobileNav} aria-label="Navegación principal">
      {primary.map((item) => (
        <NavLink
          key={item.path}
          to={item.path}
          end={item.path === '/'}
          className={({ isActive }) =>
            [styles.link, isActive ? styles.active : ''].filter(Boolean).join(' ')
          }
        >
          <Icon name={item.icon} size={22} />
          <span className={styles.label}>{item.label}</span>
        </NavLink>
      ))}
    </nav>
  );
}
