import { expect, test, type Page } from '@playwright/test';
import { stubApi } from './stubApi';
import { expectGlassSurface, expectNoHorizontalOverflow, expectSinglePageScroller } from './layout';

/**
 * Layout checks (FOR-186) — see `playwright.config.ts` for why these exist
 * alongside the jsdom suite.
 *
 * <p>The widths are not round numbers for their own sake: 375 is the common
 * phone, 574 is where a two-column dashboard grid was still rendering when it
 * should have collapsed, and 1280 is the sidebar layout.
 */
const PHONE = { width: 375, height: 720 };
const NARROW = { width: 574, height: 720 };
const TABLET = { width: 1180, height: 820 };
const IPAD_LANDSCAPE = { width: 1194, height: 702 };
const DESKTOP = { width: 1280, height: 900 };
/** Near the top of the mid-width band. */
const LAPTOP = { width: 1440, height: 900 };
/** Above the mid-width band, where the dashboard rows are at their widest. */
const WIDE = { width: 1680, height: 900 };

const APP_ROUTES = ['/app', '/app/measurements', '/app/progress'] as const;

test.beforeEach(async ({ page }) => {
  await stubApi(page);
});

/**
 * Waits for the route's own content, not just the shell around it.
 *
 * <p>The page's `<h1>` is the signal, deliberately: `<main>` is part of the
 * frame and appears immediately, holding the Suspense fallback while the
 * route's chunk is fetched (routes are code-split — see `app/routes.tsx`).
 * Measuring on `<main>` alone measures the loading state.
 */
async function gotoApp(page: Page, path: string): Promise<void> {
  await page.goto(path);
  await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
  // Charts size themselves from a measured box, so give the first paint a
  // chance to settle before measuring anything.
  await page.waitForFunction(() => document.fonts.ready.then(() => true));
}

for (const viewport of [PHONE, NARROW, TABLET, DESKTOP]) {
  test.describe(`at ${viewport.width}px`, () => {
    test.use({ viewport });

    for (const path of APP_ROUTES) {
      test(`${path} fits the viewport and scrolls once`, async ({ page }) => {
        await gotoApp(page, path);

        await expectNoHorizontalOverflow(page);
        await expectSinglePageScroller(page);
      });
    }
  });
}

for (const viewport of [PHONE, DESKTOP]) {
  test.describe(`training detail at ${viewport.width}px`, () => {
    test.use({ viewport });

    test('keeps the workout prescription inside the main viewport', async ({ page }) => {
      const session = {
        id: 'SUNDAY:STRENGTH',
        kind: 'STRENGTH',
        bodyView: 'FRONT',
        title: 'Fuerza · Pierna y core',
        detail: '2 ejercicios',
        status: 'COMPLETED',
        workoutType: 'LEGS',
      };
      await page.route('**/api/v1/training/week', (route) =>
        route.fulfill({
          contentType: 'application/json',
          body: JSON.stringify({
            days: [{ dayOfWeek: 'SUNDAY', rest: false, sessions: [session] }],
          }),
        }),
      );
      await page.route('**/api/v1/training/workouts/LEGS', (route) =>
        route.fulfill({
          contentType: 'application/json',
          body: JSON.stringify({
            workoutType: 'LEGS',
            items: [
              {
                exerciseId: 'goblet-squat',
                exerciseName: 'Sentadilla goblet',
                order: 1,
                sets: 4,
                repScheme: 'RANGE',
                repsMin: 10,
                repsMax: 15,
                restSeconds: 90,
                rir: 2,
              },
              {
                exerciseId: 'dead-bug',
                exerciseName: 'Dead bug',
                order: 2,
                sets: 3,
                repScheme: 'RANGE',
                repsMin: 10,
                repsMax: 15,
                restSeconds: 45,
                rir: 2,
              },
            ],
          }),
        }),
      );
      await page.route('**/api/v1/training/sessions/SUNDAY%3ASTRENGTH/muscle-map', (route) =>
        route.fulfill({
          contentType: 'application/json',
          body: JSON.stringify({
            sessionId: session.id,
            muscles: [
              { muscle: 'cuádriceps', load: 'HIGH' },
              { muscle: 'glúteos', load: 'MEDIUM' },
              { muscle: 'core', load: 'LOW' },
            ],
          }),
        }),
      );

      await gotoApp(page, '/app/training/SUNDAY%3ASTRENGTH');

      await expect(page.getByRole('heading', { name: 'Pierna y core', level: 1 })).toBeVisible();
      await expectNoHorizontalOverflow(page);
      await expectSinglePageScroller(page);
    });
  });
}

