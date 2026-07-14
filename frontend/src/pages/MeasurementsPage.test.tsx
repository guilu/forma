import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MeasurementsPage } from './MeasurementsPage';
import { ApiRequestError } from '../api/client';
import {
  createBodyMeasurement,
  listBodyMeasurements,
  type BodyMeasurement,
} from '../api/bodyMeasurements';

// The page reads via listBodyMeasurements and the manual entry form (reused
// as-is) writes via createBodyMeasurement — both go through the shared API
// module boundary, mocked here so no real network is used (FOR-52 test plan).
vi.mock('../api/bodyMeasurements', () => ({
  listBodyMeasurements: vi.fn(),
  createBodyMeasurement: vi.fn(),
}));

const listMock = vi.mocked(listBodyMeasurements);
const createMock = vi.mocked(createBodyMeasurement);

const SINGLE: BodyMeasurement[] = [
  {
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
    measuredAt: '2026-07-05T08:00:00Z',
    source: 'MANUAL',
    weightKg: 72.0,
    bodyFatPercentage: 14.0,
    bmi: 22.0,
    leanMassKg: 61.9,
  },
  {
    measuredAt: '2026-06-25T08:00:00Z',
    source: 'WITHINGS',
    weightKg: 72.5,
    bodyFatPercentage: 14.3,
    bmi: 22.2,
    leanMassKg: 62.1,
  },
  {
    measuredAt: '2026-06-15T08:00:00Z',
    source: 'MANUAL',
    weightKg: 73.0,
    bodyFatPercentage: 14.5,
    bmi: 22.4,
    leanMassKg: 62.4,
  },
  {
    measuredAt: '2026-06-05T08:00:00Z',
    source: 'MANUAL',
    weightKg: 73.5,
    bodyFatPercentage: 14.8,
    bmi: 22.6,
    leanMassKg: 62.7,
  },
  {
    measuredAt: '2026-05-26T08:00:00Z',
    source: 'MANUAL',
    weightKg: 74.0,
    bodyFatPercentage: 15.0,
    bmi: 22.9,
    leanMassKg: 62.9,
  },
  {
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
  });

  it('shows a loading state while the initial fetch is in flight', () => {
    listMock.mockReturnValue(new Promise(() => {}));
    render(<MeasurementsPage />);

    expect(screen.getByRole('status')).toHaveTextContent('Cargando tus mediciones…');
  });

  it('shows the empty state with a CTA when there are no measurements', async () => {
    listMock.mockResolvedValue([]);
    render(<MeasurementsPage />);

    expect(await screen.findByText('Aún no hay mediciones.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Registrar medición' })).toBeInTheDocument();
  });

  it('shows an error state with a retry action on load failure', async () => {
    listMock.mockRejectedValueOnce(new Error('network down'));
    render(<MeasurementsPage />);

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
    render(<MeasurementsPage />);

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

  // The Resumen/Evolución/Historial tab bar is CSS-hidden at the jsdom desktop
  // viewport (shown only <=768px, same pattern as layout/MobileNav), so these
  // query with `hidden: true` to exercise the mobile tab-switching logic.
  it('switches the active mobile tab panel when a tab is selected', async () => {
    listMock.mockResolvedValue(MULTI);
    const user = userEvent.setup();
    render(<MeasurementsPage />);

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
    render(<MeasurementsPage />);

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
    render(<MeasurementsPage />);

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
    render(<MeasurementsPage />);

    await screen.findByRole('table');
    await userEvent.setup().click(screen.getByRole('button', { name: 'Ver todas las mediciones' }));

    expect(screen.getAllByText('Manual').length).toBeGreaterThan(0);
    expect(screen.getByText('Withings')).toBeInTheDocument();
    expect(screen.getByText('Origen desconocido')).toBeInTheDocument();
  });

  it('opens the manual entry form in a modal and closes it with Cancelar', async () => {
    listMock.mockResolvedValue(SINGLE);
    const user = userEvent.setup();
    render(<MeasurementsPage />);

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
    render(<MeasurementsPage />);

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
    render(<MeasurementsPage />);

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
    render(<MeasurementsPage />);

    await screen.findByRole('heading', { name: 'Peso' });
    await user.click(screen.getByRole('button', { name: '+ Registrar medición' }));
    await user.click(screen.getByRole('button', { name: 'Guardar medición' }));

    expect(screen.getAllByText('Este campo es obligatorio.').length).toBeGreaterThan(0);
    expect(createMock).not.toHaveBeenCalled();
  });
});
