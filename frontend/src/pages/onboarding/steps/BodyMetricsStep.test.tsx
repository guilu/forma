import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { BodyMetricsStep } from './BodyMetricsStep';
import type { OnboardingAnswers } from '../onboardingStorage';

const EMPTY: OnboardingAnswers['metrics'] = { choice: undefined, measurementSaved: false };

function renderStep(value: OnboardingAnswers['metrics'], onChange = vi.fn()) {
  return render(
    <MemoryRouter>
      <BodyMetricsStep value={value} onChange={onChange} />
    </MemoryRouter>,
  );
}

describe('BodyMetricsStep', () => {
  it('renders the manual/import choice with neither selected by default', () => {
    renderStep(EMPTY);

    expect(screen.getByRole('button', { name: /Registrar ahora/ })).toHaveAttribute(
      'aria-pressed',
      'false',
    );
    expect(screen.getByRole('button', { name: /Importar más tarde/ })).toHaveAttribute(
      'aria-pressed',
      'false',
    );
    expect(screen.queryByLabelText('Peso (kg)')).not.toBeInTheDocument();
  });

  it('reuses MeasurementForm when the manual entry choice is selected', () => {
    renderStep({ choice: 'MANUAL', measurementSaved: false });

    expect(screen.getByLabelText('Fecha y hora')).toBeInTheDocument();
    expect(screen.getByLabelText('Peso (kg)')).toBeInTheDocument();
  });

  it('shows a saved confirmation instead of the form once a measurement was created', () => {
    renderStep({ choice: 'MANUAL', measurementSaved: true });

    expect(screen.queryByLabelText('Peso (kg)')).not.toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent('Medición guardada correctamente.');
  });

  it('shows a link to the integrations entry point when the import choice is selected', () => {
    renderStep({ choice: 'IMPORT', measurementSaved: false });

    expect(screen.getByRole('link', { name: 'Ir a Integraciones' })).toHaveAttribute(
      'href',
      '/ajustes/integraciones',
    );
  });

  it('reports the selected choice back to the caller', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    renderStep(EMPTY, onChange);

    await user.click(screen.getByRole('button', { name: /Registrar ahora/ }));

    expect(onChange).toHaveBeenCalledWith({ choice: 'MANUAL' });
  });
});