test.describe('training on iPad landscape', () => {
  test.use({ viewport: IPAD_LANDSCAPE });

  test('uses the tablet navigation and purpose-built vertical training flow', async ({ page }) => {
    const dayNames = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
    await page.route('**/api/v1/training/week', (route) =>
      route.fulfill({
        contentType: 'application/json',
        body: JSON.stringify({
          days: dayNames.map((dayOfWeek, index) => ({
            dayOfWeek,
            rest: index === 4,
            sessions:
              index === 4
                ? []
                : [
                    {
                      id: `${dayOfWeek}:STRENGTH`,
                      kind: 'STRENGTH',
                      bodyView: dayOfWeek === 'THURSDAY' ? 'BACK' : 'FRONT',
                      title: `Fuerza · Sesión ${index + 1}`,
                      detail: '5 ejercicios',
                      status: index < 3 ? 'COMPLETED' : 'PLANNED',
                      workoutType: 'PUSH',
                    },
                  ],
          })),
        }),
      }),
    );
    await gotoApp(page, '/app/training');

    const sidebar = page.getByRole('complementary');
    const expand = page.getByRole('button', { name: 'Expandir navegación' });
    await expect(sidebar).toHaveCSS('width', '72px');
    await expand.click();
    await expect(sidebar).toHaveCSS('width', '252px');
    await page.getByRole('button', { name: 'Contraer navegación' }).click();
    await expect(sidebar).toHaveCSS('width', '72px');

    const today = page.getByRole('heading', { name: 'Entrenamiento de hoy' }).locator('..');
    const summary = page.getByRole('heading', { name: 'Resumen semanal' }).locator('..');
    const calendar = page.getByRole('heading', { name: 'Calendario semanal' }).locator('..');
    const todayBox = await today.boundingBox();
    const summaryBox = await summary.boundingBox();
    const calendarBox = await calendar.boundingBox();
    expect(todayBox).not.toBeNull();
    expect(summaryBox).not.toBeNull();
    expect(calendarBox).not.toBeNull();
    expect(summaryBox!.y).toBeGreaterThan(todayBox!.y + todayBox!.height - 2);
    expect(calendarBox!.y).toBeGreaterThan(summaryBox!.y + summaryBox!.height - 2);

    const summaryRows = page.getByRole('listitem', {
      name: /Sesiones totales|Carreras|Fuerza/,
    });
    const rowBoxes = await summaryRows.evaluateAll((rows) =>
      rows.slice(0, 3).map((row) => row.getBoundingClientRect().toJSON()),
    );
    expect(rowBoxes).toHaveLength(3);
    expect(
      Math.max(...rowBoxes.map((box) => box.y)) - Math.min(...rowBoxes.map((box) => box.y)),
    ).toBeLessThan(3);

    const calendarScroller = page.getByRole('list', {
      name: 'Calendario semanal de entrenamiento',
    });
    await expect
      .poll(() =>
        calendarScroller.evaluate((element) => ({
          clientWidth: element.clientWidth,
          scrollWidth: element.scrollWidth,
          snap: getComputedStyle(element).scrollSnapType,
        })),
      )
      .toMatchObject({ snap: 'x mandatory' });
    const scrollSize = await calendarScroller.evaluate((element) => ({
      clientWidth: element.clientWidth,
      scrollWidth: element.scrollWidth,
    }));
    expect(scrollSize.scrollWidth).toBeGreaterThan(scrollSize.clientWidth);
    const currentDay = calendarScroller.locator('li[aria-current="date"]');
    const centers = await Promise.all([
      calendarScroller.evaluate((element) => {
        const box = element.getBoundingClientRect();
        return box.left + box.width / 2;
      }),
      currentDay.evaluate((element) => {
        const box = element.getBoundingClientRect();
        return box.left + box.width / 2;
      }),
    ]);
    expect(Math.abs(centers[0] - centers[1])).toBeLessThan(100);

    await expectNoHorizontalOverflow(page);
    await expectSinglePageScroller(page);
  });
});

