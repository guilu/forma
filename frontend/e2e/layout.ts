import { expect, type Page } from '@playwright/test';

/**
 * The measurements the jsdom suite cannot make. Each helper reports *which*
 * element is at fault, because "the page is 12px too wide" without a culprit
 * is a bug report you have to re-investigate by hand.
 */

/** Sub-pixel slack: layout rounding, not overflow. */
const TOLERANCE = 1;

/**
 * Nothing may be wider than the viewport. The usual cause is a grid or flex
 * item whose automatic minimum size is its content size, which is why the
 * report names the widest offenders rather than just failing.
 */
export async function expectNoHorizontalOverflow(page: Page): Promise<void> {
  const report = await page.evaluate((tolerance) => {
    const doc = document.documentElement;
    const overflowing = [...document.querySelectorAll<HTMLElement>('body *')]
      .filter((el) => el.getBoundingClientRect().right > doc.clientWidth + tolerance)
      .slice(0, 5)
      .map((el) => {
        const rect = el.getBoundingClientRect();
        return `${el.tagName.toLowerCase()}.${el.className || '(no class)'} → right ${Math.round(rect.right)}px`;
      });

    return { scrollWidth: doc.scrollWidth, clientWidth: doc.clientWidth, overflowing };
  }, TOLERANCE);

  expect(
    report.scrollWidth,
    `Page scrolls horizontally (${report.scrollWidth}px in a ${report.clientWidth}px viewport). Widest offenders:\n${report.overflowing.join('\n') || '(none past the viewport edge — check a margin or a negative offset)'}`,
  ).toBeLessThanOrEqual(report.clientWidth + TOLERANCE);
}

/**
 * The page is the only thing that scrolls vertically. A second scroll
 * container nested inside a viewport-height document is the setup that puts
 * two scrollbars side by side and leaves an empty band under the content.
 *
 * <p>An element that *may* scroll but has nothing to scroll is not a scroller,
 * so the check compares content against box, not the `overflow` value alone.
 */
export async function expectSinglePageScroller(page: Page): Promise<void> {
  const scrollers = await page.evaluate(
    (tolerance) =>
      [...document.querySelectorAll<HTMLElement>('body *')]
        .filter((el) => {
          const overflowY = getComputedStyle(el).overflowY;
          const scrollable = overflowY === 'auto' || overflowY === 'scroll';
          return scrollable && el.scrollHeight > el.clientHeight + tolerance;
        })
        .map(
          (el) =>
            `${el.tagName.toLowerCase()}.${el.className || '(no class)'} → ${el.scrollHeight}px of content in ${el.clientHeight}px`,
        ),
    TOLERANCE,
  );

  expect(
    scrollers,
    `Expected the page to be the only vertical scroller, but these also scroll:\n${scrollers.join('\n')}`,
  ).toEqual([]);
}

/**
 * A floating chrome surface actually renders the "glass" treatment: a
 * translucent fill *and* a live blur.
 *
 * <p>Worth its own check because the two can silently come apart. A later rule
 * of equal specificity re-declaring the opaque fallback leaves `backdrop-filter`
 * applied but painted behind an opaque fill — invisible, and indistinguishable
 * from "no blur" by reading the stylesheet.
 */
export async function expectGlassSurface(
  page: Page,
  selector: string,
  /** For a surface whose fill lives on a pseudo-element, e.g. `'::before'`. */
  pseudo?: string,
): Promise<void> {
  const style = await page
    .locator(selector)
    .first()
    .evaluate((el, pseudoElement) => {
      const computed = getComputedStyle(el, pseudoElement ?? null);
      return {
        background: computed.backgroundColor,
        backdropFilter: computed.backdropFilter,
      };
    }, pseudo);

  const where = pseudo ? `${selector}${pseudo}` : selector;
  expect(style.backdropFilter, `${where} has no backdrop blur`).not.toBe('none');
  // A fractional alpha in either notation the engine may compute: `rgba(…, .8)`
  // for a literal colour, `color(srgb … / .8)` for a `color-mix()` result.
  expect(
    style.background,
    `${where} is painted opaque (${style.background}), so its blur cannot show through`,
  ).toMatch(/[,/]\s*0?\.\d+\s*\)/);
}
