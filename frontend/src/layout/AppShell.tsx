import { Suspense, useEffect, useRef, useState } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { LoadingState } from '../components/LoadingState';
import { IntegrationsProvider } from '../integrations/IntegrationsContext';
import { PlanActivationGate } from '../app/PlanActivationGate';
import { Sidebar } from './Sidebar';
import { MobileNav } from './MobileNav';
import styles from './AppShell.module.css';

/**
 * Application frame (FOR-81): the second tier of the layout, mounted under the
 * global navigation bar owned by {@link RootLayout}.
 *
 * <p>FOR-185 reshaped this. It used to own the top bar too, spanning the
 * sidebar across both grid rows so the bar sat *beside* the navigation; the
 * frame is now strictly "bar on top, content below", and this component is
 * only the content half: the sidebar aside and the routed `<main>` (on small
 * screens the aside is replaced by the fixed bottom {@link MobileNav}). The
 * skip link moved up to `RootLayout` for the same reason — one per document,
 * not one per area.
 *
 * <p>FOR-61 accessibility hardening: the `<main>` landmark receives focus on
 * every client-side route change (not on first mount — see the
 * `isFirstRender` guard). React Router does not move focus on navigation by
 * itself, so without this a keyboard/screen-reader user's focus would silently
 * stay on whatever sidebar link they just activated while the page content
 * changed underneath them (spec edge case: "focus management on route
 * changes").
 */
export function AppShell() {
  const mainRef = useRef<HTMLElement>(null);
  /*
   * Activar el plan cambia lo que responden entrenamiento, nutrición y la lista de compra, y esas
   * pantallas ya han preguntado. Cambiar la clave las vuelve a montar, que es lo que hace que sus
   * cargas se repitan; refrescar la ventana entera haría lo mismo tirando también la sesión de
   * scroll y el estado del resto del marco.
   */
  const [planGeneration, setPlanGeneration] = useState(0);
  const [tabletSidebarExpanded, setTabletSidebarExpanded] = useState(false);
  const isFirstRender = useRef(true);
  const location = useLocation();

  useEffect(() => {
    if (isFirstRender.current) {
      // Skip the very first mount: initial page load should keep the
      // browser's normal landing focus, not immediately steal it into <main>.
      isFirstRender.current = false;
      return;
    }
    mainRef.current?.focus();
  }, [location.pathname]);

  return (
    /*
     * The integration list is read once here and shared (FOR-189): the
     * sidebar's status card and the settings section are two views of it, and
     * with a copy each a disconnect in settings left the card stale until a
     * reload.
     */
    <IntegrationsProvider>
      <PlanActivationGate onActivated={() => setPlanGeneration((generation) => generation + 1)} />
      <div className={styles.shell} data-sidebar-expanded={tabletSidebarExpanded}>
        <Sidebar expanded={tabletSidebarExpanded} onExpandedChange={setTabletSidebarExpanded} />
        <main id="main-content" ref={mainRef} tabIndex={-1} className={styles.content}>
          {/*
          The pages behind this outlet are code-split (see app/routes.tsx), so
          the boundary sits *inside* the frame: the bar, the sidebar and the
          bottom nav stay on screen while the next section's chunk arrives,
          and only the content area shows the loading state.
        */}
          <Suspense fallback={<LoadingState message="Cargando la sección…" />}>
            <Outlet key={planGeneration} />
          </Suspense>
        </main>
        <MobileNav />
      </div>
    </IntegrationsProvider>
  );
}
