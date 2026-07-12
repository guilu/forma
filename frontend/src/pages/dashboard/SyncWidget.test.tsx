import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { SyncWidget } from './SyncWidget';

/**
 * SyncWidget has no fetch (FOR-51: no backend integrations endpoint exists yet, see the
 * component doc comment), so there is no loading/empty/error state to test — only that
 * the static placeholder renders and links to Settings.
 */
describe('SyncWidget', () => {
  it('renders the static Withings connection status and links to settings', () => {
    render(
      <MemoryRouter>
        <SyncWidget />
      </MemoryRouter>,
    );

    expect(screen.getByRole('heading', { name: 'Integraciones' })).toBeInTheDocument();
    expect(screen.getByText('Withings')).toBeInTheDocument();
    expect(screen.getByText('Conectado')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Ver más' })).toHaveAttribute(
      'href',
      '/ajustes/integraciones',
    );
  });
});
