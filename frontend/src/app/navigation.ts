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
  /**
   * Whether this entry belongs to the sidebar's settings group (FOR-164):
   * rendered apart from the primary section list and pinned to the bottom of
   * the sidebar, matching the template's lower "Ajustes" entry. Orthogonal to
   * `primary` -- that flag is about mobile bottom-bar placement, this one is
   * about desktop sidebar grouping. Omitted (falsy) for the primary group.
   */
  readonly settings?: boolean;
}

export const NAV_ITEMS: readonly NavItem[] = [
  { path: '/', label: 'Dashboard', icon: 'dashboard', primary: true },
  { path: '/mediciones', label: 'Mediciones', icon: 'measurements', primary: true },
  { path: '/entrenamiento', label: 'Entrenamiento', icon: 'training', primary: true },
  { path: '/nutricion', label: 'Nutrición', icon: 'nutrition', primary: true },
  { path: '/lista-compra', label: 'Lista de compra', icon: 'shopping', primary: false },
  { path: '/progreso', label: 'Progreso', icon: 'progress', primary: false },
  { path: '/objetivos', label: 'Objetivos', icon: 'goals', primary: false },
  { path: '/ajustes', label: 'Ajustes', icon: 'settings', primary: false, settings: true },
];
