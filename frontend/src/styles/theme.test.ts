import { describe, expect, it } from 'vitest';
import themeCss from './theme.css?raw';

/**
 * FOR-163: source-level guard for the design-token reconciliation between
 * `theme.css` and the approved mockup templates (`docs/*.html` inline
 * `tailwind.config` token blocks).
 *
 * jsdom cannot reliably resolve CSS custom properties to computed styles
 * (see the FOR-62 comment in `theme/themedRendering.test.tsx`), so this reads
 * `theme.css` as raw text (Vite's `?raw` import) and asserts the literal
 * declarations instead of rendered output. That is the only meaningful
 * automated check for a pure-token/value change — it guards against silent
 * drift back to the pre-reconciliation values.
 */

/** Extracts the `:root, :root[data-theme='dark']` block (dark/default tokens). */
function darkBlock(css: string): string {
  const start = css.indexOf(":root,\n:root[data-theme='dark']");
  const openBrace = css.indexOf('{', start);
  const closeBrace = css.indexOf('\n}', openBrace);
  if (start === -1 || openBrace === -1 || closeBrace === -1) {
    throw new Error('Could not locate the dark token block in theme.css');
  }
  return css.slice(openBrace, closeBrace);
}

/** Extracts the `:root[data-theme='light']` override block. */
function lightBlock(css: string): string {
  const start = css.indexOf(":root[data-theme='light']");
  const openBrace = css.indexOf('{', start);
  const closeBrace = css.indexOf('\n}', openBrace);
  if (start === -1 || openBrace === -1 || closeBrace === -1) {
    throw new Error('Could not locate the light token block in theme.css');
  }
  return css.slice(openBrace, closeBrace);
}

/** Reads a single custom property's declared value out of a CSS block. */
function tokenValue(block: string, name: string): string | null {
  const re = new RegExp(`${name}:\\s*([^;]+);`);
  const match = block.match(re);
  return match ? match[1].trim() : null;
}

/** All `--foo` custom property names declared in a CSS block. */
function tokenNames(block: string): string[] {
  return [...block.matchAll(/(--[a-z0-9-]+):/gi)].map((m) => m[1]);
}

