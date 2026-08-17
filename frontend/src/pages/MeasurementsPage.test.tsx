import { afterEach, describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MeasurementsPage } from './MeasurementsPage';
import { ApiRequestError } from '../api/client';
import {
  createBodyMeasurement,
  deleteBodyMeasurement,
  listBodyMeasurements,
  type BodyMeasurement,
} from '../api/bodyMeasurements';
import { NotificationProvider } from '../components/NotificationProvider';
import { IntegrationsProvider } from '../integrations/IntegrationsContext';
import { listIntegrations, type IntegrationConnection } from '../api/integrations';
import { getProfile } from '../api/profile';

// The page reads via listBodyMeasurements and the manual entry form (reused
// as-is) writes via createBodyMeasurement — both go through the shared API
// module boundary, mocked here so no real network is used (FOR-52 test plan).
vi.mock('../api/bodyMeasurements', () => ({
  listBodyMeasurements: vi.fn(),
  createBodyMeasurement: vi.fn(),
  deleteBodyMeasurement: vi.fn(),
}));

// The header's Withings sync button reads the shared connection store, so the
// page now sits under an IntegrationsProvider exactly as the app shell mounts
// it. Default: nothing connected, i.e. no sync button (see the header suite).
vi.mock('../api/integrations', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../api/integrations')>();
  return { ...actual, listIntegrations: vi.fn(), syncIntegration: vi.fn() };
});

/** Deleting a measurement reports through the shared toast region (FOR-123). */
function renderPage() {
  return render(
    <NotificationProvider>
      <IntegrationsProvider>
        <MeasurementsPage />
      </IntegrationsProvider>
    </NotificationProvider>,
  );
}

// The body-distribution figure follows the profile's sex (male/female
// silhouette), so the page now reads the profile too.
vi.mock('../api/profile', () => ({ getProfile: vi.fn() }));

const listMock = vi.mocked(listBodyMeasurements);
const createMock = vi.mocked(createBodyMeasurement);
const deleteMock = vi.mocked(deleteBodyMeasurement);
const integrationsMock = vi.mocked(listIntegrations);
const getProfileMock = vi.mocked(getProfile);

const WITHINGS_CONNECTED: IntegrationConnection = {
  providerId: 'WITHINGS',
  providerName: 'Withings',
  description: 'Sincroniza automáticamente tus datos.',
  status: 'CONNECTED',
  lastSyncAt: '2026-08-14T20:28:00Z',
};

const SINGLE: BodyMeasurement[] = [
  {
    id: 'm1',
    measuredAt: '2026-07-05T08:00:00Z',
    source: 'MANUAL',
    weightKg: 78.4,
    bodyFatPercentage: 18.2,
    bmi: 23.9,
    fatMassKg: 14.27,
    leanMassKg: 64.13,
  },
];

// 6 points spanning 50 days: enough for the "1M" range to narrow the view
// below the full history (excludes the oldest two points) while "7D" would
// leave fewer than 2 points and "3M"/"6M"/"1A" would all show the same full
// series as "Todo" — exercising the "cap ranges to available data" rule.
const MULTI: BodyMeasurement[] = [
  {
    id: 'm2',
    measuredAt: '2026-07-05T08:00:00Z',
    source: 'MANUAL',
    weightKg: 72.0,
    bodyFatPercentage: 14.0,
    bmi: 22.0,
    leanMassKg: 61.9,
  },
  {
    id: 'm3',
    measuredAt: '2026-06-25T08:00:00Z',
    source: 'WITHINGS',
    weightKg: 72.5,
    bodyFatPercentage: 14.3,
    bmi: 22.2,
    leanMassKg: 62.1,
  },
  {
    id: 'm4',
    measuredAt: '2026-06-15T08:00:00Z',
    source: 'MANUAL',
    weightKg: 73.0,
    bodyFatPercentage: 14.5,
    bmi: 22.4,
    leanMassKg: 62.4,
  },
  {
    id: 'm5',
    measuredAt: '2026-06-05T08:00:00Z',
    source: 'MANUAL',
    weightKg: 73.5,
    bodyFatPercentage: 14.8,
    bmi: 22.6,
    leanMassKg: 62.7,
  },
  {
    id: 'm6',
    measuredAt: '2026-05-26T08:00:00Z',
    source: 'MANUAL',
    weightKg: 74.0,
    bodyFatPercentage: 15.0,
    bmi: 22.9,
    leanMassKg: 62.9,
  },
  {
    id: 'm7',
    measuredAt: '2026-05-16T08:00:00Z',
    source: '',
    weightKg: 74.3,
    bodyFatPercentage: 15.2,
    bmi: 23.0,
    leanMassKg: 63.0,
  },
];

