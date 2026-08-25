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
/** El móvil más estrecho que la cabecera aguanta a dos columnas. Ver la prueba del suelo. */
const NARROW_PHONE = { width: 360, height: 800 };
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

/** El reparto de la cabecera: dónde cae cada una de sus tres piezas. */
async function headlineLayout(page: import('@playwright/test').Page) {
  return page.evaluate(() => {
    const eyebrow = document.querySelector('[class*="headlineEyebrow"]')!;
    const value = document.querySelector('[class*="headlineValue"]')! as HTMLElement;
    const aside = document.querySelector('[class*="headlineAside"]')! as HTMLElement;
    const card = eyebrow.parentElement!;
    const box = (el: Element) => {
      const r = el.getBoundingClientRect();
      return { top: Math.round(r.top), right: Math.round(r.right), width: Math.round(r.width) };
    };
    const num = document.querySelector('[class*="headlineNumber"]')!;
    return {
      eyebrow: box(eyebrow),
      value: box(value),
      aside: box(aside),
      cardRight: Math.round(card.getBoundingClientRect().right),
      // La cifra saliéndose de su caja: invisible para cualquier medida del contenedor.
      valueOverflows: value.scrollWidth > value.clientWidth + 1,
      // Cuánto se despega el fondo del costado del fondo del número.
      baseGap: Math.abs(num.getBoundingClientRect().bottom - aside.getBoundingClientRect().bottom),
    };
  });
}

/**
 * La cabecera reparte sus tres piezas en un móvil: rótulo arriba, cifra y costado debajo.
 *
 * <p>Dos defectos vivieron aquí, y ninguno desbordaba la página —la tarjeta crecía a lo
 * alto y a lo ancho todo cabía—, que es justo por qué las pruebas de desbordamiento no
 * vieron ninguno. Con los tres en una fila flexible el rótulo se comía el ancho
 * («REQUERIMIENTO DEL PLAN» en versalitas mide 202 px de los 253 útiles) y el costado
 * caía debajo del número, amontonado contra el borde. Quitando la envoltura para
 * evitarlo, la cifra —que no tiene por dónde partirse— se salía de su caja y se pintaba
 * DEBAJO de la pastilla: se veía bien a 393 px y se rompía a 360.
 *
 * <p>De ahí que se afirme también `valueOverflows`: es la única de las tres medidas que
 * habría cazado el segundo, y ninguna geometría del contenedor lo delata.
 */
for (const [name, step] of [
  ['paso 1', 0],
  ['paso 2', 1],
] as const) {
  test(`la cabecera del ${name} reparte rótulo, cifra y costado en móvil`, async ({ page }) => {
    await stubApi(page);
    await page.setViewportSize(PHONE);
    await page.goto('/plan');

    await completeStepOne(page);
    if (step === 1) {
      await page.getByRole('button', { name: /Siguiente/ }).click();
      await page.getByRole('radio', { name: /Pérdida de peso/ }).check({ force: true });
      await expect(page.getByText('2068')).toBeVisible();
    }

    const l = await headlineLayout(page);

    // El rótulo, en su propia fila y con el ancho entero.
    expect(l.eyebrow.top, `rótulo ${l.eyebrow.top}, cifra ${l.value.top}`).toBeLessThan(
      l.value.top,
    );
    expect(l.eyebrow.width).toBeGreaterThan(l.value.width);

    // Cifra y costado, en la de abajo y en ese orden.
    expect(l.aside.top, `cifra ${l.value.top}, costado ${l.aside.top}`).toBeLessThan(
      l.value.top + 30,
    );
    expect(l.aside.right).toBeGreaterThan(l.value.right);

    // Y la cifra dentro de su caja: salirse no mueve ninguna de las medidas de arriba.
    expect(l.valueOverflows).toBe(false);
    expect(l.aside.right).toBeLessThanOrEqual(l.cardRight);

    /*
     * Apoyadas en el mismo fondo. El hueco reservado para la cifra estuvo en su propia
     * caja, que mide 32 px y se estiraba a 40: los 8 sobrantes quedaban bajo el número
     * y hundían al costado esos mismos 8 px respecto a él. El margen de 2 px es el
     * borde de la pastilla, que cuenta en su caja y no en la del texto.
     */
    expect(l.baseGap, `fondo del número vs del costado: ${l.baseGap}`).toBeLessThanOrEqual(2);
  });
}

/**
 * El suelo del reparto: 360 px, y por los pelos.
 *
 * <p>La cabecera del paso 1 es la apretada de las dos, porque su pastilla dice
 * «Mifflin-St Jeor» y la del paso 2 solo un porcentaje. A 360 px quedan 236 útiles y
 * hacen falta 224: doce de holgura. Esta prueba existe para que esos doce no se gasten
 * sin querer — subir el relleno de la tarjeta, agrandar el tipo de la pastilla o
 * alargar su texto los consume, y el defecto que aparece no desborda nada: la pastilla
 * se va a una fila propia y la tarjeta crece a lo alto.
 *
 * <p>Por debajo de ~348 px no cabe y no es un fallo: la cifra mide 124 px y no tiene
 * por dónde partirse. Ahí la pastilla envuelve a propósito, que es lo que comprueba la
 * afirmación de que sigue pegada al borde derecho.
 */
test('la cabecera del paso 1 aguanta sus dos columnas en un móvil de 360', async ({ page }) => {
  await stubApi(page);
  await page.setViewportSize(NARROW_PHONE);
  await page.goto('/plan');

  await completeStepOne(page);

  const l = await headlineLayout(page);

  expect(l.aside.top, `cifra ${l.value.top}, costado ${l.aside.top}`).toBeLessThan(
    l.value.top + 30,
  );
  expect(l.aside.right).toBeGreaterThan(l.value.right);
  expect(l.valueOverflows).toBe(false);
  expect(l.aside.right).toBeLessThanOrEqual(l.cardRight);
});