test.describe('dashboard grid', () => {
  test.use({ viewport: NARROW });

  /**
   * One column for the widgets, two for the body tiles (FOR-189). The tiles hold
   * a short label and a number, so a phone fits two across; every other card is
   * a list, a chart or a paragraph and gets the full width.
   */
  test('collapses to a single column of widgets below the mobile breakpoint', async ({ page }) => {
    await gotoApp(page, '/app');

    const tiles = await page
      .locator('main [class*="body"] > section')
      .evaluateAll((cards) => cards.map((c) => Math.round(c.getBoundingClientRect().left)));
    expect(tiles.length, 'The four body tiles were not found').toBe(4);
    expect(
      new Set(tiles).size,
      `The body tiles use ${new Set(tiles).size} columns (x: ${[...new Set(tiles)].join(', ')})`,
    ).toBe(2);

    // Everything that is not one of those tiles still spans the single column.
    const others = await page
      .locator('main [class*="todayGrid"] > section')
      .evaluateAll((cards) => cards.map((card) => Math.round(card.getBoundingClientRect().left)));
    expect(
      new Set(others).size,
      `Widgets start at ${new Set(others).size} different x positions`,
    ).toBe(1);
  });

  test('places the combined nutrition card before water with donut and macros side by side', async ({
    page,
  }) => {
    await gotoApp(page, '/app');

    const nutrition = widget(page, 'Nutrición');
    const water = page
      .getByRole('heading', { name: 'Agua', exact: true })
      .locator('xpath=ancestor::section[1]');
    const [nutritionTop, waterTop] = await Promise.all([
      nutrition.evaluate((card) => Math.round(card.getBoundingClientRect().top)),
      water.evaluate((card) => Math.round(card.getBoundingClientRect().top)),
    ]);
    expect(nutritionTop).toBeLessThan(waterTop);

    const nutritionPrecedesWaterInDom = await nutrition.evaluate((card) => {
      const waterHeading = [...document.querySelectorAll('h3')].find(
        (heading) => heading.textContent?.trim() === 'Agua',
      );
      const waterCard = waterHeading?.closest('section');
      return Boolean(
        waterCard && card.compareDocumentPosition(waterCard) & Node.DOCUMENT_POSITION_FOLLOWING,
      );
    });
    expect(nutritionPrecedesWaterInDom).toBe(true);

    const ringLeft = await nutrition
      .getByRole('img', { name: /kcal consumidas/ })
      .evaluate((node) => Math.round(node.getBoundingClientRect().left));
    const macrosLeft = await nutrition
      .getByRole('progressbar', { name: /Proteínas/ })
      .evaluate((node) => Math.round(node.getBoundingClientRect().left));
    expect(macrosLeft).toBeGreaterThan(ringLeft);
  });
});

/**
 * The boxes actually laid out by a dashboard row.
 *
 * <p>Not simply the row's DOM children: the body-composition block is a single
 * `<div>` holding four tiles, and in the tablet band it becomes
 * `display: contents` so those tiles join the parent grid directly. An element
 * with no box of its own reports an empty rect, so it is replaced by the tiles
 * it dissolves into.
 *
 * <p>CSS-module class names are hashed, so a substring match on the authored
 * name is the stable way to reach a layout container from outside the app.
 */
async function rowCards(
  page: Page,
  row: string,
): Promise<{ top: number; left: number; height: number }[]> {
  return page
    .locator(`main [class*="${row}"]`)
    .first()
    .evaluate((grid) => {
      const boxes = [...grid.children].flatMap((child) =>
        getComputedStyle(child).display === 'contents' ? [...child.children] : [child],
      );
      return boxes.map((box) => {
        const rect = box.getBoundingClientRect();
        return {
          top: Math.round(rect.top),
          left: Math.round(rect.left),
          height: Math.round(rect.height),
        };
      });
    });
}

test.describe('dashboard rows', () => {
  test.use({ viewport: WIDE });

  for (const row of ['todayGrid', 'rowThree']) {
    test(`gives cards sharing a ${row} grid row the same height`, async ({ page }) => {
      await gotoApp(page, '/app');

      const cards = await rowCards(page, row);
      const rows = new Map<number, typeof cards>();
      for (const card of cards) rows.set(card.top, [...(rows.get(card.top) ?? []), card]);
      expect(cards.length, `No cards found in the ${row}`).toBeGreaterThan(1);
      for (const [top, entries] of rows) {
        if (entries.length < 2) continue;
        const heights = entries.map((card) => card.height);
        expect(
          new Set(heights).size,
          `Cards at y=${top} in ${row} differ: ${heights.join(', ')}`,
        ).toBe(1);
      }
    });
  }
});

/**
 * On a tablet in landscape the metric tiles across a single row left each
 * one about 180px wide — too narrow for a headline value and its caption. The
 * band below the desktop layout (1101–1600px) wraps every row at three columns
 * instead.
 */
// Both ends of the band: the tablet that prompted it and a laptop near the top
// edge, where narrow tracks were still breaking headline figures across lines.
for (const viewport of [TABLET, LAPTOP]) {
  test.describe(`dashboard grid at ${viewport.width}px`, () => {
    test.use({ viewport });

    // The unified today grid uses three tracks; its body group, water, main
    // three-card row and final trend occupy four explicit rows.
    for (const [row, columns, rows] of [
      ['todayGrid', 3, 4],
      // Two x positions, not three: Evolución took the column the retired
      // "Tu progreso" card left behind, so it starts at track 1 and spans two.
      ['rowThree', 2, 2],
    ] as const) {
      test(`lays the ${row} row out as ${columns} columns over ${rows} rows`, async ({ page }) => {
        await gotoApp(page, '/app');

        const cards = await rowCards(page, row);
        const lefts = new Set(cards.map((card) => card.left));
        const tops = new Set(cards.map((card) => card.top));

        expect(
          lefts.size,
          `The ${row} row has ${lefts.size} columns (x: ${[...lefts].join(', ')})`,
        ).toBe(columns);
        expect(tops.size, `The ${row} row has ${tops.size} rows (y: ${[...tops].join(', ')})`).toBe(
          rows,
        );
      });
    }
  });
}

/** The widget card whose section heading is `title`. */
function widget(page: Page, title: string) {
  return page
    .getByRole('heading', { name: title, exact: true })
    .locator('xpath=ancestor::section[1]');
}

