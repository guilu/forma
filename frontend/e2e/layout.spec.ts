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
const DESKTOP = { width: 1280, height: 900 };

const APP_ROUTES = ['/app', '/app/measurements', '/app/goals'] as const;

test.beforeEach(async ({ page }) => {
  await stubApi(page);
});

/** Waits for the route's own content, not just the shell around it. */
async function gotoApp(page: Page, path: string): Promise<void> {
  await page.goto(path);
  await expect(page.getByRole('main')).toBeVisible();
  // Charts size themselves from a measured box, so give the first paint a
  // chance to settle before measuring anything.
  await page.waitForFunction(() => document.fonts.ready.then(() => true));
}

for (const viewport of [PHONE, NARROW, DESKTOP]) {
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

test.describe('dashboard rows', () => {
  test.use({ viewport: DESKTOP });

  // CSS-module class names are hashed, so a substring match on the authored
  // name is the stable way to reach a layout container from outside the app.
  for (const row of ['metrics', 'rowFour', 'rowThree']) {
    test(`gives every card in the ${row} row the same height`, async ({ page }) => {
      await gotoApp(page, '/app');

      const heights = await page
        .locator(`main [class*="${row}"] > *`)
        .evaluateAll((cards) => cards.map((c) => Math.round(c.getBoundingClientRect().height)));

      expect(heights.length, `No cards found in the ${row} row`).toBeGreaterThan(1);
      expect(
        new Set(heights).size,
        `Cards in the ${row} row have differing heights: ${heights.join(', ')}`,
      ).toBe(1);
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
