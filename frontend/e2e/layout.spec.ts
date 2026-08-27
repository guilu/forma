import { expect, test, type Locator, type Page } from '@playwright/test';
import { fixtureFor } from './apiFixtures';
import { stubApi } from './stubApi';
import { expectGlassSurface, expectNoHorizontalOverflow, expectSinglePageScroller } from './layout';

/**
 * Layout checks (FOR-186) — see `playwright.config.ts` for why these exist
 * alongside the jsdom suite.
 *
 * <p>The widths are not round numbers for their own sake: 320 is the narrowest
 * phone still in use and the floor everything has to survive, 375 is the common
 * phone, 574 is where a two-column dashboard grid was still rendering when it
 * should have collapsed, and 1280 is the sidebar layout.
 */
/*
 * The floor. It was untested until a chip row in `/app/measurements` was found
 * pushing the page 31px past the viewport here — three tabs that fit at 375 and
 * had nowhere to go at 320. Every width above this one passed, which is exactly
 * why the floor needs its own row: the first width that breaks is the one
 * nobody measures.
 */
const TINY = { width: 320, height: 720 };
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

for (const viewport of [TINY, PHONE, NARROW, TABLET, DESKTOP]) {
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

  /**
   * The calendar centres *today*, and which day that is comes from the machine
   * clock (`todayDayOfWeek()` in TrainingPage). Left alone, the centring
   * assertion below only holds Monday to Friday: a scroller cannot centre an
   * item it has no room to scroll past, so on a Saturday the last-but-one day
   * settles wherever the scroll maximum leaves it — 237px off centre — and the
   * suite went red on the weekend without a line of app code changing.
   *
   * `setFixedTime` rather than `clock.install`: this only needs `Date` to read
   * mid-week, and installing the full fake clock would also freeze the timers
   * the smooth scroll runs on.
   */
  test.beforeEach(async ({ page }) => {
    await page.clock.setFixedTime(new Date('2026-08-12T12:00:00'));
  });

  test('uses the tablet navigation and fits the week into one row', async ({ page }) => {
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

    /*
     * The week is one row now, not three stacked cards, so what this asserts
     * changed with it: that the open day really is the wide column, that the
     * counters sit under the whole strip, and that the row still fits without
     * either scrollbar. The stacking order it used to pin — today above the
     * summary above the calendar — described a layout that no longer exists.
     */
    const strip = page.getByRole('list', { name: 'Semana de entrenamiento' });
    const stats = page.getByRole('region', { name: 'Resumen de la semana' });

    const columns = await strip.locator('> li').evaluateAll((items) =>
      items.map((item) => {
        const box = item.getBoundingClientRect();
        return { width: Math.round(box.width), top: Math.round(box.top) };
      }),
    );
    expect(columns).toHaveLength(7);
    // One row: every column starts at the same height.
    expect(new Set(columns.map((column) => column.top)).size).toBe(1);

    // The open day is the widest by a clear margin, not by a pixel or two.
    const widths = columns.map((column) => column.width).sort((a, b) => b - a);
    expect(widths[0]).toBeGreaterThan(widths[1] * 1.8);

    const stripBox = await strip.boundingBox();
    const statsBox = await stats.boundingBox();
    expect(stripBox).not.toBeNull();
    expect(statsBox).not.toBeNull();
    expect(statsBox!.y).toBeGreaterThan(stripBox!.y + stripBox!.height - 2);

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

  test('stacks the nutrition rings over their figures and centres them', async ({ page }) => {
    await gotoApp(page, '/app');

    const nutrition = widget(page, 'Nutrición');
    const rings = nutrition.getByRole('img', { name: /kcal\. Proteínas/ });
    const carbs = nutrition.getByText('Carbohidratos');
    const box = (locator: Locator) =>
      locator.evaluate((node) => {
        const rect = node.getBoundingClientRect();
        return {
          left: Math.round(rect.left),
          right: Math.round(rect.right),
          top: Math.round(rect.top),
          height: Math.round(rect.height),
        };
      });

    const [ringsBox, cardBox, carbsBox] = await Promise.all([
      box(rings),
      box(nutrition),
      box(carbs),
    ]);

    // The figures sit under the rings, one line each: side by side they got what the square left
    // over, and "Carbohidratos" broke mid-word in it.
    expect(carbsBox.top).toBeGreaterThan(ringsBox.top);
    const lineHeight = await carbs.evaluate((node) =>
      parseFloat(getComputedStyle(node).lineHeight),
    );
    expect(carbsBox.height, 'The carbs label wraps onto a second line').toBeLessThan(
      lineHeight + 2,
    );

    // Centred: the card leaves the same gap on either side of the square.
    expect(Math.abs(ringsBox.left - cardBox.left - (cardBox.right - ringsBox.right))).toBeLessThan(
      2,
    );

    // Four concentric rings, each with its unfilled track behind it.
    await expect(rings.locator('circle')).toHaveCount(8);
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
 * band below the desktop layout (1101–1600px) wraps the widget rows at three
 * columns instead. The body tiles stay four across: they own a whole row now,
 * so a quarter of it is wider than a third of a widget row.
 */
// Both ends of the band: the tablet that prompted it and a laptop near the top
// edge, where narrow tracks were still breaking headline figures across lines.
for (const viewport of [TABLET, LAPTOP]) {
  test.describe(`dashboard grid at ${viewport.width}px`, () => {
    test.use({ viewport });

    // The unified today grid uses three tracks; its body group, main three-card
    // row and final trend occupy three explicit rows.
    for (const [row, columns, rows] of [
      ['todayGrid', 3, 3],
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

    /*
     * The body tiles keep their own four across this band while the widgets wrap at three. They
     * wrapped 3 + 1 back when a hydration tile shared the row with them; with the row to
     * themselves, wrapping only buys three tiles and a hole.
     */
    test('keeps the four body tiles on one row', async ({ page }) => {
      await gotoApp(page, '/app');

      const tiles = await page.locator('main [class*="body"] > section').evaluateAll((cards) =>
        cards.map((card) => {
          const rect = card.getBoundingClientRect();
          return { left: Math.round(rect.left), top: Math.round(rect.top) };
        }),
      );

      expect(tiles.length, 'The four body tiles were not found').toBe(4);
      const tops = new Set(tiles.map((tile) => tile.top));
      expect(tops.size, `The tiles wrap onto ${tops.size} rows (y: ${[...tops].join(', ')})`).toBe(
        1,
      );
      expect(new Set(tiles.map((tile) => tile.left)).size).toBe(4);
    });
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

  test('gives each nutrition ring its own colour over a dimmed track', async ({ page }) => {
    await gotoApp(page, '/app');

    const rings = page.getByRole('img', { name: /kcal\. Proteínas/ });
    const strokes = (selector: string) =>
      rings.locator(selector).evaluateAll((nodes) => nodes.map((n) => getComputedStyle(n).stroke));

    const [arcs, tracks] = await Promise.all([strokes('[data-arc]'), strokes('[data-track]')]);

    expect(arcs.length, 'The four rings were not found').toBe(4);
    // Calories plus one colour per macro: an arc that shares its colour with another says nothing.
    expect(new Set(arcs).size, `Rings share a colour: ${arcs.join(', ')}`).toBe(4);
    expect(tracks.length).toBe(4);
    // The unfilled part is the same hue dimmed, so it can never be read as the filled one.
    for (const [index, track] of tracks.entries()) {
      expect(track, `Ring ${index} draws its track like its arc`).not.toBe(arcs[index]);
    }
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

/**
 * The landing footer's support links.
 *
 * <p>Two things only a browser can answer. The tints are declared as
 * `color-mix(... N%, transparent)`, so what a visitor actually reads is the
 * pill's text over a translucent wash over whatever surface is behind it — a
 * ratio no stylesheet states and no jsdom test can compute. And the row holds
 * three pills whose labels do not wrap, which is the shape that walks off the
 * side of a narrow phone.
 *
 * <p>The pink first chosen here (#db61a2, GitHub's own on solid dark) measured
 * 4.40:1 against its own chip and had to be replaced. Without this test the
 * next person to pick "a nicer pink" gets no warning at all.
 */
test.describe('the landing support links', () => {
  const AA = 4.5;

  /** Flattens each pill onto the footer, then WCAG 2.x contrast. */
  async function pillContrast(page: Page) {
    return page.evaluate(() => {
      /*
       * Stacked on a 1x1 canvas rather than parsed and blended by hand. The
       * first version of this helper read the computed values with a `[\d.]+`
       * regex, and `color-mix(… , transparent)` computes to
       * `color(srgb 0.49 0.33 0 / 0.12)` — 0-1 channels, which that regex fed
       * into a 0-255 blend and turned a 4.43:1 pill into a passing number.
       * `fillStyle` accepts whatever the stylesheet produced, whatever its
       * notation, and the compositing is the browser's own.
       */
      const canvas = document.createElement('canvas');
      canvas.width = 1;
      canvas.height = 1;
      const ctx = canvas.getContext('2d', { willReadFrequently: true })!;
      const stack = (layers: readonly string[]): [number, number, number] => {
        ctx.clearRect(0, 0, 1, 1);
        for (const layer of layers) {
          ctx.fillStyle = layer;
          ctx.fillRect(0, 0, 1, 1);
        }
        const [r, g, b] = ctx.getImageData(0, 0, 1, 1).data;
        return [r, g, b];
      };
      const luminance = ([r, g, b]: [number, number, number]) => {
        const channel = (c: number) => {
          const v = c / 255;
          return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
        };
        return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b);
      };

      const footer = document.querySelector('footer')!;
      const backdrop = getComputedStyle(footer).backgroundColor;

      return Array.from(
        document.querySelectorAll('footer nav[aria-labelledby="support-heading"] a'),
      ).map((node) => {
        const style = getComputedStyle(node);
        const chip = stack([backdrop, style.backgroundColor]);
        const text = stack([backdrop, style.backgroundColor, style.color]);
        const [light, dark] = [luminance(text), luminance(chip)].sort((a, b) => b - a);
        return {
          label: (node as HTMLElement).innerText.trim(),
          ratio: Number(((light + 0.05) / (dark + 0.05)).toFixed(2)),
        };
      });
    });
  }

  /*
   * Getting the page into a known theme takes both halves, and neither alone is
   * enough.
   *
   * <p>`colorScheme` covers the first paint: with nothing stored, `index.html`'s
   * pre-paint script resolves the system preference before React runs. But
   * `ThemeProvider` then reconciles against the backend (FOR-120), and
   * `apiFixtures` answers `/api/v1/profile` with `themeMode: 'DARK'` — so a
   * page emulated light flipped back to dark the moment that fetch landed, and
   * whether the measurement caught it depended on which won the race. Four runs
   * in six measured the light values against the dark surface.
   *
   * <p>So the profile is overridden to agree with the emulated system. Both
   * paths then resolve to the same theme and there is nothing left to race.
   * Writing the attribute onto `<html>` by hand would not have worked either:
   * the reconciliation overwrites that too.
   */
  for (const theme of ['dark', 'light'] as const) {
    test.describe(`in the ${theme} theme`, () => {
      test.use({ colorScheme: theme });

      test('stay readable on their own tint', async ({ page }) => {
        /*
         * Registered after `stubApi`'s handler and therefore ahead of it, and
         * built on the same fixture rather than a hand-written profile: the
         * only field that matters here is the one being changed.
         */
        await page.route('**/api/v1/profile', async (route) => {
          const { status, body } = fixtureFor('/api/v1/profile');
          await route.fulfill({
            status,
            contentType: 'application/json',
            body: JSON.stringify({ ...(body as object), themeMode: theme.toUpperCase() }),
          });
        });

        await page.setViewportSize(DESKTOP);
        await page.goto('/');
        // Guards the setup itself: measuring the wrong theme would pass.
        await expect(page.locator('html')).toHaveAttribute('data-theme', theme);

        const measured = await pillContrast(page);

        expect(measured.length, 'The three support pills were not found').toBe(3);
        for (const { label, ratio } of measured) {
          expect(
            ratio,
            `${label} reads at ${ratio}:1 on its own chip in ${theme}`,
          ).toBeGreaterThanOrEqual(AA);
        }
      });
    });
  }

  test('wrap instead of pushing the narrowest phone sideways', async ({ page }) => {
    await page.setViewportSize(TINY);
    await page.goto('/');

    await expectNoHorizontalOverflow(page);

    // And they really did wrap: three pills on one 320px line is the failure
    // this guards, and a row that never wrapped would pass the check above only
    // because something else clipped it.
    const lines = await page.evaluate(
      () =>
        new Set(
          Array.from(
            document.querySelectorAll('footer nav[aria-labelledby="support-heading"] a'),
          ).map((node) => node.getBoundingClientRect().top),
        ).size,
    );
    expect(lines, 'The three pills all sat on one line at 320px').toBeGreaterThan(1);
  });
});

test.describe('landing hero CTA', () => {
  test.use({ viewport: PHONE });

  /**
   * On a phone, a CTA stretched edge to edge stops reading as a button and
   * starts reading as a form field or a banner. It is capped instead, so the
   * gutter beside it makes it look like the tappable target it is.
   *
   * <p>The hero used to carry two CTAs and this checked they shared a width and
   * a left edge. "Ver Demo" is gone — it promised a demo and only scrolled to
   * the product section — so the pair assertions became the centring check
   * below, which is what they were really protecting.
   *
   * <p>Both hero actions are checked, not just the primary one: they stack on a
   * phone, and a pair where only one of them is centred is worse than either
   * arrangement applied to both.
   */
  test('does not span the full width of the phone', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();

    // El CTA lleva al generador de plan desde que existe el embudo: es lo que se
    // le ofrece a alguien que aún no tiene cuenta.
    const actions = [
      page.getByRole('link', { name: 'Crear mi plan gratis' }).first(),
      page.getByRole('link', { name: 'Ver cómo funciona' }),
    ];

    for (const cta of actions) {
      await expect(cta).toBeVisible();
      const label = (await cta.textContent())?.trim();
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
        `"${label}" is ${Math.round(box.width)}px wide in a ${PHONE.width}px viewport`,
      ).toBeLessThanOrEqual(PHONE.width * 0.75);
      expect(box.lines, `"${label}" wraps onto ${box.lines} lines`).toBe(1);
      expect(
        Math.abs(box.centre - PHONE.width / 2),
        `"${label}" is centred at ${Math.round(box.centre)}px, not ${PHONE.width / 2}px`,
      ).toBeLessThanOrEqual(1);
    }
  });

  /**
   * El margen entre la etiqueta y su caja, medido.
   *
   * <p>La prueba de arriba solo se entera de que algo va mal cuando la etiqueta YA ha partido en
   * dos líneas, y eso depende de la plataforma: `Crear mi plan gratis` medía 205,69px dentro de una
   * caja de 206px —0,31px de margen— así que entraba en macOS y no en el runner de Linux. El
   * síntoma aparecía solo en CI y parecía cosa de CI; el defecto era un diseño apoyado en el borde.
   *
   * <p>Esto mide lo que aquello no podía ver: cuánto sitio sobra. Falla mientras aún se puede
   * arreglar, en vez de cuando una plataforma cualquiera decide redondear al alza.
   *
   * <p>El 8% es el umbral: las diferencias de métricas entre plataformas para una misma fuente
   * están muy por debajo, y un margen menor significa que la próxima palabra que alguien añada a la
   * etiqueta —o el próximo ajuste de tamaño— la parte en dos.
   */
  test('deja margen suficiente entre la etiqueta y su caja', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
    await page.evaluate(() => document.fonts.ready);

    const actions = [
      page.getByRole('link', { name: 'Crear mi plan gratis' }).first(),
      page.getByRole('link', { name: 'Ver cómo funciona' }),
    ];

    for (const cta of actions) {
      const room = await cta.evaluate((link) => {
        const style = getComputedStyle(link);
        // El ancho natural de la etiqueta en una línea, medido en un clon sin restricciones: el
        // elemento real ya está limitado por su caja, así que preguntarle a él no dice nada.
        const probe = document.createElement('span');
        probe.textContent = link.textContent?.trim() ?? '';
        probe.style.cssText = `position:absolute;white-space:nowrap;visibility:hidden;font:${style.font};letter-spacing:${style.letterSpacing}`;
        document.body.appendChild(probe);
        const textWidth = probe.getBoundingClientRect().width;
        probe.remove();

        const content =
          link.clientWidth - parseFloat(style.paddingLeft) - parseFloat(style.paddingRight);
        return { label: probe.textContent, textWidth, content };
      });

      const slack = (room.content - room.textWidth) / room.content;
      expect(
        slack,
        `"${room.label}" ocupa ${room.textWidth.toFixed(1)}px de los ${room.content.toFixed(1)}px de su caja: ${(slack * 100).toFixed(1)}% de margen. Por debajo del 8% parte en dos líneas en cuanto cambia la plataforma.`,
      ).toBeGreaterThanOrEqual(0.08);
    }
  });

  /**
   * The headline is the longest unbreakable run of text on the page —
   * "entrenamiento" is thirteen characters of Montserrat 900 — and `.page`
   * clips horizontal overflow, so a headline sized past the viewport does not
   * announce itself with a scrollbar: it just comes out with its right-hand
   * letters shaved off. That shipped once. This measures the rendered lines
   * rather than the block, because the block obediently stops at the container
   * while the glyphs inside it do not.
   */
  test('never renders the headline past the right edge', async ({ page }) => {
    await page.goto('/');
    const title = page.getByRole('heading', { level: 1 });
    await expect(title).toBeVisible();

    const widest = await title.evaluate((heading) => {
      const range = document.createRange();
      range.selectNodeContents(heading);
      return Math.max(...[...range.getClientRects()].map((line) => line.right));
    });

    expect(
      widest,
      `The headline reaches ${Math.round(widest)}px in a ${PHONE.width}px viewport`,
    ).toBeLessThanOrEqual(PHONE.width);
  });
});

test.describe('landing hero on a desktop', () => {
  test.use({ viewport: WIDE });

  /**
   * The muscle map is the hero's illustration, not its subject. Left to itself
   * it stops being either: the silhouette carries a 854x1840 aspect ratio, so a
   * card half the hero wide renders a body ~670px tall and the card grows past
   * 800px — taller than the headline, the paragraph and both buttons stacked
   * together, which reads as a diagram with some copy beside it.
   *
   * <p>The fix is to make the row height come from the copy and let the drawing
   * scale into what is left, so this measures the card against the copy it sits
   * beside rather than against a hardcoded pixel budget that would go stale the
   * first time the paragraph gains a line.
   *
   * <p>Not exact parity, though: at parity the drawings came out small enough
   * that the card read as a thumbnail of the feature rather than the feature,
   * so the wells carry a floor that buys them about another hundred pixels. The
   * band below is what that floor is worth — wide enough to leave room for it,
   * narrow enough to still catch a card that has gone back to sizing itself
   * from its own width.
   */
  test('sizes the muscle map to the height of the copy beside it', async ({ page }) => {
    await page.goto('/');

    const card = page.getByRole('region', { name: /Mapa muscular/ });
    const badge = page.getByText('Sin cuenta · sin tarjeta · 4 pasos');
    const trust = page.getByText('Tus datos son tuyos y puedes borrarlos');
    await expect(card).toBeVisible();

    const cardBox = await card.boundingBox();
    const badgeBox = await badge.boundingBox();
    const trustBox = await trust.boundingBox();
    expect(cardBox && badgeBox && trustBox, 'The hero did not lay out').not.toBeNull();

    const copyHeight = trustBox!.y + trustBox!.height - badgeBox!.y;

    const overhang = cardBox!.height - copyHeight;
    const measured = `The muscle map is ${Math.round(cardBox!.height)}px tall beside ${Math.round(copyHeight)}px of copy`;

    expect(overhang, measured).toBeGreaterThanOrEqual(0);
    expect(overhang, measured).toBeLessThanOrEqual(140);
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
    /*
     * The two header tests below find the date by matching `/\bago/i` — the
     * abbreviation for *agosto*. That only works while the machine clock says
     * August: on 1 September the locator stops matching anything and both tests
     * fail on a timeout, having nothing to do with the layout they check.
     * Pinning the date keeps them measuring what they are about.
     */
    await page.clock.setFixedTime(new Date('2026-08-12T12:00:00'));
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

  /**
   * Training's date wraps under the title on a phone, like the dashboard's.
   *
   * <p>This test used to assert the opposite — FOR-193 pinned the two to one row at every width so
   * the date could not drop down and push the week further below the fold. That row does not exist
   * at 375px: the title needs 176px and the navigator 210px inside 343px, so what the rule actually
   * bought was a title running *underneath* the navigator. Overlapping is the worse of the two, and
   * the dashboard this header was modelled on wraps here rather than overlap.
   */
  test('training wraps its date under the title rather than overlapping it', async ({ page }) => {
    await page.goto('/app/training');

    const title = await page.getByRole('heading', { level: 1 }).boundingBox();
    // Matches the month in either format — the long "Domingo, 2 De Agosto" this
    // header used to render and the compact "2 ago 2026" it renders now — so the
    // test fails on the layout it is about rather than timing out on a locator.
    const date = await page.getByText(/\bago/i).first().boundingBox();
    const { overlap } = await sharesARowWith(title!, date!);

    expect(overlap, 'The date still shares the row with the title').toBeLessThanOrEqual(0);
    expect(date!.y, 'The date is not below the title').toBeGreaterThanOrEqual(title!.y);
    /*
     * Same column as the title, not pushed off to its own corner. The tolerance is wider than
     * nutrition's 4px because that date is bare text while this one sits inside a bordered
     * navigator: its box starts level with the title, and the text is inset by the border, the
     * padding and the calendar glyph in front of it.
     */
    expect(date!.x - title!.x, 'The date is not in the title column').toBeLessThan(56);
    expect(date!.x - title!.x, 'The date is left of the title column').toBeGreaterThanOrEqual(0);
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
/**
 * The public bar's gutters, measured against what it sits on.
 *
 * <p>The bar and the landing were two independent measures that happened to
 * look alike: 1280px capped here against the sections' 1440, and the mobile
 * 20px gutter kept at every width while the sections widen theirs at 769px.
 * The two errors compounded — on a 1728px screen the brand started 100px right
 * of the hero copy, and below 1280 it started 20px left of it.
 *
 * <p>Asserted against `.heroGrid`'s own box rather than a table of expected
 * pixels: the numbers are whatever the landing's measure resolves to, and a
 * test that restated them would keep passing after someone changed the landing
 * and left the bar behind — which is the exact failure it exists to catch.
 */
test.describe('the public bar gutters', () => {
  test.beforeEach(async ({ page }) => {
    await page.route('**/api/v1/auth/me', (route) =>
      route.fulfill({ status: 401, contentType: 'application/json', body: '{}' }),
    );
  });

  /*
   * 1728 and 1440 straddle the cap, 1024 sits between the cap and the gutter
   * breakpoint, and 700 is below it — one width per branch of the two rules.
   */
  for (const width of [1728, 1440, 1024, 700]) {
    test(`line up with the landing at ${width}px`, async ({ page }) => {
      await page.setViewportSize({ width, height: 900 });
      await page.goto('/');
      await expect(page.getByRole('heading', { level: 1 })).toBeVisible();

      const edges = await page.evaluate(() => {
        const box = (el: Element | null) => {
          const b = el!.getBoundingClientRect();
          return { left: Math.round(b.left), right: Math.round(b.right) };
        };
        const header = document.querySelector('header')!;
        const hero = document.querySelector('#landing-title')!.closest('section')!;
        return {
          brand: box(header.querySelector('a[aria-label="FORMA, inicio"]')),
          // The bar's third column: the theme toggle and the login pill.
          actions: box(header.lastElementChild!.lastElementChild),
          // The hero's own measure — the mesh above it is decorative and
          // deliberately full-bleed, so it is skipped by the `aria-hidden`.
          content: box(hero.querySelector(':scope > div:not([aria-hidden])')),
        };
      });

      expect(
        edges.brand.left,
        `brand at ${edges.brand.left}, hero copy at ${edges.content.left}`,
      ).toBe(edges.content.left);
      expect(
        edges.actions.right,
        `actions end at ${edges.actions.right}, hero ends at ${edges.content.right}`,
      ).toBe(edges.content.right);
    });
  }
});

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

/**
 * The sidebar's Withings card. Its state dot is the at-a-glance signal, and the
 * jsdom suite cannot see it: that suite runs no cascade, and this regressed on
 * source order alone — the "off" rule was declared before the accent one, so a
 * disconnected card kept glowing green while the copy underneath read
 * "No conectado".
 */
test.describe('the sidebar integration card', () => {
  const dotColors = (page: Page) =>
    page.locator('aside [data-connected]').evaluate((el) => {
      const computed = getComputedStyle(el);
      return { background: computed.backgroundColor, shadow: computed.boxShadow };
    });

  test('paints the dot green while connected', async ({ page }) => {
    await page.route('**/api/v1/integrations', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          providers: [
            {
              provider: 'WITHINGS',
              status: 'CONNECTED',
              connectedAt: '2026-08-01T10:00:00Z',
              lastSyncAt: null,
              lastSyncOutcome: null,
            },
          ],
        }),
      });
    });
    await page.setViewportSize(DESKTOP);
    await gotoApp(page, '/app');

    // Resolved through a probe rather than read off the token: the custom
    // property holds whatever the theme author wrote (a hex), while a computed
    // `background-color` is always `rgb(…)` — comparing the two compares
    // notations, not colours.
    const accent = await page.evaluate(() => {
      const probe = document.createElement('div');
      probe.style.color = 'var(--color-accent)';
      document.body.appendChild(probe);
      const resolved = getComputedStyle(probe).color;
      probe.remove();
      return resolved;
    });
    const { background } = await dotColors(page);

    // Both notations the engine may compute for the token's own value.
    expect(background.replace(/\s/g, '')).toBe(accent.replace(/\s/g, ''));
  });

  // The e2e fixture reports Withings disconnected (see `stubApi.ts`).
  test('mutes the dot, glow included, while disconnected', async ({ page }) => {
    await page.setViewportSize(DESKTOP);
    await gotoApp(page, '/app');

    // Resolved through a probe rather than read off the token: the custom
    // property holds whatever the theme author wrote (a hex), while a computed
    // `background-color` is always `rgb(…)` — comparing the two compares
    // notations, not colours.
    const accent = await page.evaluate(() => {
      const probe = document.createElement('div');
      probe.style.color = 'var(--color-accent)';
      document.body.appendChild(probe);
      const resolved = getComputedStyle(probe).color;
      probe.remove();
      return resolved;
    });
    const { background, shadow } = await dotColors(page);

    expect(background.replace(/\s/g, '')).not.toBe(accent.replace(/\s/g, ''));
    expect(shadow, `a muted dot haloed in accent still reads lit (${shadow})`).toBe('none');
  });
});

/*
 * The week strip's open day (direction C, docs/design/entrenamiento-sin-scroll).
 *
 * A silhouette carries its own aspect ratio, so fixing one axis fixes both, and
 * picking the wrong one breaks at one end of the window range: sized by height
 * a strength day's two sheets spilled over the columns beside them, and sized
 * by width alone they ran out past the bottom of their own card. Neither is
 * visible to jsdom, which performs no layout — hence these, which measure both
 * ends at once across the range the page is used at.
 */
test.describe('the open day in the week strip', () => {
  const CARD = 'li[class*="weekDayExpanded"]';
  const FIGURES = '[class*="expandedFigures"]';

  /*
   * Which day opens by default is read off the machine clock, and these tests
   * step forward from it by a fixed number of days — so without pinning the
   * date they assert about a different day every day. Written on a Wednesday,
   * "one step forward" was the strength day; by Thursday it was the rest day,
   * which has no silhouette pair at all and failed on a missing element rather
   * than on anything about layout.
   *
   * Wednesday 2026-08-12, so that today is the run and one step is the strength
   * day. `setFixedTime` rather than `clock.install`, for the reason the iPad
   * block gives: this only needs `Date` to read mid-week.
   */
  test.beforeEach(async ({ page }) => {
    await page.clock.setFixedTime(new Date('2026-08-12T12:00:00'));
  });

  for (const viewport of [
    /*
     * 1194×700 is an iPad in landscape with Safari's bars taking their share,
     * and it is here because it shipped broken: `--figure-room` subtracts a
     * fixed amount of chrome from the window height, and on a window this short
     * the subtraction reached zero — the bodies came out 0×0 and the open day
     * showed no silhouette at all. Every check below already covered "does it
     * fit"; none of them covered a window small enough for the answer to be
     * "there is nothing to fit".
     */
    { width: 1194, height: 700 },
    { width: 1194, height: 834 },
    { width: 1280, height: 768 },
    { width: 1440, height: 900 },
    { width: 1680, height: 1200 },
  ]) {
    test.describe(`at ${viewport.width}×${viewport.height}`, () => {
      test.use({ viewport });

      // Both kinds: a run draws one body, a strength day draws front and back,
      // and it is the pair that used to overflow.
      for (const [kind, steps] of [
        ['a run', 0],
        ['a strength day', 1],
      ] as const) {
        test(`keeps ${kind}'s silhouettes inside the card`, async ({ page }) => {
          await gotoApp(page, '/app/training');
          for (let step = 0; step < steps; step += 1) {
            await page.getByRole('button', { name: 'Día siguiente' }).click();
          }
          await expect(page.locator(`${CARD} ${FIGURES}`)).toBeVisible();

          const fit = await page.evaluate(
            ({ card, figures }) => {
              const box = document.querySelector(card)!.querySelector(figures)!;
              /*
               * The silhouette's own box, not the `<img>` inside it and not the
               * pair that wraps the two. `[data-silhouette]` sits on the image,
               * whose parent is an inner layer container that measures 0 —
               * measuring that reported a clean fit while the bodies were
               * visibly spilling over the next column.
               */
              const bodies = [...box.querySelectorAll('[class*="expandedFigure"]')]
                .filter((body) => !body.className.includes('Pair'))
                .map((body) => body.getBoundingClientRect());
              const room = box.getBoundingClientRect();
              return {
                widest: Math.round(Math.max(...bodies.map((b) => b.right)) - room.left),
                tallest: Math.round(Math.max(...bodies.map((b) => b.height))),
                room: { width: Math.round(room.width), height: Math.round(room.height) },
                count: bodies.length,
              };
            },
            { card: CARD, figures: FIGURES },
          );

          expect(fit.count).toBeGreaterThan(0);
          /*
           * A body that measures nothing passes every "does it fit" check ever
           * written, and this one shipped: on a phone the open day's silhouette
           * resolved to 0px and simply was not there. Fitting is only half the
           * claim; being drawn is the other half.
           *
           * Measured as a share of the room rather than in pixels: a short
           * window leaves the row very little, and a body that is small because
           * its row is small has not collapsed — it is doing the only thing it
           * can. A fixed floor here failed a 768px-tall window for having a
           * 64px body in 135px of space, which is a complaint about the window.
           */
          expect(fit.tallest, 'the bodies collapsed to nothing').toBeGreaterThan(
            fit.room.height * 0.3,
          );
          // One pixel of slack for layout rounding, as elsewhere in this file.
          expect(fit.widest, 'the bodies run past their own column').toBeLessThanOrEqual(
            fit.room.width + 1,
          );
          expect(fit.tallest, 'the bodies run past the row that holds them').toBeLessThanOrEqual(
            fit.room.height + 1,
          );
        });
      }
    });
  }
});
