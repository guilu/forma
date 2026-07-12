import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { IntegrationsPage } from './IntegrationsPage';
import { listIntegrations } from '../api/integrations';

vi.mock('../api/integrations', () => ({
  listIntegrations: vi.fn().mockResolvedValue([]),
}));

/** Thin wrapper smoke test — the real behavior is covered by IntegrationsSection.test.tsx. */
describe('IntegrationsPage', () => {
  it('renders the page heading and mounts the integrations section', async () => {
    render(<IntegrationsPage />);

    expect(screen.getByRole('heading', { name: 'Integraciones' })).toBeInTheDocument();
    expect(
      await screen.findByRole('heading', { name: 'Conexiones e integraciones' }),
    ).toBeInTheDocument();
    expect(vi.mocked(listIntegrations)).toHaveBeenCalled();
  });
});
