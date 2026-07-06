import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { App } from './App';

// The Dashboard (index route) fetches measurements on mount; keep this routing
// smoke test hermetic by stubbing the API call.
vi.mock('./api/bodyMeasurements', () => ({
  listBodyMeasurements: vi.fn().mockResolvedValue([]),
}));

/**
 * Router smoke tests (FOR-81): the shell mounts, the index route renders the
 * Dashboard placeholder, a known route resolves, and unknown routes fall back to
 * the not-found page.
 */
describe('App', () => {
  it('renders the Dashboard placeholder on the index route', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <App />
      </MemoryRouter>,
    );

    expect(screen.getByRole('heading', { name: 'Dashboard' })).toBeInTheDocument();
    // The persistent shell navigation is present.
    expect(screen.getAllByRole('navigation').length).toBeGreaterThan(0);
  });

  it('renders a known section route', () => {
    render(
      <MemoryRouter initialEntries={['/nutricion']}>
        <App />
      </MemoryRouter>,
    );

    expect(screen.getByRole('heading', { name: 'Nutrición' })).toBeInTheDocument();
  });

  it('falls back to the not-found page for unknown routes', () => {
    render(
      <MemoryRouter initialEntries={['/does-not-exist']}>
        <App />
      </MemoryRouter>,
    );

    expect(screen.getByRole('heading', { name: 'Página no encontrada' })).toBeInTheDocument();
  });
});