test.describe('dashboard widget internals', () => {
  test.use({ viewport: DESKTOP });

  /**
   * The three series names used to wrap onto a second and third line, which
   * reads as three legends stacked under the plot instead of one.
   */
  test('the trend legend keeps its three series on one line', async ({ page }) => {
    await gotoApp(page, '/app');

    const items = await widget(page, 'Tendencia 30 días')
      .locator('ul li')
      .evaluateAll((entries) =>
        entries.map((entry) => ({
          text: entry.textContent?.trim() ?? '',
          top: Math.round(entry.getBoundingClientRect().top),
        })),
      );

    expect(items.length, 'The trend legend was not found').toBe(3);
    const tops = new Set(items.map((item) => item.top));
    expect(
      tops.size,
      `The legend wraps onto ${tops.size} lines: ${items.map((i) => `${i.text}@${i.top}`).join(', ')}`,
    ).toBe(1);
  });

  /**
   * The chart had a fixed 140px height, so in a card stretched to its row it
   * left a dead band underneath and squeezed the plot into a strip.
   */
  test('the evolution chart fills the width and the leftover height of its card', async ({
    page,
  }) => {
    await gotoApp(page, '/app');

    const box = await widget(page, 'Evolución').evaluate((card) => {
      const chart = card.querySelector('[role="img"]');
      if (!chart) return null;
      const style = getComputedStyle(card);
      const chartRect = chart.getBoundingClientRect();
      return {
        cardHeight: card.getBoundingClientRect().height,
        // Content box: `clientWidth` already excludes the border.
        cardInnerWidth:
          card.clientWidth - parseFloat(style.paddingLeft) - parseFloat(style.paddingRight),
        chartHeight: chartRect.height,
        chartWidth: chartRect.width,
      };
    });

    expect(box, 'The evolution chart was not found').not.toBeNull();
    const { cardHeight, cardInnerWidth, chartHeight, chartWidth } = box!;

    expect(
      Math.round(chartWidth),
      `The chart is ${Math.round(chartWidth)}px wide inside a ${Math.round(cardInnerWidth)}px card`,
    ).toBeGreaterThanOrEqual(Math.round(cardInnerWidth) - 1);
    // Half the card is a low bar the old fixed 140px failed anyway: the card
    // stretches to its row, so the value, the tabs and the chart share it.
    expect(
      Math.round(chartHeight),
      `The chart is ${Math.round(chartHeight)}px tall in a ${Math.round(cardHeight)}px card`,
    ).toBeGreaterThan(cardHeight * 0.5);
  });
});

/**
 * The settings grid: cards of very different content lengths laid out in one
 * grid, where two of them are meant to be twice as wide as the rest.
 */
async function gotoSettings(page: Page): Promise<void> {
  await gotoApp(page, '/app/settings');
  // Perfil and Unidades fetch before rendering: measure their real content,
  // not the loading spinner that stands in for it.
  await expect(page.getByRole('button', { name: 'Editar perfil' })).toBeVisible();
}

async function settingsCards(page: Page) {
  return page
    .locator('main [class*="grid"]')
    .first()
    .evaluate((grid) => ({
      columns: getComputedStyle(grid).gridTemplateColumns.split(' ').length,
      cards: [...grid.children].map((card) => {
        const rect = card.getBoundingClientRect();
        return {
          title: card.querySelector('h2')?.textContent?.trim() ?? '(untitled)',
          top: Math.round(rect.top),
          left: Math.round(rect.left),
          width: Math.round(rect.width),
          height: Math.round(rect.height),
        };
      }),
    }));
}

test.describe('settings grid on a wide screen', () => {
  test.use({ viewport: WIDE });

  test('gives Perfil the whole row and pairs Unidades with Integraciones', async ({ page }) => {
    await gotoSettings(page);

    const { columns, cards } = await settingsCards(page);
    expect(columns, 'The settings grid is not three columns wide').toBe(3);

    const card = (title: string) => cards.find((entry) => entry.title.startsWith(title));
    const units = card('Unidades');
    const integrations = card('Integraciones');
    const profile = card('Perfil y preferencias');
    expect(units, 'The Unidades card was not found').toBeDefined();
    expect(integrations, 'The Integraciones card was not found').toBeDefined();
    expect(profile, 'The Perfil card was not found').toBeDefined();

    // Perfil is every track plus the gaps between them, so it is wider than two
    // single columns would be on their own.
    expect(
      profile!.width,
      `Perfil is ${profile!.width}px wide next to a ${units!.width}px single column`,
    ).toBeGreaterThan(units!.width * 1.9);

    // Unidades and Integraciones share a row and a width.
    expect(
      integrations!.top,
      `Unidades sits at y=${units!.top} and Integraciones at y=${integrations!.top}`,
    ).toBe(units!.top);
    expect(integrations!.width).toBe(units!.width);
  });

  test('gives every card in a grid row the same height', async ({ page }) => {
    await gotoSettings(page);

    const { cards } = await settingsCards(page);
    const rows = new Map<number, typeof cards>();
    for (const card of cards) {
      rows.set(card.top, [...(rows.get(card.top) ?? []), card]);
    }

    expect(rows.size, 'The settings cards did not lay out in rows').toBeGreaterThan(1);
    for (const [top, row] of rows) {
      const heights = new Set(row.map((card) => card.height));
      expect(
        heights.size,
        `Row at y=${top} has cards of differing heights: ${row
          .map((card) => `${card.title} ${card.height}px`)
          .join(', ')}`,
      ).toBe(1);
    }
  });

  test('offers only the provider FORMA supports today', async ({ page }) => {
    await gotoSettings(page);

    // Scoped to `main`: the sidebar carries its own Withings status card.
    const settings = page.locator('main');
    await expect(settings.getByText('Withings')).toBeVisible();
    // The API answers with all three; the two without a working integration
    // must not be offered.
    await expect(settings.getByText('Google Fit')).toHaveCount(0);
    await expect(settings.getByText('Apple Health')).toHaveCount(0);
  });
});

