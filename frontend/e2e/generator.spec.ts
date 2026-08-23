import { expect, test } from '@playwright/test';
import { stubApi } from './stubApi';
import { expectNoHorizontalOverflow } from './layout';

/**
 * El embudo público, medido en un navegador (FOR-190).
 *
 * <p>Existe por el mismo motivo que el resto de `e2e/`: jsdom no hace layout. El
 * rediseño de FOR-190 pasó la suite unitaria entera en verde con cinco defectos
 * de colocación dentro, y los cinco eran invisibles sin pintar la página —
 * pastillas sin caja que parecían una lista de texto, un título que se salía de
 * su tarjeta, barras que perdían la línea de base cuando una etiqueta ocupaba
 * dos líneas, y dos botones partiendo su texto por falta de ancho.
 *
 * <p>Deliberadamente estrecho: no es una segunda suite funcional. Recorre los
 * cuatro pasos y mide geometría.
 */
const PHONE = { width: 393, height: 852 };
const DESKTOP = { width: 1280, height: 900 };

/** Rellena el paso 1 y espera a que llegue la cifra del servidor. */
async function completeStepOne(page: import('@playwright/test').Page) {
  await page.getByLabel('Edad', { exact: true }).fill('45');
  await page.getByLabel('Peso', { exact: true }).fill('75');
  await page.getByLabel('Altura', { exact: true }).fill('182');
  await expect(page.getByText('2585')).toBeVisible();
}

for (const [name, viewport] of [
  ['móvil', PHONE],
  ['escritorio', DESKTOP],
] as const) {
  test(`el embudo no se desborda a lo ancho en ${name}`, async ({ page }) => {
    await stubApi(page);
    await page.setViewportSize(viewport);
    await page.goto('/plan');

    await expectNoHorizontalOverflow(page);

    await completeStepOne(page);
    await expectNoHorizontalOverflow(page);

    await page.getByRole('button', { name: /Siguiente/ }).click();
    await page.getByRole('radio', { name: /Pérdida de peso/ }).check({ force: true });
    await expectNoHorizontalOverflow(page);

    await page.getByRole('button', { name: /Siguiente/ }).click();
    await expectNoHorizontalOverflow(page);

    await page.getByRole('button', { name: /Siguiente/ }).click();
    await expectNoHorizontalOverflow(page);
  });
}

/**
 * La cifra de la cabecera es el eje del rediseño: si no llega, los cuatro pasos
 * pierden lo único que enseña el efecto de contestar. Y es justo lo que estaba
 * roto en el arnés — el cebado CSRF respondía 404 y el POST no salía nunca, sin
 * que nada lo dijera.
 */
test('la cifra viva llega del servidor y se mueve con el objetivo', async ({ page }) => {
  await stubApi(page);
  await page.setViewportSize(PHONE);
  await page.goto('/plan');

  await completeStepOne(page);

  await page.getByRole('button', { name: /Siguiente/ }).click();
  await page.getByRole('radio', { name: /Pérdida de peso/ }).check({ force: true });

  // planKcal del fixture, con su ajuste — no el GET del paso anterior.
  await expect(page.getByText('2068')).toBeVisible();
  await expect(page.getByText('-20 %')).toBeVisible();
});

/**
 * Las barras del reparto del día comparten línea de base.
 *
 * <p>«Media mañana» es la única etiqueta que ocupa dos líneas, y sin reservarle
 * el hueco su barra subía una línea entera respecto a las demás: una fila de
 * cinco barras iguales en la que una estaba más alta, que se lee como que esa
 * comida es mayor. Es una medida, así que vive aquí y no en jsdom.
 */
test('las barras del día se alinean aunque una etiqueta ocupe dos líneas', async ({ page }) => {
  await stubApi(page);
  await page.setViewportSize(PHONE);
  await page.goto('/plan');

  await completeStepOne(page);
  await page.getByRole('button', { name: /Siguiente/ }).click();
  await page.getByRole('radio', { name: /Pérdida de peso/ }).check({ force: true });
  await page.getByRole('button', { name: /Siguiente/ }).click();

  const tops = await page
    .locator('[class*="dayShapeBar"]')
    .evaluateAll((bars) => bars.map((bar) => Math.round(bar.getBoundingClientRect().top)));

  expect(tops.length).toBe(5);
  expect(Math.max(...tops) - Math.min(...tops), `bordes superiores: ${tops.join(', ')}`).toBeLessThanOrEqual(1);
});
