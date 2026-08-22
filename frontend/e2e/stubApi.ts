import type { Page } from '@playwright/test';
import { fixtureFor } from './apiFixtures';

/**
 * Serves the API from `apiFixtures` inside the Playwright browser, so the
 * layout checks need no backend and render the same page on every run.
 *
 * <p>The fixtures themselves live in `./apiFixtures`, which the dev server's
 * `devApiFixtures` plugin also serves — adding an endpoint there gains it for
 * both.
 */
export async function stubApi(page: Page): Promise<void> {
  /*
   * El cliente pide `/actuator/health` antes de cualquier método no seguro, para
   * que el servidor le siembre la cookie `XSRF-TOKEN` (ver `api/client.ts`). Sin
   * esto ese cebado responde 404, el cliente lanza y el POST no llega a salir —
   * o sea que cualquier prueba de un flujo de escritura veía una pantalla que no
   * había pedido nada, sin un solo error en la consola de red.
   */
  await page.route('**/actuator/health', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ status: 'UP' }),
      headers: { 'set-cookie': 'XSRF-TOKEN=e2e-token; Path=/' },
    }),
  );

  // Matched on the versioned prefix, not `**/api/**`: in dev the app's own
  // source modules are served from `/src/api/…`, and a looser glob answers
  // those with JSON too — the page then loads no JavaScript at all.
  await page.route('**/api/v1/**', async (route) => {
    const { status, body } = fixtureFor(new URL(route.request().url()).pathname);
    await route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) });
  });
}