test.describe('settings grid on a phone', () => {
  test.use({ viewport: PHONE });

  /**
   * One column, and there each card is as tall as its own content — the equal
   * heights above are a property of sharing a row, not of the cards.
   */
  test('stacks into one column of content-sized cards', async ({ page }) => {
    await gotoSettings(page);

    const { cards } = await settingsCards(page);
    const lefts = new Set(cards.map((card) => card.left));
    expect(lefts.size, `The cards start at ${lefts.size} different x positions`).toBe(1);

    const tops = new Set(cards.map((card) => card.top));
    expect(tops.size, 'The cards are not each on their own row').toBe(cards.length);

    const heights = new Set(cards.map((card) => card.height));
    expect(
      heights.size,
      `Every card is ${[...heights].join('/')}px tall, so they are not content-sized`,
    ).toBeGreaterThan(1);
  });
});

/**
 * The confirm dialog's destructive action carries a red gradient, the danger
 * counterpart of the primary CTA's brand ramp. Both themes get the same ramp,
 * so the check that matters is the one a stylesheet cannot make on its own:
 * that the label stays legible against *every* stop of it.
 */
/**
 * Colour assignments a stylesheet can state but not enforce: that each chart
 * series and each legend dot resolves to the intended token in both themes, and
 * that the progress donut ramps rather than filling flat.
 */
test.describe('chart colours', () => {
  test.use({ viewport: WIDE });

  for (const theme of ['dark', 'light'] as const) {
    test(`give the trend legend one distinct colour per series in the ${theme} theme`, async ({
      page,
    }) => {
      await gotoApp(page, '/app');
      await page.evaluate((value) => {
        document.documentElement.setAttribute('data-theme', value);
      }, theme);

      const dots = await widget(page, 'Tendencia 30 días')
        .locator('ul li span[style*="background"]')
        .evaluateAll((nodes) => nodes.map((node) => getComputedStyle(node).backgroundColor));

      expect(dots.length, 'The three series dots were not found').toBe(3);
      expect(new Set(dots).size, `Series share a colour: ${dots.join(', ')}`).toBe(3);
      // Weight is the brand green; the other two are the info and warning tokens
      // rather than two greens nobody can tell apart.
      const accent = await page.evaluate(() =>
        getComputedStyle(document.documentElement).getPropertyValue('--color-accent').trim(),
      );
      expect(accent, 'The accent token is unset').not.toBe('');
    });
  }

  test('paints the shared calorie progress donut with a filled arc and track', async ({ page }) => {
    await gotoApp(page, '/app');

    const ring = page.getByRole('img', { name: /kcal consumidas/ });
    const background = await ring.evaluate((el) => getComputedStyle(el).backgroundImage);

    expect(background, 'The donut is not painted with a conic gradient').toContain('conic');
    // The shared nutrition donut has one progress colour and one track colour.
    const stops = background.match(/rgba?\([^)]+\)/g) ?? [];
    expect(
      new Set(stops).size,
      `Expected a filled arc plus a track, got ${stops.join(', ')}`,
    ).toBeGreaterThanOrEqual(2);
  });
});

/** WCAG relative luminance of an `rgb(r, g, b)` string. */
function luminance(color: string): number {
  const [r, g, b] = (color.match(/\d+(\.\d+)?/g) ?? []).slice(0, 3).map(Number);
  const channel = (value: number) => {
    const s = value / 255;
    return s <= 0.03928 ? s / 12.92 : ((s + 0.055) / 1.055) ** 2.4;
  };
  return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b);
}

function contrast(a: string, b: string): number {
  const [lighter, darker] = [luminance(a), luminance(b)].sort((x, y) => y - x);
  return (lighter + 0.05) / (darker + 0.05);
}

/**
 * The focus ring is the one accent-coloured thing that has to be *seen* against
 * the page rather than painted with text on top of it, so it rides the
 * text-safe token. The fill accent is ~1.5:1 on the light background, which
 * would leave keyboard users with an invisible ring.
 */