describe('theme.css design tokens (FOR-163 reconciliation)', () => {
  const dark = darkBlock(themeCss);
  const light = lightBlock(themeCss);

  describe('dark tokens reconciled to the approved template values', () => {
    it.each([
      // [token, expected value, template source token]
      ['--color-bg', '#10141a', 'background / surface / surface-dim'],
      ['--color-surface', '#161b22', 'surface-elevated'],
      ['--color-card', '#1c2026', 'surface-container'],
      ['--color-border', '#30363d', 'surface-stroke'],
      ['--color-text', '#dfe2eb', 'on-surface / on-background'],
      ['--color-text-muted', '#8b949e', 'text-dimmed'],
      ['--color-accent', '#4cdf97', 'primary'],
      ['--color-accent-contrast', '#003920', 'on-primary'],
      ['--color-warning', '#ffab70', 'warning-amber'],
      ['--color-danger', '#ff5757', 'error-pulse'],
    ])('%s reconciles to %s (template: %s)', (token, expected) => {
      expect(tokenValue(dark, token)).toBe(expected);
    });
  });

  describe('light counterparts stay valid for every touched token', () => {
    const colorTokens = [
      '--color-bg',
      '--color-surface',
      '--color-card',
      '--color-border',
      '--color-text',
      '--color-text-muted',
      '--color-accent',
      '--color-accent-contrast',
      '--color-warning',
      '--color-danger',
      '--shadow-card',
    ];

    it.each(colorTokens)('%s is defined (non-empty) in both themes', (token) => {
      expect(tokenValue(dark, token)).toBeTruthy();
      expect(tokenValue(light, token)).toBeTruthy();
    });

    it('reconciles the light accent to the template inverse-primary, not the raw dark hue', () => {
      // Templates are dark-only; the light accent is derived (documented in
      // theme.css) from the template's own `inverse-primary` role (#006d42),
      // which is the M3-designed variant of `primary` for opposite-brightness
      // surfaces -- not an invented value.
      expect(tokenValue(light, '--color-accent')).toBe('#006d42');
      expect(tokenValue(light, '--color-accent-contrast')).toBe('#ffffff');
    });

    it('keeps the light warning accessible (AA) after the amber hue reconciliation', () => {
      // Pre-existing #b7791f only reached ~3.38:1 on the light bg (fails AA);
      // reconciling to the template's amber hue also fixes that regression.
      expect(tokenValue(light, '--color-warning')).toBe('#8f4d13');
    });
  });

  describe('spacing / radius reconciled to the template scale', () => {
    it('keeps the spacing steps that already matched the template 1:1', () => {
      expect(tokenValue(dark, '--space-2')).toBe('8px'); // template: base / stack-gap-sm
      expect(tokenValue(dark, '--space-4')).toBe('16px'); // template: stack-gap-md
      expect(tokenValue(dark, '--space-5')).toBe('24px'); // template: card-padding / gutter
      expect(tokenValue(dark, '--space-6')).toBe('32px'); // template: stack-gap-lg
    });

    it('adds the template container-margin steps with no current equivalent', () => {
      expect(tokenValue(dark, '--space-container-mobile')).toBe('20px');
      expect(tokenValue(dark, '--space-container-desktop')).toBe('40px');
    });

    it('reconciles the radius scale to the template values', () => {
      expect(tokenValue(dark, '--radius-sm')).toBe('4px'); // template: DEFAULT
      expect(tokenValue(dark, '--radius-md')).toBe('8px'); // template: lg
      expect(tokenValue(dark, '--radius-lg')).toBe('12px'); // template: xl
      expect(tokenValue(dark, '--radius-full')).toBe('9999px'); // template: full (new)
    });
  });

  describe('typography reconciled to the bundled template fonts', () => {
    it('wires --font-sans to the self-hosted Be Vietnam Pro body font (no CDN)', () => {
      const value = tokenValue(dark, '--font-sans');
      expect(value).toContain("'Be Vietnam Pro'");
      expect(value).not.toContain('Poppins');
    });

    it('adds --font-heading for the self-hosted Montserrat headline font (no CDN)', () => {
      const value = tokenValue(dark, '--font-heading');
      expect(value).toContain("'Montserrat'");
    });

    it('reconciles font-size-base to the template body-md size, keeps the already-matching sizes', () => {
      expect(tokenValue(dark, '--font-size-base')).toBe('1rem'); // template body-md: 16px
      expect(tokenValue(dark, '--font-size-lg')).toBe('1.125rem'); // template body-lg: 18px (already matched)
      expect(tokenValue(dark, '--font-size-xl')).toBe('1.5rem'); // template headline-lg-mobile: 24px (already matched)
    });
  });

  describe('no CDN dependency leaks into the token layer', () => {
    it('never references fonts.googleapis.com or a CDN url()', () => {
      expect(themeCss).not.toMatch(/fonts\.googleapis\.com/);
      expect(themeCss).not.toMatch(/@import\s+url\(/);
    });
  });

  describe('every dark color token has a same-named declaration reachable in light mode', () => {
    it('does not leave the light theme with a var missing from the dark set', () => {
      // Every color-role name declared in dark must also resolve for light —
      // either overridden in the light block, or (for theme-invariant tokens
      // like spacing/radius/font-size) inherited from the shared `:root`
      // selector, which the light block does not need to repeat.
      const themeInvariant = new Set(tokenNames(dark).filter((n) => !n.startsWith('--color')));
      const darkColorTokens = tokenNames(dark).filter((n) => n.startsWith('--color'));
      const lightNames = new Set(tokenNames(light));

      for (const name of darkColorTokens) {
        expect(lightNames.has(name), `expected light block to override ${name}`).toBe(true);
      }
      // Sanity: theme-invariant tokens are intentionally NOT duplicated in light.
      expect(themeInvariant.has('--radius-full')).toBe(true);
      expect(lightNames.has('--radius-full')).toBe(false);
    });
  });
});
