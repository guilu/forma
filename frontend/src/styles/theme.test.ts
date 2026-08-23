import { describe, expect, it } from 'vitest';
import foodFormCss from '../pages/admin/FoodForm.module.css?raw';
import planGeneratorCss from '../pages/generator/PlanGenerator.module.css?raw';
import nutritionCss from '../pages/NutritionPage.module.css?raw';
import nutritionPageSource from '../pages/NutritionPage.tsx?raw';
import trainingDetailSource from '../pages/TrainingDetailPage.tsx?raw';
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

/**
 * WCAG relative luminance / contrast ratio for the `#rrggbb` literals this file
 * reads out of `theme.css`.
 *
 * Written out here rather than pulled from a colour library: the whole point of
 * this suite is to check the tokens without a browser, and a dependency added
 * for four lines of arithmetic is a dependency to keep patched forever. The
 * formula is WCAG 2.1 §1.4.3 verbatim.
 */
function relativeLuminance(hex: string): number {
  const value = hex.trim().replace('#', '');
  if (!/^[0-9a-f]{6}$/i.test(value)) {
    throw new Error(`Not a #rrggbb literal: "${hex}"`);
  }
  const channels = [0, 2, 4]
    .map((offset) => Number.parseInt(value.slice(offset, offset + 2), 16) / 255)
    .map((channel) => (channel <= 0.03928 ? channel / 12.92 : ((channel + 0.055) / 1.055) ** 2.4));
  return 0.2126 * channels[0] + 0.7152 * channels[1] + 0.0722 * channels[2];
}