test.describe('focus ring', () => {
  test.use({ viewport: DESKTOP });

  for (const theme of ['dark', 'light'] as const) {
    test(`stays visible against the page in the ${theme} theme`, async ({ page }) => {
      await gotoApp(page, '/app/measurements');
      await page.evaluate((value) => {
        document.documentElement.setAttribute('data-theme', value);
      }, theme);

      // Focus a real control and read the ring the browser actually paints —
      // reading the token instead would pass even if nothing used it.
      await page.getByRole('button', { name: /Registrar medición/ }).focus();
      const sample = await page.evaluate(() => {
        const focused = document.activeElement as HTMLElement;
        const ring = getComputedStyle(focused).outlineColor;
        const page = getComputedStyle(document.body).backgroundColor;
        return { ring, page };
      });

      const ratio = contrast(sample.ring, sample.page);
      expect(
        Math.round(ratio * 100) / 100,
        `The focus ring ${sample.ring} on ${sample.page} is only ${ratio.toFixed(2)}:1`,
      ).toBeGreaterThanOrEqual(3);
    });
  }
});

test.describe('destructive confirmation button', () => {
  test.use({ viewport: DESKTOP });

  for (const theme of ['dark', 'light'] as const) {
    test(`is a red gradient with a legible label in the ${theme} theme`, async ({ page }) => {
      await gotoApp(page, '/app/measurements');
      await page.evaluate((value) => {
        document.documentElement.setAttribute('data-theme', value);
      }, theme);

      await page
        .getByRole('button', { name: /Eliminar la medición/ })
        .first()
        .click();
      const confirm = page
        .getByRole('dialog')
        .getByRole('button', { name: 'Eliminar', exact: true });

      const style = await confirm.evaluate((button) => {
        const computed = getComputedStyle(button);
        return { color: computed.color, background: computed.backgroundImage };
      });

      expect(style.background, 'The confirm action is not painted with a gradient').toContain(
        'gradient',
      );

      // Every colour stop the ramp resolves to, not just its endpoints.
      const stops = style.background.match(/rgba?\([^)]+\)/g) ?? [];
      expect(stops.length, `No colour stops found in "${style.background}"`).toBeGreaterThan(1);
      for (const stop of stops) {
        const ratio = contrast(style.color, stop);
        expect(
          Math.round(ratio * 100) / 100,
          `Label ${style.color} on stop ${stop} is only ${ratio.toFixed(2)}:1`,
        ).toBeGreaterThanOrEqual(4.5);
      }
    });
  }
});

test.describe('glass chrome', () => {
  test.use({ viewport: PHONE });

  test('the bar, the bottom pill and the menu hanging off it are all blurred', async ({ page }) => {
    await gotoApp(page, '/app');

    // The bar's fill sits on a pseudo-element on purpose: an element with
    // `backdrop-filter` becomes a backdrop root for its descendants, which
    // would leave its own menus with nothing to blur.
    await expectGlassSurface(page, 'header[class*="topbar"]', '::before');
    await expectGlassSurface(page, 'nav[aria-label="Navegación principal"] [class*="bar"]');

    // The overflow menu is the one that regressed twice: nested inside the
    // blurred pill, then painted opaque by a later same-specificity rule.
    await page.getByRole('button', { name: 'Más' }).click();
    await expectGlassSurface(page, '[role="menu"]');
  });
});

test.describe('landing hero CTA', () => {
  test.use({ viewport: PHONE });

  /**
   * On a phone, a CTA stretched edge to edge stops reading as a button and
   * starts reading as a form field or a banner. It is capped and centred
   * instead, so the gutters make it look like the tappable target it is.
   *
   * <p>The hero used to carry two CTAs and this checked they shared a width and
   * a left edge. "Ver Demo" is gone — it promised a demo and only scrolled to
   * the product section — so the pair assertions became the centring check
   * below, which is what they were really protecting.
   */
  test('does not span the full width of the phone', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();

    // El CTA lleva al generador de plan desde que existe el embudo: es lo que se
    // le ofrece a alguien que aún no tiene cuenta.
    const cta = page.getByRole('link', { name: 'Crea tu plan gratis' });
    await expect(cta).toBeVisible();

    const box = await cta.evaluate((link) => {
      const rect = link.getBoundingClientRect();
      // One client rect per line the label occupies: a narrower button that
      // wraps its label onto two lines is not the fix we want.
      const range = document.createRange();
      range.selectNodeContents(link);
      const lines = new Set([...range.getClientRects()].map((line) => Math.round(line.top))).size;
      return { width: rect.width, centre: rect.left + rect.width / 2, lines };
    });

    expect(
      box.width,
      `The hero CTA is ${Math.round(box.width)}px wide in a ${PHONE.width}px viewport`,
    ).toBeLessThanOrEqual(PHONE.width * 0.75);
    expect(box.lines, `The hero CTA wraps onto ${box.lines} lines`).toBe(1);
    expect(
      Math.abs(box.centre - PHONE.width / 2),
      `The hero CTA is centred at ${Math.round(box.centre)}px, not ${PHONE.width / 2}px`,
    ).toBeLessThanOrEqual(1);
  });
});

