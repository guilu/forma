import { test } from '@playwright/test';
import { stubApi } from './stubApi';

/*
 * Declared here rather than pulled in with `@types/node`, for the reason
 * playwright.config.ts gives: that package has no `types` fence, so installing
 * it would make Node's globals visible to the application sources too.
 */
declare const process: { readonly env: Record<string, string | undefined> };

/**
 * Not a test — a way to *drive* the app by hand with no backend running.
 *
 * <p>The signed-in app is unreachable from `npm run dev` alone: authentication
 * is server-side (`AuthContext` asks `/api/v1/auth/me`, and a 401 means
 * anonymous), so without a backend the landing is all you can browse. The
 * layout checks do not have that problem because they intercept the API in the
 * browser, and `stubApi` is exactly that interception — including an
 * `/api/v1/auth/me` fixture, which is what makes the session real as far as the
 * app is concerned.
 *
 * <p>So this opens a real Chromium with those same fixtures and then parks on
 * `page.pause()`, handing control to the Playwright Inspector: click through the
 * app, open DevTools, resize, toggle the theme. The dev server behind it is the
 * ordinary one, so hot reload still applies as you edit.
 *
 * <p>Skipped unless `PLAYGROUND=1`, or `npm run test:layout` would sit forever
 * waiting for someone to close a browser that CI never opened.
 *
 * <p>Landing somewhere that renders an error usually means that route's
 * endpoint has no fixture yet — unstubbed paths answer 404 by design. Add it to
 * `FIXTURES` in `stubApi.ts` and the layout checks gain the coverage too.
 */
test.describe('playground', () => {
  test.skip(process.env.PLAYGROUND !== '1', 'Manual only — run `npm run playground`.');

  test('browse the app signed in, with the API served from the e2e fixtures', async ({ page }) => {
    // No deadline: the whole point is to sit here as long as you are poking at
    // the page.
    test.setTimeout(0);

    await stubApi(page);
    await page.goto(process.env.PLAYGROUND_PATH ?? '/app');
    await page.pause();
  });
});
