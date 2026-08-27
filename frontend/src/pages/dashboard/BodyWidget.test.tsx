import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { BodyWidget, type BodyState } from './BodyWidget';
import { type BodyMeasurement } from '../../api/bodyMeasurements';

/**
 * The widget no longer fetches (FOR-189): `DashboardPage` owns the history so
 * its date navigator and these tiles share one selection. So these tests hand it
 * a state instead of mocking the API — the same states the page can produce.
 */
function renderWidget(state: BodyState) {
  return render(
    <MemoryRouter>
      <BodyWidget state={state} />
    </MemoryRouter>,
  );
}

const latest: BodyMeasurement = {
  measuredAt: '2026-07-05T08:00:00Z',
  source: 'MANUAL',
  weightKg: 73.6,
  bodyFatPercentage: 14.7,
  leanMassKg: 62.8,
  bmi: 22.7,
};

const older: BodyMeasurement = {
  ...latest,
  measuredAt: '2026-06-28T08:00:00Z',
  weightKg: 74.1,
  bodyFatPercentage: 15.2,
  leanMassKg: 62.1,
  bmi: 23.0,
};

describe('BodyWidget', () => {
  it('shows a loading state while the history is in flight', () => {
    renderWidget({ status: 'loading' });

    expect(screen.getByRole('status')).toHaveTextContent('Cargando tu composición corporal');
  });

  it('renders the four metric cards from the selected measurement', () => {
    renderWidget({ status: 'ready', history: [latest], selected: 0 });

    // One-word labels (FOR-189): four tiles side by side read as a set, and the
    // long forms made two of the four wrap while the other two did not.
    expect(screen.getByRole('heading', { name: 'Peso' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Grasa' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Músculo' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'IMC' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Grasa corporal' })).not.toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Masa muscular' })).not.toBeInTheDocument();
    // Coma decimal, como el resto del panel: la tarjeta de tendencia de al lado
    // escribe «73,6 kg» y dos separadores distintos en una pantalla se leen como
    // un fallo.
    expect(screen.getByText('73,6')).toBeInTheDocument();
    expect(screen.getByText('14,7')).toBeInTheDocument();
    expect(screen.getByText('62,8')).toBeInTheDocument();
    expect(screen.getByText('22,7')).toBeInTheDocument();
  });

  /**
   * Cada ficha lleva al lado del valor cuánto ha cambiado desde la medición
   * anterior. La cifra sola («73,6 kg») no dice si eso es subir o bajar, y el
   * pie de la ficha sólo cuenta cuántas mediciones hay.
   */
  it('shows each tile the change from the previous measurement', () => {
    renderWidget({ status: 'ready', history: [latest, older], selected: 0 });

    // Peso y grasa bajan lo mismo (-0,5): dos fichas con el mismo texto.
    expect(screen.getAllByText('(-0,5)')).toHaveLength(2);
    expect(screen.getByText('(+0,7)')).toBeInTheDocument();
    expect(screen.getByText('(-0,3)')).toBeInTheDocument();
  });

  it('spells the change out for whoever is not looking at it', () => {
    renderWidget({ status: 'ready', history: [latest, older], selected: 0 });

    expect(screen.getByText('0,5 kg menos que la medición anterior')).toBeInTheDocument();
    expect(screen.getByText('0,7 kg más que la medición anterior')).toBeInTheDocument();
  });

  /** Sin una medición previa no hay contra qué comparar, y no se inventa una. */
  it('omits the change when the selected measurement is the first one', () => {
    renderWidget({ status: 'ready', history: [latest, older], selected: 1 });

    expect(screen.queryByText(/^\([-+]/)).not.toBeInTheDocument();
  });

  it('omits the change for a metric the previous measurement never recorded', () => {
    const sinGrasa = { ...older, bodyFatPercentage: undefined };
    renderWidget({ status: 'ready', history: [latest, sinGrasa], selected: 0 });

    // El peso sí cambió; la grasa no tiene contra qué compararse.
    expect(screen.getByText('(-0,5)')).toBeInTheDocument();
    expect(screen.queryByText(/puntos/)).not.toBeInTheDocument();
  });

  it('reads the tiles from whichever measurement is selected, not always the newest', () => {
    renderWidget({ status: 'ready', history: [latest, older], selected: 1 });

    expect(screen.getByText('74,1')).toBeInTheDocument();
    expect(screen.queryByText('73,6')).not.toBeInTheDocument();
  });

  it('renders a weight sparkline when there are at least two measurements', () => {
    renderWidget({ status: 'ready', history: [latest, older], selected: 0 });

    expect(screen.getByRole('img', { name: /Evolución de peso/ })).toBeInTheDocument();
  });

  /**
   * The sparkline is the run-up to the number on the tile, so selecting the
   * oldest measurement leaves nothing before it to draw.
   */
  it('omits the sparkline when the selected measurement is the first one', () => {
    renderWidget({ status: 'ready', history: [latest, older], selected: 1 });

    expect(screen.queryByRole('img')).not.toBeInTheDocument();
  });

  it('omits the sparkline with a single measurement', () => {
    renderWidget({ status: 'ready', history: [latest], selected: 0 });

    expect(screen.queryByRole('img')).not.toBeInTheDocument();
  });

  it('shows an empty state with no measurements', () => {
    renderWidget({ status: 'empty' });

    expect(screen.getByRole('status')).toHaveTextContent('Aún no hay mediciones');
    expect(screen.queryByRole('heading', { name: 'Peso' })).not.toBeInTheDocument();
  });

  /**
   * The empty state told the user to register a measurement without offering
   * any way to do it — the form lives on the measurements page, so the widget
   * points there rather than mounting a second copy of it.
   */
  it('offers a way out of the empty state, to the page that has the form', () => {
    renderWidget({ status: 'empty' });

    expect(screen.getByRole('link', { name: '+ Registrar medición' })).toHaveAttribute(
      'href',
      '/app/measurements',
    );
  });

  it('shows an error state when the history could not be read', () => {
    renderWidget({ status: 'error' });

    expect(screen.getByRole('alert')).toHaveTextContent(
      'No se pudo cargar tu composición corporal',
    );
  });

  it('captions each tile with the measurement count (FOR-164: honest count, not an invented delta)', () => {
    renderWidget({ status: 'ready', history: [latest], selected: 0 });

    // One caption per tile (4), all reading "1 medición".
    expect(screen.getAllByText('1 medición')).toHaveLength(4);
  });
});
