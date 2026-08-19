import type { Plugin } from 'vite';
import { fixtureFor } from './e2e/apiFixtures';

/*
 * The pieces of Node's request and response this middleware touches, named
 * here rather than imported. `@types/node` has no `types` fence around it, so
 * installing it would make Node's globals visible to the application sources
 * too — the same reason playwright.config.ts declares `process` by hand.
 * Without those types Vite's own `IncomingMessage` resolves to an empty stub,
 * so the two arguments are narrowed at the point of use instead of in the
 * handler's signature, where an empty type makes every annotation unassignable.
 */
interface FixtureRequest {
  readonly url?: string;
}

interface FixtureReply {
  statusCode: number;
  setHeader(name: string, value: string): void;
  end(body: string): void;
}

/**
 * Serves the API from the e2e fixtures, in the dev server.
 *
 * <p>Why this exists: the signed-in app is unreachable from `npm run dev`
 * alone. Authentication is server-side — `AuthContext` asks
 * `/api/v1/auth/me` and treats a 401 as anonymous — so with no backend running
 * the landing page is all you can browse, and on a machine without Docker
 * that is every machine. `npm run playground` solved it by intercepting the
 * API inside a Playwright browser, but that browser's viewport is pinned by
 * the `Desktop Chrome` device preset, so it cannot be resized: no way to look
 * at a layout at 1440×900, or at a phone width, or to open the responsive
 * tools.
 *
 * <p>This moves the same interception one layer down, into the dev server, so
 * the app is browsable in an ordinary browser at any window size, with real
 * devtools and hot reload. The fixtures are the ones `stubApi` already used
 * (`e2e/apiFixtures`), so an endpoint added for a layout check is served here
 * too and neither can drift from the other.
 *
 * <p><strong>Development only, and opt-in.</strong> It is installed only when
 * `FIXTURES=1` is set (`npm run dev:fixtures`), so a plain `npm run dev` still
 * proxies to a real backend on :8080 and nothing about a production build sees
 * this file. It also refuses to install for `vite build`, which is belt and
 * braces: fixture data must never reach a bundle.
 */
export function devApiFixtures(): Plugin {
  return {
    name: 'forma-dev-api-fixtures',
    apply: 'serve',
    configureServer(server) {
      /*
       * Registered from `configureServer` without the returned-function form,
       * so it lands *before* Vite's internal middlewares — the `/api` proxy to
       * :8080 among them. Deferred, the proxy would answer first and every
       * request would hang on a backend that is not running.
       */
      server.middlewares.use((req, res, next) => {
        const url = (req as FixtureRequest).url ?? '';
        // The versioned prefix, not `/api`: the app's own source modules are
        // served from `/src/api/…` in dev, and a looser test would answer those
        // with JSON — the page then loads no JavaScript at all.
        if (!url.startsWith('/api/v1/')) {
          next();
          return;
        }

        // Split rather than `new URL`: this file compiles under a Node-only
        // tsconfig with no DOM lib, where `URL` is a type and not a value.
        const { status, body } = fixtureFor(url.split('?')[0]);
        const reply = res as unknown as FixtureReply;
        reply.statusCode = status;
        reply.setHeader('Content-Type', 'application/json');
        reply.end(JSON.stringify(body));
      });

      server.config.logger.info(
        '\n  [33m➜[0m  API servida desde los fixtures de e2e — sin backend, sesión falsa.\n',
      );
    },
  };
}
