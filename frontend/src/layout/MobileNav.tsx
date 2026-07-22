import { useState } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import { NAV_ITEMS } from '../app/navigation';
import { Icon } from '../components/Icon';
import styles from './MobileNav.module.css';

/**
 * Compact bottom navigation for small screens (FOR-81 / FOR-49), mirroring the
 * mobile frame in the mockups. Shows the primary sections plus a "Más" overflow
 * so every MVP section is reachable from navigation on mobile — not just by URL.
 *
 * The primary bar is limited to four sections (Dashboard, Mediciones,
 * Entrenamiento, Nutrición); the secondary items (Lista de compra, Progreso,
 * Objetivos, Ajustes) live behind a "Más" disclosure that expands above the bar
 * and collapses on selection.
 */
export function MobileNav() {
  const primary = NAV_ITEMS.filter((item) => item.primary);
  const secondary = NAV_ITEMS.filter((item) => !item.primary);
  const [moreOpen, setMoreOpen] = useState(false);
  const location = useLocation();

  const secondaryActive = secondary.some((item) => item.path === location.pathname);
  const linkClass = ({ isActive }: { isActive: boolean }) =>
    [styles.link, isActive ? styles.active : ''].filter(Boolean).join(' ');

  return (
    <nav className={styles.mobileNav} aria-label="Navegación principal">
      {moreOpen && (
        <div className={styles.moreMenu} role="menu" aria-label="Más secciones">
          {secondary.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              role="menuitem"
              className={linkClass}
              onClick={() => setMoreOpen(false)}
            >
              <Icon name={item.icon} size={20} />
              <span className={styles.moreLabel}>{item.label}</span>
            </NavLink>
          ))}
        </div>
      )}

      <div className={styles.bar}>
        {primary.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            end={item.path === '/'}
            className={linkClass}
            onClick={() => setMoreOpen(false)}
          >
            <Icon name={item.icon} size={22} />
            <span className={styles.label}>{item.label}</span>
          </NavLink>
        ))}
        <button
          type="button"
          className={[styles.link, styles.moreButton, secondaryActive ? styles.active : '']
            .filter(Boolean)
            .join(' ')}
          aria-haspopup="menu"
          aria-expanded={moreOpen}
          onClick={() => setMoreOpen((open) => !open)}
        >
          <Icon name="more" size={22} />
          <span className={styles.label}>Más</span>
        </button>
      </div>
    </nav>
  );
}