/**
 * Los rótulos de sección respiran por debajo.
 *
 * <p>Es una trampa con nombre propio: `.group` es un `fieldset` en `display: flex` con
 * `gap`, y ese hueco NO llega a la leyenda. La leyenda renderizada de un `fieldset` no
 * es un elemento flex —el navegador la coloca aparte y mete el resto en una caja
 * anónima, que es la que forma el contexto flex—, así que entre `legend` y el primer
 * hermano el hueco medido es 0 y toda la separación sale del `padding-bottom` del
 * rótulo. Sin esta prueba, quien vea un `padding` y un `gap` haciendo «lo mismo»
 * borrará el primero y dejará «SEXO» pegado a la tarjeta que rotula.
 *
 * <p>Se mide desde el TEXTO y no desde la caja: el relleno va dentro de la caja del
 * rótulo, así que `legend.bottom` y el techo del contenido coinciden aunque haya aire.
 */
test('los rótulos de sección se despegan de lo que rotulan', async ({ page }) => {
  await stubApi(page);
  await page.setViewportSize(PHONE);
  await page.goto('/plan');

  await completeStepOne(page);

  const separaciones = await page.evaluate(() =>
    Array.from(document.querySelectorAll('legend')).map((legend) => {
      const content = legend.nextElementSibling!;
      const padding = parseFloat(getComputedStyle(legend).paddingBottom);
      const textoAbajo = legend.getBoundingClientRect().bottom - padding;
      return {
        rotulo: (legend.textContent ?? '').trim(),
        separacion: Math.round(content.getBoundingClientRect().top - textoAbajo),
      };
    }),
  );

  expect(separaciones.length).toBeGreaterThan(0);
  for (const { rotulo, separacion } of separaciones) {
    expect(separacion, `«${rotulo}» a ${separacion} px de su contenido`).toBeGreaterThanOrEqual(8);
  }
});

/** Lleva el embudo hasta el paso 3, donde vive la forma del día. */
async function goToStepThree(page: import('@playwright/test').Page) {
  await completeStepOne(page);
  await page.getByRole('button', { name: /Siguiente/ }).click();
  await page.getByRole('radio', { name: /Pérdida de peso/ }).check({ force: true });
  await page.getByRole('button', { name: /Siguiente/ }).click();
}

/**
 * Las barras del día se apoyan en una línea de base común.
 *
 * <p>«Media mañana» es la única etiqueta que ocupa dos líneas, y sin reservarle el
 * hueco su barra subía una línea entera respecto a las demás. Cuando todas medían lo
 * mismo eso se veía como una barra más alta que el resto — o sea, como que esa comida
 * era mayor. Ahora que las alturas SÍ significan eso, el defecto sería peor: diría un
 * reparto falso con la misma pinta que el verdadero.
 *
 * <p>Por eso se miden los BORDES INFERIORES y no los superiores. Los de arriba fueron
 * la medida buena mientras las cinco barras eran iguales, pero medían la altura tanto
 * como la alineación; los de abajo aíslan lo que esta prueba defiende.
 */
test('las barras del día se apoyan en la misma base aunque una etiqueta ocupe dos líneas', async ({
  page,
}) => {
  await stubApi(page);
  await page.setViewportSize(PHONE);
  await page.goto('/plan');
  await goToStepThree(page);

  const bottoms = await page
    .locator('[class*="dayShapeBar"]')
    .evaluateAll((bars) => bars.map((bar) => Math.round(bar.getBoundingClientRect().bottom)));

  expect(bottoms.length).toBe(5);
  expect(
    Math.max(...bottoms) - Math.min(...bottoms),
    `bordes inferiores: ${bottoms.join(', ')}`,
  ).toBeLessThanOrEqual(1);
});

/**
 * La forma del día tiene forma: las comidas grandes se ven grandes.
 *
 * <p>El perfil es ilustrativo y vive en `StepPreferences` (`DAY_SHAPE`), no en el
 * servidor — no hay reparto por comida en el dominio y esta pantalla no se lo inventa
 * con cifras. Lo que sí afirma es el orden: comida por encima de desayuno y cena, y
 * esas por encima de media mañana y merienda. Si alguien aplana las barras o invierte
 * el perfil, el dibujo deja de decir lo que dice su propio nombre.
 *
 * <p>Se comprueba con las alturas y no con los estilos porque el suelo de `BAR_MIN_PX`
 * puede aplastar los extremos: lo que importa es lo que se ve, no lo que se declara.
 */
test('la forma del día ordena las comidas por tamaño', async ({ page }) => {
  await stubApi(page);
  await page.setViewportSize(PHONE);
  await page.goto('/plan');
  await goToStepThree(page);

  const alturas = await page
    .locator('[class*="dayShapeBar"]')
    .evaluateAll((bars) => bars.map((bar) => Math.round(bar.getBoundingClientRect().height)));

  const [desayuno, mediaManana, comida, merienda, cena] = alturas;

  expect(alturas.length).toBe(5);
  expect(comida, `alturas: ${alturas.join(', ')}`).toBeGreaterThan(desayuno);
  expect(desayuno).toBeGreaterThan(mediaManana);
  expect(cena).toBeGreaterThan(merienda);
  // Y ninguna aplastada hasta desaparecer: la más pequeña sigue siendo una barra.
  expect(Math.min(...alturas)).toBeGreaterThanOrEqual(10);
});
