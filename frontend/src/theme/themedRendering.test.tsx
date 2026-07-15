import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Badge } from '../components/Badge';
import { Button } from '../components/Button';
import { Card } from '../components/Card';
import { ChartContainer } from '../components/ChartContainer';
import { LineChart } from '../components/LineChart';
import { ThemeProvider, useTheme } from './ThemeContext';

// FOR-120: ThemeProvider reads/persists the theme preference through this
// module on mount and on toggle. Mocked so this FOR-62 test stays network-free
// and deterministic; 'SYSTEM' matches the default local mode so the mount-time
// reconciliation is a no-op here.
vi.mock('../api/profile', () => ({
  getProfile: vi.fn().mockResolvedValue({
    unitPreferences: { weightUnit: 'KG', heightUnit: 'CM', distanceUnit: 'KM', energyUnit: 'KCAL' },
    themeMode: 'SYSTEM',
  }),
  updateThemeMode: vi.fn().mockResolvedValue(undefined),
}));

/** A small stand-in for a real screen: a card, a status badge, a chart and a
 * focusable button, all styled purely through `theme.css` tokens. */
function Surfaces() {
  const { setMode } = useTheme();
  return (
    <div>
      <button type="button" onClick={() => setMode('dark')}>
        go-dark
      </button>
      <button type="button" onClick={() => setMode('light')}>
        go-light
      </button>
      <Card title="Resumen">
        <Badge tone="accent">Activo</Badge>
        <Button variant="primary">Guardar</Button>
      </Card>
      <ChartContainer title="Evolución" state="ready">
        <LineChart
          points={[
            { t: 1, y: 1, dateLabel: '1 ene' },
            { t: 2, y: 2, dateLabel: '2 ene' },
          ]}
          formatValue={(value) => `${value}`}
          ariaLabel="Evolución de peso"
        />
      </ChartContainer>
    </div>
  );
}

/**
 * Cross-cutting FOR-62 check: card, badge, chart container and a focusable
 * button all keep rendering correctly under both themes because they consume
 * `--color-*` tokens (styles/theme.css) rather than hardcoding colors — the
 * regression this story explicitly guards against. This does not assert
 * computed pixel colors (jsdom's CSS custom-property support is unreliable
 * for that); the source-level audit for hardcoded colors was done manually
 * for this story (see FOR-62 PR description).
 */
describe('token-driven components render in both themes (FOR-62)', () => {
  afterEach(() => {
    window.localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
  });

  it('renders correctly with data-theme="dark"', async () => {
    const user = userEvent.setup();
    render(
      <ThemeProvider>
        <Surfaces />
      </ThemeProvider>,
    );

    await user.click(screen.getByRole('button', { name: 'go-dark' }));

    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
    expect(screen.getByRole('heading', { name: 'Resumen' })).toBeInTheDocument();
    expect(screen.getByText('Activo')).toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'Evolución de peso' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Guardar' })).toBeInTheDocument();
  });

  it('renders correctly with data-theme="light"', async () => {
    const user = userEvent.setup();
    render(
      <ThemeProvider>
        <Surfaces />
      </ThemeProvider>,
    );

    await user.click(screen.getByRole('button', { name: 'go-light' }));

    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
    expect(screen.getByRole('heading', { name: 'Resumen' })).toBeInTheDocument();
    expect(screen.getByText('Activo')).toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'Evolución de peso' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Guardar' })).toBeInTheDocument();
  });
});
