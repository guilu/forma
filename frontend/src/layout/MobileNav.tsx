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
 * The primary bar is four sections (Dashboard, Mediciones, Entrenamiento,
 * Nutrición); the rest live behind a "Más" disclosure that expands above the bar
 * and collapses on selection.
 *
 * <p>The bar shows glyphs and no labels. At four sections plus the disclosure
 * the words no longer fit — they clipped and wrapped — and a broken word under
 * an icon is worse than no word at all. Nothing is lost to assistive tech: each
 * control carries its section name as its accessible name, and `title` shows
 * the same word on a long press or a hover. The disclosure keeps its labels,
 * since it is a list with room for them.
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
            end={item.path === '/app'}
            className={linkClass}
            aria-label={item.label}
            title={item.label}
            onClick={() => setMoreOpen(false)}
          >
            <Icon name={item.icon} size={26} />
          </NavLink>
        ))}
        <button
          type="button"
          className={[styles.link, styles.moreButton, secondaryActive ? styles.active : '']
            .filter(Boolean)
            .join(' ')}
          aria-haspopup="menu"
          aria-expanded={moreOpen}
          aria-label="Más secciones"
          title="Más secciones"
          onClick={() => setMoreOpen((open) => !open)}
        >
          <Icon name="more" size={26} />
        </button>
      </div>
    </nav>
  );
}
