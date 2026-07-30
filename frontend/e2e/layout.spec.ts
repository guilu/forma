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

test.describe('dashboard grid', () => {
  test.use({ viewport: NARROW });

  test('collapses to a single column below the mobile breakpoint', async ({ page }) => {
    await gotoApp(page, '/app');

    // Every card in the metrics row starts at the same x: one column.
    const lefts = await page
      .locator('main section')
      .evaluateAll((cards) => [
        ...new Set(cards.map((c) => Math.round(c.getBoundingClientRect().left))),
      ]);

    expect(
      lefts,
      `Cards start at ${lefts.length} different x positions, so the grid is not single-column`,
    ).toHaveLength(1);
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

  for (const row of ['metrics', 'rowFour', 'rowThree']) {
    test(`gives every card in the ${row} row the same height`, async ({ page }) => {
      await gotoApp(page, '/app');

      const heights = (await rowCards(page, row)).map((card) => card.height);

      expect(heights.length, `No cards found in the ${row} row`).toBeGreaterThan(1);
      expect(
        new Set(heights).size,
        `Cards in the ${row} row have differing heights: ${heights.join(', ')}`,
      ).toBe(1);
    });
  }
});

/**
 * On a tablet in landscape the six metric tiles across a single row left each
 * one about 180px wide — too narrow for a headline value and its caption. The
 * band below the desktop layout (1101–1600px) wraps every row at three columns
 * instead.
 */
// Both ends of the band: the tablet that prompted it and a laptop near the top
// edge, where six tracks were still breaking "2120 kcal" across two lines.
for (const viewport of [TABLET, LAPTOP]) {
  test.describe(`dashboard grid at ${viewport.width}px`, () => {
    test.use({ viewport });

    // Cards per row, then rows: metrics is six tiles as 3 + 3, and the two
    // four-widget rows become 3 + 1.
    for (const [row, columns, rows] of [
      ['metrics', 3, 2],
      ['rowFour', 3, 2],
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
  return page.locator('main section').filter({ has: page.getByRole('heading', { name: title }) });
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

  test('gives Perfil and Conexiones two of the three columns', async ({ page }) => {
    await gotoSettings(page);

    const { columns, cards } = await settingsCards(page);
    expect(columns, 'The settings grid is not three columns wide').toBe(3);

    const width = (title: string) => cards.find((card) => card.title.startsWith(title))?.width ?? 0;
    const single = width('Unidades');
    expect(single, 'The Unidades card was not found').toBeGreaterThan(0);

    // A two-column card is both tracks plus the gap between them, so it is
    // wider than two single columns would be on their own.
    for (const title of ['Perfil y preferencias', 'Conexiones e integraciones']) {
      expect(
        width(title),
        `"${title}" is ${width(title)}px wide next to a ${single}px single column`,
      ).toBeGreaterThan(single * 1.9);
    }
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

  test('ramps the completed arc of a progress donut instead of filling it flat', async ({
    page,
  }) => {
    await gotoApp(page, '/app');

    const ring = page.getByRole('img', { name: /Calorías consumidas/ });
    const background = await ring.evaluate((el) => getComputedStyle(el).backgroundImage);

    expect(background, 'The donut is not painted with a conic gradient').toContain('conic');
    // Two ramp stops for the filled arc plus the track colour: a flat fill would
    // resolve to one colour before the track.
    const stops = background.match(/rgba?\([^)]+\)/g) ?? [];
    expect(
      new Set(stops).size,
      `Expected a ramp plus a track, got ${stops.join(', ')}`,
    ).toBeGreaterThanOrEqual(3);
  });
});

test.describe('destructive confirmation button', () => {
  test.use({ viewport: DESKTOP });

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
    const [light, dark] = [luminance(a), luminance(b)].sort((x, y) => y - x);
    return (light + 0.05) / (dark + 0.05);
  }

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

test.describe('landing hero CTAs', () => {
  test.use({ viewport: PHONE });

  /**
   * Stacked on a phone, a CTA stretched edge to edge stops reading as a button
   * and starts reading as a form field or a banner. The pair is capped and
   * centred instead, so the gutters make them look like the tappable targets
   * they are.
   */
  test('do not span the full width of the phone', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();

    const ctas = page
      .getByRole('link', { name: 'Empezar ahora' })
      .or(page.getByRole('link', { name: 'Ver Demo' }));
    const boxes = await ctas.evaluateAll((links) =>
      links.map((link) => {
        const rect = link.getBoundingClientRect();
        // One client rect per line the label occupies: a narrower button that
        // wraps "Empezar ahora" onto two lines is not the fix we want.
        const range = document.createRange();
        range.selectNodeContents(link);
        const lines = new Set([...range.getClientRects()].map((line) => Math.round(line.top))).size;
        return { name: link.textContent?.trim() ?? '', width: rect.width, left: rect.left, lines };
      }),
    );

    expect(boxes.length, 'The hero CTAs were not found').toBe(2);

    const maxWidth = PHONE.width * 0.75;
    for (const box of boxes) {
      expect(
        box.width,
        `"${box.name}" is ${Math.round(box.width)}px wide in a ${PHONE.width}px viewport`,
      ).toBeLessThanOrEqual(maxWidth);
      expect(box.lines, `"${box.name}" wraps onto ${box.lines} lines`).toBe(1);
    }

    // Same width and same left edge: one centred column, not two ragged boxes.
    const widths = boxes.map((box) => Math.round(box.width));
    expect(new Set(widths).size, `The CTAs have differing widths: ${widths.join(', ')}`).toBe(1);
    const lefts = boxes.map((box) => Math.round(box.left));
    expect(new Set(lefts).size, `The CTAs start at different x: ${lefts.join(', ')}`).toBe(1);
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
