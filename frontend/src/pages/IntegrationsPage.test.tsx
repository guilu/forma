import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { IntegrationsPage } from './IntegrationsPage';
import { NotificationProvider } from '../components/NotificationProvider';
import { IntegrationsProvider } from '../integrations/IntegrationsContext';
import { listIntegrations } from '../api/integrations';

vi.mock('../api/integrations', () => ({
  listIntegrations: vi.fn().mockResolvedValue([]),
}));

/** Thin wrapper smoke test — the real behavior is covered by IntegrationsSection.test.tsx. */
describe('IntegrationsPage', () => {
  it('renders the page heading and mounts the integrations section', async () => {
    // FOR-123: IntegrationsSection now calls `useNotify()`, which requires a provider
    // (App.tsx provides it at the route-tree level; this file mounts the page standalone).
    render(
      <NotificationProvider>
        <IntegrationsProvider>
          <IntegrationsPage />
        </IntegrationsProvider>
      </NotificationProvider>,
    );

    // The page's own <h1> and the section's <h2> now carry the same word
    // (FOR-189 renamed the section), so this pins the level it means.
    expect(screen.getByRole('heading', { name: 'Integraciones', level: 1 })).toBeInTheDocument();
    expect(
      await screen.findByRole('heading', { name: 'Integraciones', level: 2 }),
    ).toBeInTheDocument();
    expect(vi.mocked(listIntegrations)).toHaveBeenCalled();
  });
});
