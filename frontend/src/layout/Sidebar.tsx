import { Link, NavLink } from 'react-router-dom';
import { NAV_ITEMS } from '../app/navigation';
import { Icon } from '../components/Icon';
import { useIntegrations } from '../integrations/IntegrationsContext';
import styles from './Sidebar.module.css';

/**
 * Desktop sidebar navigation (FOR-81). Renders from the centralized NAV_ITEMS
 * model; the active route is highlighted via NavLink. Hidden on small screens,
 * where MobileNav takes over.
 *
 * <p>FOR-185 dropped the brand lockup from the top of this aside: the global
 * navigation bar now sits above the sidebar rather than beside it, so a brand
 * here would be a second copy directly under the first.
 *
 * <p>The Withings card reads the real connection state (FOR-126's
 * `GET /api/v1/integrations`). It used to print "Conectado" unconditionally —
 * written before that endpoint existed — which contradicted the settings screen
 * and, for anyone who had never connected anything, plain reality.
 *
 * <p>It reads that state from the shared {@link useIntegrations} store rather
 * than fetching its own copy (FOR-189): with two copies, disconnecting in
 * settings left this card claiming "Conectado" until a full reload. While the
 * state is unknown — in flight, or the request failed — the card is not
 * rendered at all: a status indicator that cannot read the status is worse than
 * absent, and the navigation must not grow an error banner because a secondary
 * card could not load.
 */
export function Sidebar({
  expanded = false,
  onExpandedChange,
}: {
  readonly expanded?: boolean;
  readonly onExpandedChange?: (expanded: boolean) => void;
}) {
  const { status, connections } = useIntegrations();
  const withings =
    status === 'ready' ? connections.find((c) => c.providerId === 'WITHINGS') : undefined;

  const renderLink = (item: (typeof NAV_ITEMS)[number]) => (
    <NavLink
      key={item.path}
      to={item.path}
      aria-label={item.label}
      end={item.path === '/app'}
      className={({ isActive }) =>
        [styles.link, isActive ? styles.active : ''].filter(Boolean).join(' ')
      }
    >
      <Icon name={item.icon} />
      <span>{item.label}</span>
    </NavLink>
  );

  return (
    <aside className={styles.sidebar} data-expanded={expanded}>
      <button
        type="button"
        className={styles.tabletToggle}
        aria-label={expanded ? 'Contraer navegación' : 'Expandir navegación'}
        aria-expanded={expanded}
        onClick={() => onExpandedChange?.(!expanded)}
      >
        <Icon name="menu" />
      </button>
      {/*
       * FOR-185: every entry is a product section now. "Ajustes" used to be
       * pinned apart at the bottom via a `settings` grouping flag; it moved to
       * the topbar's account menu, and the group went with it.
       */}
      <nav className={styles.nav} aria-label="Navegación principal">
        {NAV_ITEMS.map(renderLink)}
      </nav>
      {withings && (
        <Link className={styles.integration} to="/app/settings">
          <span className={styles.integrationHeader}>
            <span className={styles.integrationLabel}>WITHINGS</span>
            <span
              className={[
                styles.integrationDot,
                withings.status === 'CONNECTED' ? '' : styles.integrationDotOff,
              ]
                .filter(Boolean)
                .join(' ')}
              aria-hidden="true"
            />
          </span>
          <span className={styles.integrationStatus}>
            {withings.status === 'CONNECTED' ? 'Conectado' : 'No conectado'}
          </span>
        </Link>
      )}
    </aside>
  );
}
