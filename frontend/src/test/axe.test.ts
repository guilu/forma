import { describe, expect, it } from 'vitest';
import { axe } from './axe';

/**
 * Sanity-checks the axe wiring itself (FOR-114 `tests.md`): proves
 * `toHaveNoViolations()` actually runs axe-core rules against rendered DOM
 * output rather than being a no-op that always passes, before any screen
 * relies on it.
 */
describe('axe test helper', () => {
  it('flags a deliberately-inaccessible fixture (image with no alt text) as a violation', async () => {
    const container = document.createElement('div');
    container.innerHTML = '<img src="chart.png" />';

    const results = await axe(container);

    expect(results.violations.length).toBeGreaterThan(0);
    expect(results.violations.some((violation) => violation.id === 'image-alt')).toBe(true);
    // The matcher itself must fail (throw) for a violating result -- proves
    // `toHaveNoViolations` is wired to real axe-core output, not stubbed out.
    expect(() => expect(results).toHaveNoViolations()).toThrow();
  });

  it('passes on a minimal compliant fixture', async () => {
    const container = document.createElement('div');
    container.innerHTML = '<button type="button">Guardar</button>';

    const results = await axe(container);

    expect(results).toHaveNoViolations();
  });
});