describe('MeasurementsPage', () => {
  beforeEach(() => {
    listMock.mockReset();
    createMock.mockReset();
    deleteMock.mockReset();
    integrationsMock.mockReset();
    integrationsMock.mockResolvedValue([]);
    getProfileMock.mockReset();
    getProfileMock.mockResolvedValue({ sex: 'MALE' } as never);
  });

  it('shows a loading state while the initial fetch is in flight', () => {
    listMock.mockReturnValue(new Promise(() => {}));
    renderPage();

    expect(screen.getByRole('status')).toHaveTextContent('Cargando tus mediciones…');
  });

  /**
   * With nothing to list, the header action and the empty state's own CTA are
   * the same offer twice over. The empty state keeps it — it sits with the
   * sentence explaining why the page is blank.
   */
  it('shows the empty state with a single CTA when there are no measurements', async () => {
    listMock.mockResolvedValue([]);
    renderPage();

    expect(await screen.findByText('Aún no hay mediciones.')).toBeInTheDocument();
    // Deliberately matched loosely: the header action and the empty state's own
    // CTA differ only by the "+", so an exact name would pass while both are on
    // screen — which is the duplication this asserts against.
    expect(screen.getAllByRole('button', { name: /Registrar medición/ })).toHaveLength(1);
    expect(screen.getByRole('button', { name: '+ Registrar medición' })).toBeInTheDocument();
  });

  it('opens the form from the empty state', async () => {
    listMock.mockResolvedValue([]);
    const user = userEvent.setup();
    renderPage();

    // Wait for the empty state before reaching for the CTA: while the page is
    // still loading, the *header* action carries the same name, and clicking
    // that one lands on a node the state change is about to unmount.
    await screen.findByText('Aún no hay mediciones.');
    await user.click(screen.getByRole('button', { name: '+ Registrar medición' }));

    expect(await screen.findByRole('dialog')).toBeInTheDocument();
  });

  it('shows an error state with a retry action on load failure', async () => {
    listMock.mockRejectedValueOnce(new Error('network down'));
    renderPage();

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No se pudieron cargar tus mediciones.',
    );
    const retry = screen.getByRole('button', { name: 'Reintentar' });

    listMock.mockResolvedValueOnce(SINGLE);
    await userEvent.setup().click(retry);

    expect(await screen.findByRole('heading', { name: 'Peso' })).toBeInTheDocument();
    // Entry is still reachable even while an error was showing.
    expect(screen.getByRole('button', { name: '+ Registrar medición' })).toBeInTheDocument();
  });

  it('renders latest metric cards without a delta for a single measurement', async () => {
    listMock.mockResolvedValue(SINGLE);
    renderPage();

    // Metric cards are direct siblings of the page's <h1> (no intervening <h2>
    // section heading), so per FOR-112 they must render as <h2> to avoid
    // skipping a level.
    expect(await screen.findByRole('heading', { name: 'Peso', level: 2 })).toBeInTheDocument();
    expect(screen.getByText('78.4')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Grasa corporal', level: 2 })).toBeInTheDocument();
    expect(screen.getByText('18.2')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Masa muscular', level: 2 })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'IMC', level: 2 })).toBeInTheDocument();
    // "vs semana pasada" is not backed by the API (documented gap) — never rendered.
    expect(screen.queryByText(/semana pasada/i)).not.toBeInTheDocument();
  });

  it('renders the placeholder water tile and the body-distribution card (real muscle/fat, placeholder bone/water)', async () => {
    listMock.mockResolvedValue(SINGLE);
    renderPage();

    // Placeholder "Agua corporal" tile — no "vs semana pasada" delta.
    expect(
      await screen.findByRole('heading', { name: 'Agua corporal', level: 2 }),
    ).toBeInTheDocument();
    expect(screen.getByText('58.0')).toBeInTheDocument();

    // Distribución corporal: real muscle (leanMassKg) + fat (fatMassKg), and
    // placeholder bone/water values.
    const distribution = screen
      .getByRole('heading', { name: 'Distribución corporal', level: 2 })
      .closest('section') as HTMLElement;
    expect(within(distribution).getByText('64.1 kg')).toBeInTheDocument(); // leanMassKg
    expect(within(distribution).getByText('14.3 kg')).toBeInTheDocument(); // fatMassKg
    expect(within(distribution).getByText('3.2 kg')).toBeInTheDocument(); // placeholder bone
    expect(
      within(distribution).getByRole('link', { name: 'Ver análisis detallado' }),
    ).toHaveAttribute('href', '/app/progress');
    // The figure is the anatomy pack's front silhouette, same asset the
    // training page draws, and it follows the profile's sex — male here.
    expect(within(distribution).getByRole('img', { name: 'Composición corporal' })).toHaveAttribute(
      'data-silhouette',
      'male/front',
    );
  });

  it('draws the female front silhouette when the profile says FEMALE', async () => {
    listMock.mockResolvedValue(SINGLE);
    getProfileMock.mockResolvedValue({ sex: 'FEMALE' } as never);
    renderPage();

    const figure = await screen.findByRole('img', { name: 'Composición corporal' });
    await waitFor(() => expect(figure).toHaveAttribute('data-silhouette', 'female/front'));
  });

  // The Resumen/Evolución/Historial tab bar is CSS-hidden at the jsdom desktop
  // viewport (shown only <=768px, same pattern as layout/MobileNav), so these
  // query with `hidden: true` to exercise the mobile tab-switching logic.
  it('switches the active mobile tab panel when a tab is selected', async () => {
    listMock.mockResolvedValue(MULTI);
    const user = userEvent.setup();
    renderPage();

    await screen.findByRole('heading', { name: 'Peso' });

    const resumenTab = screen.getByRole('tab', { name: 'Resumen', hidden: true });
    const historialTab = screen.getByRole('tab', { name: 'Historial', hidden: true });
    expect(resumenTab).toHaveAttribute('aria-selected', 'true');
    expect(historialTab).toHaveAttribute('aria-selected', 'false');
    expect(screen.getByRole('tabpanel', { name: 'Resumen' })).toHaveAttribute(
      'data-active',
      'true',
    );

    await user.click(historialTab);

    expect(resumenTab).toHaveAttribute('aria-selected', 'false');
    expect(historialTab).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('tabpanel', { name: 'Historial' })).toHaveAttribute(
      'data-active',
      'true',
    );
    expect(screen.getByRole('tabpanel', { name: 'Resumen' })).toHaveAttribute(
      'data-active',
      'false',
    );
  });

  it('renders the weight evolution chart with a range selector capped to available data', async () => {
    listMock.mockResolvedValue(MULTI);
    renderPage();

    // Same reasoning as the metric cards above: no <h2> section heading sits
    // between the page <h1> and this chart's title (FOR-112).
    expect(
      await screen.findByRole('heading', { name: 'Evolución de peso', level: 2 }),
    ).toBeInTheDocument();
    const rangeGroup = screen.getByRole('group', { name: 'Rango del gráfico' });
    expect(within(rangeGroup).getByRole('button', { name: '1M' })).toBeInTheDocument();
    expect(within(rangeGroup).getByRole('button', { name: 'Todo' })).toBeInTheDocument();
    // 7D would leave fewer than 2 points for this fixture — not a meaningful range.
    expect(within(rangeGroup).queryByRole('button', { name: '7D' })).not.toBeInTheDocument();
  });

  it('lists recent measurements in the history table with the expected columns', async () => {
    listMock.mockResolvedValue(MULTI);
    renderPage();

    // No <h2> section heading between the page <h1> and this card (FOR-112).
    expect(
      await screen.findByRole('heading', { name: 'Últimas mediciones', level: 2 }),
    ).toBeInTheDocument();
    const table = screen.getByRole('table');
    for (const header of ['Fecha', 'Peso', 'Grasa corporal', 'Masa muscular', 'IMC', 'Fuente']) {
      expect(within(table).getByRole('columnheader', { name: header })).toBeInTheDocument();
    }
    // Only the 5 most recent rows show initially (6 fixtures, preview cap is 5).
    expect(within(table).getAllByRole('row')).toHaveLength(1 + 5);

    await userEvent.setup().click(screen.getByRole('button', { name: 'Ver todas las mediciones' }));
    expect(within(table).getAllByRole('row')).toHaveLength(1 + 6);
  });

  it('distinguishes manual and Withings measurements, and gives an unknown source a neutral label', async () => {
    listMock.mockResolvedValue(MULTI);
    renderPage();

    await screen.findByRole('table');
    await userEvent.setup().click(screen.getByRole('button', { name: 'Ver todas las mediciones' }));

    expect(screen.getAllByText('Manual').length).toBeGreaterThan(0);
    expect(screen.getByText('Withings')).toBeInTheDocument();
    expect(screen.getByText('Origen desconocido')).toBeInTheDocument();
  });

  it('opens the manual entry form in a modal and closes it with Cancelar', async () => {
    listMock.mockResolvedValue(SINGLE);
    const user = userEvent.setup();
    renderPage();

    await screen.findByRole('heading', { name: 'Peso' });
    await user.click(screen.getByRole('button', { name: '+ Registrar medición' }));

    expect(screen.getByRole('dialog', { name: 'Registrar medición' })).toBeInTheDocument();
    expect(screen.getByLabelText('Peso (kg)')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('refreshes the list after a successful manual entry and closes the modal', async () => {
    listMock.mockResolvedValueOnce(SINGLE);
    createMock.mockResolvedValue({
      measuredAt: '2026-07-08T08:00:00Z',
      source: 'MANUAL',
      weightKg: 77.9,
      bodyFatPercentage: 17.9,
      bmi: 23.7,
    });
    const user = userEvent.setup();
    renderPage();

    await screen.findByRole('heading', { name: 'Peso' });
    await user.click(screen.getByRole('button', { name: '+ Registrar medición' }));

    listMock.mockResolvedValueOnce([
      {
        measuredAt: '2026-07-08T08:00:00Z',
        source: 'MANUAL',
        weightKg: 77.9,
        bodyFatPercentage: 17.9,
        bmi: 23.7,
      },
      ...SINGLE,
    ]);

    await user.type(screen.getByLabelText('Fecha y hora'), '2026-07-08T08:00');
    await user.type(screen.getByLabelText('Peso (kg)'), '77.9');
    await user.type(screen.getByLabelText('Grasa corporal (%)'), '17.9');
    await user.type(screen.getByLabelText('IMC'), '23.7');
    await user.click(screen.getByRole('button', { name: 'Guardar medición' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    await waitFor(() => expect(listMock).toHaveBeenCalledTimes(2));
  });

  it('keeps the existing list and shows an inline error when manual entry submission fails', async () => {
    listMock.mockResolvedValue(SINGLE);
    createMock.mockRejectedValue(new ApiRequestError(400, 'Request validation failed'));
    const user = userEvent.setup();
    renderPage();

    await screen.findByRole('heading', { name: 'Peso' });
    await user.click(screen.getByRole('button', { name: '+ Registrar medición' }));

    await user.type(screen.getByLabelText('Fecha y hora'), '2026-07-08T08:00');
    await user.type(screen.getByLabelText('Peso (kg)'), '77.9');
    await user.type(screen.getByLabelText('Grasa corporal (%)'), '17.9');
    await user.type(screen.getByLabelText('IMC'), '23.7');
    await user.click(screen.getByRole('button', { name: 'Guardar medición' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Request validation failed');
    // The modal/form stays open and the list was fetched only once (no refresh on failure).
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(listMock).toHaveBeenCalledTimes(1);
  });

  it('shows field-level validation errors close to fields without calling the API', async () => {
    listMock.mockResolvedValue(SINGLE);
    const user = userEvent.setup();
    renderPage();

    await screen.findByRole('heading', { name: 'Peso' });
    await user.click(screen.getByRole('button', { name: '+ Registrar medición' }));
    await user.click(screen.getByRole('button', { name: 'Guardar medición' }));

    expect(screen.getAllByText('Este campo es obligatorio.').length).toBeGreaterThan(0);
    expect(createMock).not.toHaveBeenCalled();
  });

  /**
   * On a phone the header put the button on its own row under the subtitle,
   * which pushed the section tabs and everything below them further down. Title
   * and button now share a row; the button loses the verb to fit.
   */
  describe('the header on a narrow screen', () => {
    const matchNarrow = (matches: boolean) => {
      window.matchMedia = ((query: string) => ({
        matches,
        media: query,
        onchange: null,
        addEventListener: () => {},
        removeEventListener: () => {},
        addListener: () => {},
        removeListener: () => {},
        dispatchEvent: () => false,
      })) as typeof window.matchMedia;
    };

    afterEach(() => {
      // The suite-wide stub (src/test/setup.ts) answers "no match", i.e. wide.
      matchNarrow(false);
    });

    it('shortens the register action', async () => {
      matchNarrow(true);
      listMock.mockResolvedValue(SINGLE);
      renderPage();

      expect(await screen.findByRole('button', { name: '+ Medición' })).toBeInTheDocument();
      expect(
        screen.queryByRole('button', { name: '+ Registrar medición' }),
      ).not.toBeInTheDocument();
    });

    it('keeps the full label with room for it', async () => {
      matchNarrow(false);
      listMock.mockResolvedValue(SINGLE);
      renderPage();

      expect(
        await screen.findByRole('button', { name: '+ Registrar medición' }),
      ).toBeInTheDocument();
    });
  });

  /**
   * Withings is where most of these measurements come from, so the manual sync
   * belongs on this screen and not only in Ajustes — pulling today's weigh-in
   * should not cost a trip through the settings menu.
   */
  describe('the Withings sync action in the header', () => {
    it('sits before the register action while Withings is connected', async () => {
      listMock.mockResolvedValue(SINGLE);
      integrationsMock.mockResolvedValue([WITHINGS_CONNECTED]);

      renderPage();

      const sync = await screen.findByRole('button', { name: 'Sincronizar Withings' });
      const register = screen.getByRole('button', { name: '+ Registrar medición' });
      expect(sync).toHaveAttribute('title', 'Sincronizar Withings');
      // Same row, sync first: it is the quieter of the two and must not take
      // the place the page's main action occupies on every other screen.
      expect(sync.compareDocumentPosition(register)).toBe(Node.DOCUMENT_POSITION_FOLLOWING);
    });

    it('is absent when Withings is not connected', async () => {
      listMock.mockResolvedValue(SINGLE);
      integrationsMock.mockResolvedValue([{ ...WITHINGS_CONNECTED, status: 'NOT_CONNECTED' }]);

      renderPage();

      await screen.findByRole('button', { name: '+ Registrar medición' });
      expect(
        screen.queryByRole('button', { name: 'Sincronizar Withings' }),
      ).not.toBeInTheDocument();
    });

    /* The empty state hides the register action and offers its own; the sync
       button has no such duplicate, and a page with no measurements is exactly
       when pulling them from Withings is the useful thing to do. */
    it('stays available on the empty page', async () => {
      listMock.mockResolvedValue([]);
      integrationsMock.mockResolvedValue([WITHINGS_CONNECTED]);

      renderPage();

      expect(
        await screen.findByRole('button', { name: 'Sincronizar Withings' }),
      ).toBeInTheDocument();
    });
  });

  describe('deleting a measurement', () => {
    /** Names the delete action for the row whose date cell reads `date`. */
    function deleteButtonFor(date: string) {
      return within(screen.getByText(date).closest('tr') as HTMLElement).getByRole('button', {
        name: /Eliminar/,
      });
    }

    it('asks for confirmation before deleting, then removes the row and reports it', async () => {
      listMock.mockResolvedValueOnce(SINGLE).mockResolvedValueOnce([]);
      deleteMock.mockResolvedValue(undefined);
      const user = userEvent.setup();
      renderPage();

      await screen.findByRole('table');
      await user.click(deleteButtonFor('5 jul'));

      // Destructive confirmation first (FOR-63 pattern), no call yet.
      const dialog = await screen.findByRole('dialog');
      expect(deleteMock).not.toHaveBeenCalled();
      await user.click(within(dialog).getByRole('button', { name: 'Eliminar' }));

      await waitFor(() => expect(deleteMock).toHaveBeenCalledWith('m1'));
      // The list is re-read rather than patched locally: every card and chart on
      // the page derives from it.
      await waitFor(() => expect(listMock).toHaveBeenCalledTimes(2));
      expect(await screen.findByRole('log')).toHaveTextContent('Medición eliminada.');
    });

    it('does not call the API when the user backs out', async () => {
      listMock.mockResolvedValue(SINGLE);
      const user = userEvent.setup();
      renderPage();

      await screen.findByRole('table');
      await user.click(deleteButtonFor('5 jul'));
      const dialog = await screen.findByRole('dialog');
      await user.click(within(dialog).getByRole('button', { name: 'Cancelar' }));

      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      expect(deleteMock).not.toHaveBeenCalled();
    });

    it('surfaces the error and keeps the row when the delete fails', async () => {
      listMock.mockResolvedValue(SINGLE);
      deleteMock.mockRejectedValue(new ApiRequestError(404, 'No existe una medición con ese id.'));
      const user = userEvent.setup();
      renderPage();

      await screen.findByRole('table');
      await user.click(deleteButtonFor('5 jul'));
      const dialog = await screen.findByRole('dialog');
      await user.click(within(dialog).getByRole('button', { name: 'Eliminar' }));

      expect(await screen.findByRole('alert')).toHaveTextContent('No existe una medición');
      // Still listed, and no success toast for a call that did not succeed.
      expect(screen.getByText('5 jul')).toBeInTheDocument();
      expect(screen.queryByText('Medición eliminada.')).not.toBeInTheDocument();
    });
  });
});