function contrastRatio(foreground: string | null, background: string | null): number {
  if (foreground === null || background === null) {
    throw new Error('Missing token — cannot measure contrast against nothing');
  }
  const [lighter, darker] = [relativeLuminance(foreground), relativeLuminance(background)].sort(
    (a, b) => b - a,
  );
  return (lighter + 0.05) / (darker + 0.05);
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
      // counterpart instead (~4.70:1). Dark needs no split — its accent is
      // already has strong contrast on its own background — but declares the token anyway
      // so component CSS can name the text role unconditionally.
      expect(tokenValue(light, '--color-accent-strong')).toBe('#16801f');
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
      expect(tokenValue(light, '--color-accent-strong')).toBe('#16801f');
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

    /*
     * The rules above pin literal hexes, which is what catches drift back to a
     * pre-reconciliation value. What they cannot catch is a *new* value that
     * nobody measured: `--color-accent-strong` was once hand-edited to a
     * brighter #47a946 (~2.77:1, below even the 3:1 large-text bar) and the
     * only complaint was a hex mismatch, which reads as "the test is stale"
     * rather than "the change is unreadable".
     *
     * So these compute the ratio instead of comparing a string. They keep
     * passing when somebody picks a different green on purpose, and fail only
     * when the token stops doing the job it exists for.
     */
    it('keeps every text-role token above AA on both light surfaces', () => {
      // Text tokens are painted on the page *and* on cards; a value can clear
      // AA on --color-bg and still fail on the white --color-card underneath a
      // soft button or a chip label.
      const surfaces = [
        ['--color-bg', tokenValue(light, '--color-bg')],
        ['--color-card', tokenValue(light, '--color-card')],
      ] as const;
      const textRoles = [
        '--color-accent-strong',
        '--color-info-strong',
        '--color-warning',
        '--color-danger',
      ];

      for (const role of textRoles) {
        for (const [surfaceName, surface] of surfaces) {
          const ratio = contrastRatio(tokenValue(light, role), surface);
          expect(
            ratio,
            `${role} on ${surfaceName}: ${ratio.toFixed(2)}:1 is below the AA 4.5:1 bar`,
          ).toBeGreaterThanOrEqual(4.5);
        }
      }
    });

    it('keeps ink on an accent fill readable', () => {
      // The other direction: --color-accent-contrast is what gets painted *on*
      // the bright fill (skip link, PlanBanner, a selected Chip).
      const ratio = contrastRatio(
        tokenValue(light, '--color-accent-contrast'),
        tokenValue(light, '--color-accent'),
      );
      expect(ratio, `accent ink: ${ratio.toFixed(2)}:1`).toBeGreaterThanOrEqual(4.5);
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

    /*
     * Regression guard. `--font-size-xs` was consumed by thirteen rules across
     * six modules (the training detail set tables and session metrics, the plan
     * generator, the meal form, the calorie ring) but never declared anywhere.
     * `font-size: var(--font-size-xs)` with no fallback is invalid at computed-
     * value time, so every one of those micro-labels silently inherited 1rem
     * and rendered at 16px instead of the ~12px the mockups show. Declaring it
     * is what actually fixes the "fonts look too big" report.
     */
    it('declares --font-size-xs, the micro-label step its consumers already reference', () => {
      expect(tokenValue(dark, '--font-size-xs')).toBe('0.75rem'); // template label-sm: 12px
    });
  });

  /*
   * The mockups' fourth data hue. The donut/legend palette in the training
   * screens cycles accent -> info -> warning-graphic -> secondary, and
   * `--color-secondary` is a lime that reads as a second green next to the
   * accent; the mockups paint that fourth slice violet instead. Same
   * fill/text split every other hue in this file uses: one exact reference
   * value for graphical marks (3:1 bar) and a per-theme darkened/lightened
   * counterpart that clears AA for text (4.5:1).
   */
  describe('violet data hue for the training legends and muscle tags', () => {
    it('shares one reference violet for graphical marks across both themes', () => {
      expect(tokenValue(dark, '--color-violet')).toBe('#8b5cf6');
      expect(tokenValue(light, '--color-violet')).toBe('#8b5cf6');
    });

    it('re-derives the text-safe counterpart per theme', () => {
      // ~7.4:1 on the dark page background.
      expect(tokenValue(dark, '--color-violet-strong')).toBe('#a78bfa');
      // ~7.5:1 on --color-bg (#f4f7f5); the reference violet is only ~4.2:1.
      expect(tokenValue(light, '--color-violet-strong')).toBe('#6d28d9');
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

    /*
     * Ink ON the accent fill, the other direction. The funnel's stepper used to
     * paint `background: var(--color-accent)` with a hardcoded `#fff` label —
     * invisible (~1.61:1) once the accent resolved to the brand green instead of
     * the blue it had been silently falling back to.
     *
     * FOR-190 replaced that stepper with a progress bar, so the accent fill now
     * lives on the chosen option of a segmented selector. The rule is the one
     * being guarded, not the class that happens to carry it: whatever sits on an
     * accent fill takes `--color-accent-contrast` (~8.2:1), and no stylesheet in
     * the funnel hardcodes a colour literal at all.
     */
    it('paints ink on an accent fill with the on-accent token, never a literal', () => {
      expect(planGeneratorCss).toMatch(
        /background-color:\s*var\(--color-accent\);\s*color:\s*var\(--color-accent-contrast\);/s,
      );
      expect(planGeneratorCss).not.toMatch(/:\s*#[0-9a-f]{3,8}\b/i);
    });

    /*
     * The muscle donut and its legend are one chart drawn twice — a
     * conic-gradient for the ring, a swatch per legend row — so the two palettes
     * have to stay the same list in the same order or the legend lies about
     * which slice is which.
     */
    it('draws the muscle donut and its legend from one shared palette', () => {
      expect(trainingDetailSource).toContain("'var(--color-accent)'");
      expect(trainingDetailSource).toContain("'var(--color-violet)'");
      expect(trainingDetailSource).toContain("'var(--color-warning-graphic)'");
      // A single palette constant, consumed by both the ring and the legend.
      expect(trainingDetailSource).toMatch(/MUSCLE_SLICE_COLORS/);
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

/*
 * Regression guard for *undeclared* tokens, the failure mode FOR-163 already
 * hit once with `--font-size-xs` (see the typography block above).
 *
 * `color: var(--color-text-primary)` with no fallback is invalid at computed-
 * value time, so the declaration is dropped and the property silently falls
 * back to its inherited value. With a fallback it is worse than silent: it
 * renders the fallback, and every one of the fallbacks below was a *blue*
 * left over from a pre-brand palette — `.dotNow` and `.choiceOn` in the plan
 * generator were painting #2563eb inside a green product.
 *
 * Pinning three more hexes would not have caught either case, because neither
 * token existed to be pinned. This asserts the relationship instead: every
 * global-namespace token a stylesheet reaches for must actually be declared in
 * `theme.css`.
 */
describe('no stylesheet reaches for a token theme.css does not declare', () => {
  /* Prefixes owned by the token layer. Modules also declare their own local
     custom properties (`--chart-height`, `--muscle-mask`, `--step-size`);
     those are none of this test's business, and none of them use a prefix
     below. A module that *does* declare one locally is honoured, so a
     component may still shadow a global token on purpose. */
  const GLOBAL_PREFIXES = [
    '--color-',
    '--space-',
    '--radius-',
    '--font-',
    '--shadow-',
    '--gradient-',
    '--line-height-',
  ];

  const modules = import.meta.glob('../**/*.module.css', {
    query: '?raw',
    import: 'default',
    eager: true,
  }) as Record<string, string>;

  const declared = new Set(tokenNames(themeCss));

  it('finds the stylesheets to check', () => {
    // Guards the glob itself: a pattern that matches nothing would make every
    // assertion below pass for the wrong reason.
    expect(Object.keys(modules).length).toBeGreaterThan(20);
  });

  it.each(Object.keys(modules).sort())('%s', (path) => {
    const css = modules[path];
    const localTokens = new Set(tokenNames(css));
    const missing = [...css.matchAll(/var\((--[a-z0-9-]+)/gi)]
      .map((match) => match[1])
      .filter((name) => GLOBAL_PREFIXES.some((prefix) => name.startsWith(prefix)))
      .filter((name) => !declared.has(name) && !localTokens.has(name));

    expect([...new Set(missing)].sort()).toEqual([]);
  });
});
