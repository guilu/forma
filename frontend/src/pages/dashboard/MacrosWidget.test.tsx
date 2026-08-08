import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MacrosWidget } from './MacrosWidget';
import type { DayConsumption } from '../../api/nutrition';

const consumption = {
  consumed: { kcal: 1264, proteinG: 111.4, carbsG: 156, fatG: 22.4 },
  target: { kcal: 2320, proteinG: 165, carbsG: 270, fatG: 65 },
} as DayConsumption;

describe('MacrosWidget', () => {
  it('renders real consumed and target values in one progress bar per macro', () => {
    render(<MacrosWidget state={{ status: 'ready', consumption }} />);

    // One bar per macro, each labelled with its own "consumido / objetivo".
    expect(screen.getByRole('progressbar', { name: /Proteínas/ })).toBeInTheDocument();
    expect(screen.getByRole('progressbar', { name: /Carbohidratos/ })).toBeInTheDocument();
    expect(screen.getByRole('progressbar', { name: /Grasas/ })).toBeInTheDocument();
    // Targets are real (160 / 250 / 70 g); the consumed halves are placeholders.
    expect(screen.getByText('111,4 / 165 g')).toBeInTheDocument();
    expect(screen.getByText('156 / 270 g')).toBeInTheDocument();
    expect(screen.getByText('22,4 / 65 g')).toBeInTheDocument();
  });

  it('says there is no meal plan yet when the day has no meals', async () => {
    render(
      <MacrosWidget state={{ status: 'ready', consumption: { ...consumption, target: null } }} />,
    );

    expect(screen.getAllByText(/Sin objetivo/)).toHaveLength(3);
    expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
  });

  it('shows an error state when the request fails', async () => {
    render(<MacrosWidget state={{ status: 'error' }} />);

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No se pudieron cargar tus macronutrientes',
    );
  });
});
