import type { IconName } from '../components/Icon';

/**
 * Centralized navigation model (ADR-006: "Keep navigation definitions
 * centralized"). The sidebar, mobile nav and router all derive from this list,
 * so later stories add a screen by adding one entry here and one route — no
 * layout rewrites.
 *
 * `owner` records the Jira story that will implement each section's real
 * content. Until then every section renders a placeholder; the skeleton owns
 * only the shell, not product functionality.
 */
export interface NavItem {
  /** Route path used by react-router. */
  readonly path: string;
  /** Label shown in the navigation (Spanish, matching docs/mockup.png). */
  readonly label: string;
  /** Icon key resolved by the Icon component. */
  readonly icon: IconName;
  /** Whether this entry appears in the compact mobile navigation bar. */
  readonly primary: boolean;
}

export const NAV_ITEMS: readonly NavItem[] = [
  { path: '/app', label: 'Dashboard', icon: 'dashboard', primary: true },
  { path: '/app/measurements', label: 'Mediciones', icon: 'measurements', primary: true },
  { path: '/app/training', label: 'Entrenamiento', icon: 'training', primary: true },
  // FOR-185: moved out of the mobile bar into the "Más" overflow, where its
  // position in this list puts it first, above "Lista de compra". The bar is
  // down to three sections plus the disclosure, which leaves the remaining
  // tap targets noticeably wider.
  { path: '/app/nutrition', label: 'Nutrición', icon: 'nutrition', primary: false },
  { path: '/app/shopping-list', label: 'Lista de compra', icon: 'shopping', primary: false },
  { path: '/app/progress', label: 'Progreso', icon: 'progress', primary: false },
  { path: '/app/goals', label: 'Objetivos', icon: 'goals', primary: false },
  /*
   * FOR-185: "Ajustes" is deliberately absent. It moved out of section
   * navigation entirely and into the topbar's account menu, next to "Cerrar
   * sesión" — it is account chrome, not a section of the product. That also
   * retired the `settings` grouping flag this model used to carry, which
   * existed solely to pin this one entry to the bottom of the sidebar.
   */
];
