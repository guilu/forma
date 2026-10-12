import { beforeEach, describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { AnalyticsConsent } from './AnalyticsConsent';
import { ANALYTICS_CONSENT_KEY, GA_SCRIPT_ID } from './analytics';

const MEASUREMENT_ID = 'G-GGBK0B9LNN';

describe('AnalyticsConsent', () => {
  beforeEach(() => {
    localStorage.clear();
    document.getElementById(GA_SCRIPT_ID)?.remove();
    delete window.dataLayer;
    delete window.gtag;
  });

  it('asks before loading GA4 and accepts an explicit grant', async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter initialEntries={['/plan?private=value']}>
        <AnalyticsConsent measurementId={MEASUREMENT_ID} />
      </MemoryRouter>,
    );

    expect(screen.getByRole('dialog', { name: 'Preferencias de analítica' })).toBeInTheDocument();
    expect(document.getElementById(GA_SCRIPT_ID)).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Aceptar analítica' }));

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(document.getElementById(GA_SCRIPT_ID)).toBeInTheDocument();
    expect(window.dataLayer).toContainEqual(['event', 'page_view', { page_path: '/plan' }]);
    expect(screen.getByRole('button', { name: 'Configurar analítica' })).toBeInTheDocument();
  });

  it('keeps GA4 unloaded after rejection and lets the user change the decision', async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter>
        <AnalyticsConsent measurementId={MEASUREMENT_ID} />
      </MemoryRouter>,
    );

    await user.click(screen.getByRole('button', { name: 'Rechazar analítica' }));

    expect(localStorage.getItem(ANALYTICS_CONSENT_KEY)).toBe('denied');
    expect(document.getElementById(GA_SCRIPT_ID)).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Configurar analítica' }));
    expect(screen.getByRole('dialog', { name: 'Preferencias de analítica' })).toBeInTheDocument();
  });

  it('withdraws a previous grant from the preferences dialog', async () => {
    localStorage.setItem(ANALYTICS_CONSENT_KEY, 'granted');
    const user = userEvent.setup();
    render(
      <MemoryRouter>
        <AnalyticsConsent measurementId={MEASUREMENT_ID} />
      </MemoryRouter>,
    );

    await user.click(screen.getByRole('button', { name: 'Configurar analítica' }));
    await user.click(screen.getByRole('button', { name: 'Rechazar analítica' }));

    expect(localStorage.getItem(ANALYTICS_CONSENT_KEY)).toBe('denied');
    expect(document.getElementById(GA_SCRIPT_ID)).not.toBeInTheDocument();
  });
});
