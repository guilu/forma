import { useState } from 'react';
import { FoodsPanel } from './admin/FoodsPanel';
import { StorePanel } from './admin/StorePanel';
import styles from './AdminPage.module.css';

/**
 * Catalog maintenance (FOR-190, FOR-191), reachable from the account menu by an
 * admin.
 *
 * <p>Two catalogs, two tabs. **Macros** is the food catalog every plan is built
 * from — what a food is worth nutritionally. **Compra** is the store catalog —
 * where that food is bought, in what package and for how much. They are separate
 * tables and separate tabs because they answer different questions and change
 * for different reasons: a price moves weekly, a macro does not.
 *
 * <p>The shopping tab covers every chain rather than one tab per supermarket:
 * the store is a column, so Carrefour will be rows in this same table.
 *
 * <p>`RequireAdmin` keeps non-admins off the route and the account menu hides
 * the link, but neither is what protects the catalogs: every write here is
 * refused server-side by `@PreAuthorize` without the admin authority. The
 * client-side checks only spare ordinary accounts a screen full of 403s.
 */
const TABS = [
  { key: 'macros', label: 'Macros' },
  { key: 'compra', label: 'Compra' },
] as const;

type TabKey = (typeof TABS)[number]['key'];

export function AdminPage() {
  const [active, setActive] = useState<TabKey>('macros');

  return (
    <div className={styles.wrapper}>
      <header className={styles.header}>
        <div className={styles.titles}>
          <h1 className={styles.title}>Administrar</h1>
          <p className={styles.subtitle}>Catálogos compartidos por toda la aplicación.</p>
        </div>
      </header>

      <div className={styles.tabs} role="tablist" aria-label="Catálogos">
        {TABS.map((tab) => (
          <button
            key={tab.key}
            type="button"
            role="tab"
            id={`tab-${tab.key}`}
            aria-selected={tab.key === active}
            aria-controls={`panel-${tab.key}`}
            className={tab.key === active ? styles.tabActive : styles.tab}
            onClick={() => setActive(tab.key)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Only the selected panel is mounted: each owns its own request, and a
          hidden panel that still loads its catalog is a request nobody asked
          for. */}
      <section
        id={`panel-${active}`}
        role="tabpanel"
        aria-labelledby={`tab-${active}`}
        className={styles.panel}
      >
        {active === 'macros' ? <FoodsPanel /> : <StorePanel />}
      </section>
    </div>
  );
}