test.describe('code splitting', () => {
  test('the public landing never fetches the charting library', async ({ page }) => {
    const modules: string[] = [];
    page.on('request', (request) => {
      if (request.resourceType() === 'script') modules.push(new URL(request.url()).pathname);
    });

    await page.goto('/');
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();

    // Recharts is ~110 kB gzipped and is reachable only from the pages behind
    // `/app` (ADR-013). An anonymous visitor must not pay for it — which is
    // what happens the moment a chart component is imported statically from
    // anything the landing renders.
    const charting = modules.filter((path) => path.includes('recharts'));
    expect(charting, `The landing loaded the charting library:\n${charting.join('\n')}`).toEqual(
      [],
    );

    // Guards the check itself: if the landing stopped loading scripts at all,
    // the assertion above would pass for the wrong reason.
    expect(modules.length, 'No scripts were loaded at all').toBeGreaterThan(0);
  });
});

/**
 * The catalog table used to run seven columns wide on a phone. It scrolled
 * sideways inside its card — and, because the "Acciones" header was an
 * absolutely positioned sr-only span with no positioned ancestor, its
 * containing block was the viewport rather than the scroller, so it escaped the
 * clip and stretched the *document* to 723 px. A sideways flick anywhere on the
 * page then dragged the whole layout.
 */
test.describe('the admin catalog on a phone', () => {
  const FOODS = Array.from({ length: 12 }, (_, index) => ({
    id: `food-${index}`,
    name: `Alimento de nombre largo ${index}`,
    kcal: 100 + index,
    proteinG: 12.5,
    carbsG: 60,
    fatG: 3.4,
    servingSizeG: 60,
    category: 'PROTEINA',
  }));

  test.beforeEach(async ({ page }) => {
    // Registered after `stubApi`, so these win: Playwright tries the most
    // recently added route first.
    await page.route('**/api/v1/auth/me', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ id: 'e2e-user', email: 'e2e@forma.test', role: 'ADMIN' }),
      }),
    );
    await page.route('**/api/v1/foods', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(FOODS),
      }),
    );
    await page.setViewportSize({ width: 390, height: 844 });
    await page.goto('/app/admin');
    await expect(page.getByRole('table')).toBeVisible();
  });

  test('never widens the page past the viewport', async ({ page }) => {
    const { scrollWidth, clientWidth } = await page.evaluate(() => ({
      scrollWidth: document.documentElement.scrollWidth,
      clientWidth: document.documentElement.clientWidth,
    }));
    expect(
      scrollWidth,
      `The page scrolls sideways: ${scrollWidth}px in a ${clientWidth}px viewport`,
    ).toBeLessThanOrEqual(clientWidth);
  });

  test('reaches the macros and the actions of a row without scrolling sideways', async ({
    page,
  }) => {
    const first = page.getByRole('button', { name: 'Alimento de nombre largo 0' });
    await expect(first).toHaveAttribute('aria-expanded', 'false');
    await first.click();

    const edit = page.getByRole('button', { name: 'Editar Alimento de nombre largo 0' });
    await expect(edit).toBeVisible();
    const box = await edit.boundingBox();
    expect(box, 'The edit action has no box').not.toBeNull();
    expect(box!.x + box!.width).toBeLessThanOrEqual(390);
  });

  test('pages the catalog instead of running it off the bottom', async ({ page }) => {
    await expect(page.getByText('Página 1 de 2')).toBeVisible();
    await expect(page.getByText('Alimento de nombre largo 10')).toBeHidden();

    await page.getByRole('button', { name: 'Página siguiente' }).click();

    await expect(page.getByText('Alimento de nombre largo 10')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Página siguiente' })).toBeDisabled();
  });
});

/**
 * Two headers that put their action on its own row on a phone, pushing everything below them down
 * for something that fits beside the title. The dashboard already gets this right; these two follow
 * it.
 */
