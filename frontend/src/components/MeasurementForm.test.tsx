import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MeasurementForm } from './MeasurementForm';
import { ApiRequestError } from '../api/client';
import { createBodyMeasurement } from '../api/bodyMeasurements';

// Mock the API module so the form is tested without a live backend (FOR-18 test plan).
vi.mock('../api/bodyMeasurements', () => ({
  createBodyMeasurement: vi.fn(),
}));

const createMock = vi.mocked(createBodyMeasurement);

async function fillValidForm(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText('Fecha y hora'), '2026-07-05T08:00');
  await user.type(screen.getByLabelText('Peso (kg)'), '78.4');
  await user.type(screen.getByLabelText('Grasa corporal (%)'), '18.2');
  await user.type(screen.getByLabelText('IMC'), '23.9');
}

describe('MeasurementForm', () => {
  beforeEach(() => {
    createMock.mockReset();
  });

  it('renders all measurement fields', () => {
    render(<MeasurementForm />);

    expect(screen.getByLabelText('Fecha y hora')).toBeInTheDocument();
    expect(screen.getByLabelText('Peso (kg)')).toBeInTheDocument();
    expect(screen.getByLabelText('Grasa corporal (%)')).toBeInTheDocument();
    expect(screen.getByLabelText('IMC')).toBeInTheDocument();
    expect(screen.getByLabelText('Notas (opcional)')).toBeInTheDocument();
  });

  it('renders a Cancelar action only when onCancel is provided', async () => {
    const onCancel = vi.fn();
    const user = userEvent.setup();
    const { rerender } = render(<MeasurementForm />);
    expect(screen.queryByRole('button', { name: 'Cancelar' })).not.toBeInTheDocument();

    rerender(<MeasurementForm onCancel={onCancel} />);
    await user.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(onCancel).toHaveBeenCalledTimes(1);
  });

  it('blocks submit and shows inline errors when required fields are empty', async () => {
    const user = userEvent.setup();
    render(<MeasurementForm />);

    await user.click(screen.getByRole('button', { name: 'Guardar medición' }));

    expect(screen.getAllByText('Este campo es obligatorio.').length).toBeGreaterThan(0);
    expect(createMock).not.toHaveBeenCalled();
  });

  it('submits a valid form with the FOR-17 payload shape and shows success', async () => {
    createMock.mockResolvedValue({
      measuredAt: '2026-07-05T08:00:00Z',
      source: 'MANUAL',
      weightKg: 78.4,
      bodyFatPercentage: 18.2,
      bmi: 23.9,
    });
    const onCreated = vi.fn();
    const user = userEvent.setup();
    render(<MeasurementForm onCreated={onCreated} />);

    await fillValidForm(user);
    await user.click(screen.getByRole('button', { name: 'Guardar medición' }));

    await waitFor(() => expect(createMock).toHaveBeenCalledTimes(1));
    const payload = createMock.mock.calls[0][0];
    expect(payload).toEqual({
      // datetime-local is local wall-clock; the form converts to an absolute instant.
      measuredAt: new Date('2026-07-05T08:00').toISOString(),
      weightKg: 78.4,
      bodyFatPercentage: 18.2,
      bmi: 23.9,
      notes: undefined,
    });
    expect(await screen.findByRole('status')).toHaveTextContent('Medición guardada correctamente.');
    expect(onCreated).toHaveBeenCalledTimes(1);
  });

  it('shows the backend error message when the API rejects', async () => {
    createMock.mockRejectedValue(new ApiRequestError(400, 'Request validation failed'));
    const user = userEvent.setup();
    render(<MeasurementForm />);

    await fillValidForm(user);
    await user.click(screen.getByRole('button', { name: 'Guardar medición' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Request validation failed');
  });

  it('shows a generic error and no raw detail on a non-API failure', async () => {
    createMock.mockRejectedValue(new Error('TypeError: Failed to fetch'));
    const user = userEvent.setup();
    render(<MeasurementForm />);

    await fillValidForm(user);
    await user.click(screen.getByRole('button', { name: 'Guardar medición' }));

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('No se pudo guardar la medición. Inténtalo de nuevo.');
    expect(alert).not.toHaveTextContent('Failed to fetch');
  });
});
