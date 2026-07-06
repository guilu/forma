import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MeasurementsPage } from './MeasurementsPage';

// Keep the form's API call inert; this suite is about the page's open/close wiring.
import { vi } from 'vitest';
vi.mock('../api/bodyMeasurements', () => ({
  createBodyMeasurement: vi.fn(),
}));

/**
 * Measurements page tests (FOR-18): the header renders title/subtitle/action, and
 * the entry form only appears inside the modal after clicking "Registrar
 * medición", and closes again via "Cancelar".
 */
describe('MeasurementsPage', () => {
  it('renders the header without the form initially', () => {
    render(<MeasurementsPage />);

    expect(screen.getByRole('heading', { name: 'Mediciones' })).toBeInTheDocument();
    expect(screen.getByText('Controla tu composición corporal y evolución.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '+ Registrar medición' })).toBeInTheDocument();
    // Form fields are not mounted until the modal opens.
    expect(screen.queryByLabelText('Peso (kg)')).not.toBeInTheDocument();
  });

  it('opens the form in a modal and closes it with Cancelar', async () => {
    const user = userEvent.setup();
    render(<MeasurementsPage />);

    await user.click(screen.getByRole('button', { name: '+ Registrar medición' }));

    expect(screen.getByRole('dialog', { name: 'Registrar medición' })).toBeInTheDocument();
    expect(screen.getByLabelText('Peso (kg)')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Cancelar' }));

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(screen.queryByLabelText('Peso (kg)')).not.toBeInTheDocument();
  });
});
