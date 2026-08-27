import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MeasurementNavigator } from './MeasurementNavigator';
import type { BodyMeasurement } from '../../api/bodyMeasurements';

const measurement = (iso: string): BodyMeasurement => ({
  measuredAt: iso,
  source: 'MANUAL',
  weightKg: 74,
  bodyFatPercentage: 15,
  leanMassKg: 62,
  bmi: 22.5,
});

/** Como llega de la API: de más nueva a más vieja. */
const history = [
  measurement('2026-08-27T08:00:00Z'),
  measurement('2026-07-28T08:00:00Z'),
  measurement('2026-07-01T08:00:00Z'),
  measurement('2025-08-26T08:00:00Z'),
];

function renderNavigator(selected = 0) {
  const onSelect = vi.fn();
  render(<MeasurementNavigator history={history} selected={selected} onSelect={onSelect} />);
  return { onSelect, user: userEvent.setup() };
}

describe('MeasurementNavigator', () => {
  it('shows the selected measurement date', () => {
    renderNavigator(1);

    expect(screen.getByText('28 jul 2026')).toBeInTheDocument();
  });

  describe('the arrows', () => {
    it('step one measurement at a time in each direction', async () => {
      const { onSelect, user } = renderNavigator(1);

      await user.click(screen.getByRole('button', { name: /Medición anterior/ }));
      expect(onSelect).toHaveBeenCalledWith(2);

      await user.click(screen.getByRole('button', { name: /Medición siguiente/ }));
      expect(onSelect).toHaveBeenCalledWith(0);
    });

    it('stops at both ends of the history', () => {
      const { rerender } = render(
        <MeasurementNavigator history={history} selected={0} onSelect={vi.fn()} />,
      );
      expect(screen.getByRole('button', { name: /Medición siguiente/ })).toBeDisabled();

      rerender(
        <MeasurementNavigator history={history} selected={history.length - 1} onSelect={vi.fn()} />,
      );
      expect(screen.getByRole('button', { name: /Medición anterior/ })).toBeDisabled();
    });

    /**
     * Las flechas del teclado hacen lo mismo, pero sólo con el foco dentro de la
     * barra: escucharlas en toda la ventana se lleva por delante las flechas del
     * resto de la página.
     */
    it('answer the arrow keys while the focus is inside the bar', async () => {
      const { onSelect, user } = renderNavigator(1);

      await user.click(screen.getByRole('button', { name: /Medición anterior/ }));
      onSelect.mockClear();

      await user.keyboard('{ArrowLeft}');
      expect(onSelect).toHaveBeenCalledWith(2);

      await user.keyboard('{ArrowRight}');
      expect(onSelect).toHaveBeenCalledWith(0);
    });
  });

  describe('the quick jumps', () => {
    /**
     * Estar en la última medición es un estado en el que se puede estar, así que
     * la pastilla lo reporta. Los otros dos no: son saltos que se ejecutan y se
     * acaban, y por eso son botones y no pastillas.
     */
    it('report "Última" as pressed only while the newest is selected', () => {
      const { rerender } = render(
        <MeasurementNavigator history={history} selected={0} onSelect={vi.fn()} />,
      );
      expect(screen.getByRole('button', { name: 'Última', pressed: true })).toBeInTheDocument();

      rerender(<MeasurementNavigator history={history} selected={2} onSelect={vi.fn()} />);
      expect(screen.getByRole('button', { name: 'Última', pressed: false })).toBeInTheDocument();
    });

    it('never claim a pressed state for the relative jumps', () => {
      renderNavigator(0);

      expect(screen.getByRole('button', { name: '-30 d' })).not.toHaveAttribute('aria-pressed');
      expect(screen.getByRole('button', { name: '-1 año' })).not.toHaveAttribute('aria-pressed');
    });

    it('sends "Última" back to the newest measurement', async () => {
      const { onSelect, user } = renderNavigator(3);

      await user.click(screen.getByRole('button', { name: 'Última' }));

      expect(onSelect).toHaveBeenCalledWith(0);
    });

    it('lands "-30 d" and "-1 año" on the nearest measurement to that date', async () => {
      const { onSelect, user } = renderNavigator(0);

      // Un mes antes del 27 de agosto es el 28 de julio, que existe.
      await user.click(screen.getByRole('button', { name: '-30 d' }));
      expect(onSelect).toHaveBeenCalledWith(1);

      // Un año antes es el 27 de agosto de 2025: la más cercana es la del 26.
      await user.click(screen.getByRole('button', { name: '-1 año' }));
      expect(onSelect).toHaveBeenCalledWith(3);
    });

    /** Desde la más antigua no hay nada más atrás a lo que saltar. */
    it('disables the backwards jumps on the oldest measurement', () => {
      renderNavigator(history.length - 1);

      expect(screen.getByRole('button', { name: '-30 d' })).toBeDisabled();
      expect(screen.getByRole('button', { name: '-1 año' })).toBeDisabled();
    });
  });

  describe('the calendar', () => {
    /** El día local, no el UTC: una medición de las 23:00 no es la de mañana. */
    it('carries the selected day, read in the local time zone', () => {
      const { container } = render(
        <MeasurementNavigator
          history={[measurement('2026-08-27T22:30:00')]}
          selected={0}
          onSelect={vi.fn()}
        />,
      );

      expect(container.querySelector('input[type="date"]')).toHaveValue('2026-08-27');
    });

    it('snaps a chosen date to the nearest measurement', () => {
      const onSelect = vi.fn();
      const { container } = render(
        <MeasurementNavigator history={history} selected={0} onSelect={onSelect} />,
      );

      const picker = container.querySelector('input[type="date"]') as HTMLInputElement;
      fireEvent.change(picker, { target: { value: '2026-07-04' } });

      // El 4 de julio está a tres días de la del 1 y a veinticuatro de la del 28.
      expect(onSelect).toHaveBeenCalledWith(2);
    });

    /**
     * `showPicker` no existe en jsdom, ni en navegadores antiguos. El botón no
     * puede romperse por eso: deja el campo enfocado y alcanzable.
     */
    it('survives a browser without the native picker', async () => {
      const { user } = renderNavigator(0);

      await user.click(screen.getByRole('button', { name: /27 ago 2026/ }));

      expect(screen.getByRole('button', { name: /27 ago 2026/ })).toBeInTheDocument();
    });
  });
});