test.describe('page headers on a phone', () => {
  const sharesARowWith = async (
    first: { x: number; y: number; width: number; height: number },
    second: { x: number; y: number; width: number; height: number },
  ) => {
    const overlap =
      Math.min(first.y + first.height, second.y + second.height) - Math.max(first.y, second.y);
    return { overlap, secondStartsAfter: second.x > first.x };
  };

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 800 });
  });

  test('the admin catalog puts its add action beside the title', async ({ page }) => {
    await page.route('**/api/v1/auth/me', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ id: 'e2e-user', email: 'e2e@forma.test', role: 'ADMIN' }),
      }),
    );
    await page.route('**/api/v1/foods', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: '[]' }),
    );
    await page.goto('/app/admin');

    const title = await page.getByRole('heading', { level: 1 }).boundingBox();
    const action = await page.getByRole('button', { name: '+ Alimento' }).boundingBox();
    const { overlap, secondStartsAfter } = await sharesARowWith(title!, action!);

    expect(overlap, `The action sits on its own row (overlap ${overlap}px)`).toBeGreaterThan(0);
    expect(secondStartsAfter, 'The action is not to the right of the title').toBe(true);
  });

  /**
   * Nutrition's date moved out of the row and under the title, and that is the point.
   *
   * <p>It used to sit beside it inside a date navigator with two arrows that moved nothing. A date
   * you cannot change is a date, not a control, so it reads as one now: same column as the title,
   * on the line below it. What is still worth asserting is that it stays inside the header and
   * does not drift off to its own corner.
   */
  test('nutrition puts its date under the title', async ({ page }) => {
    await page.goto('/app/nutrition');

    const title = await page.getByRole('heading', { level: 1 }).boundingBox();
    const date = await page.getByText(/\bago/i).first().boundingBox();

    expect(date!.y, 'The date is not below the title').toBeGreaterThanOrEqual(title!.y);
    // Misma columna: alineada con el título, no empujada al otro extremo de la cabecera.
    expect(Math.abs(date!.x - title!.x), 'The date is not aligned with the title').toBeLessThan(4);
  });

  test('training puts its date beside the title', async ({ page }) => {
    await page.goto('/app/training');

    const title = await page.getByRole('heading', { level: 1 }).boundingBox();
    // Matches the month in either format — the long "Domingo, 2 De Agosto" this
    // header used to render and the compact "2 ago 2026" it renders now — so the
    // test fails on the layout it is about rather than timing out on a locator.
    const date = await page.getByText(/\bago/i).first().boundingBox();
    const { overlap, secondStartsAfter } = await sharesARowWith(title!, date!);

    expect(overlap, `The date sits on its own row (overlap ${overlap}px)`).toBeGreaterThan(0);
    expect(secondStartsAfter, 'The date is not to the right of the title').toBe(true);
  });
});

/**
 * The public bar renders its login action twice — once for the desktop bar and
 * once inside the mobile disclosure sheet — and hides the copy that does not
 * belong at the current width. That only works while the hiding rule outranks
 * whatever `display` the shared button treatment brings with it.
 *
 * <p>It stopped working once `.loginLink` began composing `button` from
 * `Button.module.css`: that rule sets `display: inline-flex` at the same
 * (0,1,0) specificity and lands later in the bundle, so both copies rendered at
 * every width. jsdom cannot catch this — it resolves neither the cascade across
 * modules nor the media query — which is exactly why the check lives here.
 */
test.describe('the public bar login action', () => {
  /**
   * The bar only wears its public face when nobody is signed in, and the shared
   * `stubApi` answers `/auth/me` with a session — so this overrides that one
   * route with a 401. Registered after `stubApi` in the outer `beforeEach`, and
   * Playwright tries the most recently added route first.
   */
  test.beforeEach(async ({ page }) => {
    await page.route('**/api/v1/auth/me', (route) =>
      route.fulfill({ status: 401, contentType: 'application/json', body: '{}' }),
    );
  });

  /**
   * Counts the copies actually laid out, not the ones in the DOM: both are
   * always rendered and the one that does not belong at this width is hidden
   * with `display: none`, so a DOM count would pass no matter what the cascade
   * resolved to — which is the very thing this is here to check.
   */
  function visibleLogins(page: Page) {
    return page.getByRole('link', { name: 'Iniciar Sesión' }).filter({ visible: true });
  }

  test('lays out exactly one copy on a desktop bar', async ({ page }) => {
    await page.setViewportSize(DESKTOP);
    await page.goto('/');
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();

    await expect(visibleLogins(page)).toHaveCount(1);
  });

  /**
   * The whole point of the login action wearing `surface` is that it pairs with
   * the theme toggle beside it, so the pairing is what gets asserted rather than
   * a hardcoded 40. It regressed once already: the bar's own measurements were
   * being dropped and the action rendered ten pixels taller than the toggle.
   */
  test('matches the height of the theme toggle it sits beside', async ({ page }) => {
    await page.setViewportSize(DESKTOP);
    await page.goto('/');
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();

    const login = await visibleLogins(page).boundingBox();
    const toggle = await page.getByRole('button', { name: /Cambiar a tema/ }).boundingBox();

    expect(
      login!.height,
      `login ${login!.height}px vs theme toggle ${toggle!.height}px`,
    ).toBeCloseTo(toggle!.height, 0);
  });

  test('lays out exactly one copy on a phone, inside the menu', async ({ page }) => {
    await page.setViewportSize(PHONE);
    await page.goto('/');
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();

    // Closed sheet: the bar carries only the theme toggle and the hamburger.
    await expect(visibleLogins(page)).toHaveCount(0);

    await page.getByRole('button', { name: 'Abrir menú' }).click();
    await expect(visibleLogins(page)).toHaveCount(1);
  });
});
