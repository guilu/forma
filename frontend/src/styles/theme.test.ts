import { describe, expect, it } from 'vitest';
import foodFormCss from '../pages/admin/FoodForm.module.css?raw';
import planGeneratorCss from '../pages/generator/PlanGenerator.module.css?raw';
import nutritionCss from '../pages/NutritionPage.module.css?raw';
import nutritionPageSource from '../pages/NutritionPage.tsx?raw';
import trainingCss from '../pages/TrainingPage.module.css?raw';
import trainingPageSource from '../pages/TrainingPage.tsx?raw';
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
      ['--color-accent', '#63e662', 'primary'],
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

    it('pins the light accent to the reference green and its readable ink', () => {
      expect(tokenValue(light, '--color-accent')).toBe('#63e662');
      expect(tokenValue(light, '--color-accent-contrast')).toBe('#0f1a13');
    });

    it('splits the text-safe accent out from the fill accent (FOR-185)', () => {
      // The selected green is a fill colour: ~1.38:1 on the light page background,
      // far below the AA 4.5:1 bar. Accent-coloured *text* uses the darkened
      // counterpart instead (~4.99:1). Dark needs no split — its accent is
      // already has strong contrast on its own background — but declares the token anyway
      // so component CSS can name the text role unconditionally.
      expect(tokenValue(light, '--color-accent-strong')).toBe('#3e7810');
      expect(tokenValue(dark, '--color-accent-strong')).toBe(tokenValue(dark, '--color-accent'));
      expect(tokenValue(light, '--color-accent-strong')).not.toBe(
        tokenValue(light, '--color-accent'),
      );
    });

    it('keeps the CTA gradient and its ink identical in both themes (FOR-185)', () => {
      // The brand owner wants one bright ramp everywhere, so light does not
      // derive a darker gradient from its own endpoints. The ink must travel
      // with it: white on #63e662 is low contrast, while the dark ink remains readable.
      expect(tokenValue(light, '--gradient-accent')).toBe(tokenValue(dark, '--gradient-accent'));
      expect(tokenValue(light, '--color-on-gradient')).toBe(
        tokenValue(dark, '--color-on-gradient'),
      );
    });

    it('keeps warning text accessible (AA) beside the exact reference graphic colour', () => {
      // Exact #f19c2b is reserved for marks; #9a5700 reaches ~5.21:1 on the light bg.
      expect(tokenValue(light, '--color-warning')).toBe('#9a5700');
    });

    it('maps the reference palette to light-theme semantic roles', () => {
      expect(tokenValue(light, '--color-accent')).toBe('#63e662');
      expect(tokenValue(light, '--color-info')).toBe('#53adf3');
      expect(tokenValue(light, '--color-warning-graphic')).toBe('#f19c2b');
      expect(tokenValue(light, '--color-danger-graphic')).toBe('#ec5c51');
    });

    it('pairs bright reference fills with text-safe light-theme variants', () => {
      expect(tokenValue(light, '--color-accent-strong')).toBe('#3e7810');
      expect(tokenValue(light, '--color-info-strong')).toBe('#1f71ae');
      expect(tokenValue(light, '--color-warning')).toBe('#9a5700');
      expect(tokenValue(light, '--color-danger')).toBe('#b63b33');
    });

    it('keeps the remaining light-only reference colours out of the dark theme', () => {
      // Accent is intentionally shared by both themes; blue/orange/red remain light-only.
      for (const referenceColour of ['#53adf3', '#f19c2b', '#ec5c51']) {
        expect(dark).not.toContain(referenceColour);
      }
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

  describe('semantic colour consumers preserve the light palette roles', () => {
    it('uses the accessible accent for nutrition labels rather than the fill colour', () => {
      expect(nutritionCss).toMatch(/\.mealsCount\s*{[^}]*color:\s*var\(--color-accent-strong\)/s);
      expect(nutritionCss).toMatch(/\.mealType\s*{[^}]*color:\s*var\(--color-accent-strong\)/s);
    });

    it('uses text-safe accent values for generator labels and focus outlines', () => {
      expect(planGeneratorCss).not.toMatch(
        /(?:^|\n)\s*color:\s*var\(--color-accent(?:,\s*#[0-9a-f]+)?\)/i,
      );
      expect(planGeneratorCss).not.toMatch(
        /outline:[^;]*var\(--color-accent(?:,\s*#[0-9a-f]+)?\)/i,
      );
      expect(foodFormCss).toMatch(/outline:[^;]*var\(--color-accent-strong\)/i);
    });

    it('keeps fat series and training dots on the orange graphic role', () => {
      expect(nutritionPageSource).toContain(
        "label: 'Grasas', color: 'var(--color-warning-graphic)'",
      );
      expect(trainingPageSource).toContain('conic-gradient(var(--color-warning-graphic)');
      expect(trainingCss).not.toMatch(/background-color:\s*var\(--color-warning\)/);
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
