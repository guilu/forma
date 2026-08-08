import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { CaloriesWidget } from './CaloriesWidget';
import type { DayConsumption } from '../../api/nutrition';

const consumption = {
  consumed: { kcal: 1264, proteinG: 111.4, carbsG: 156, fatG: 22.4 },
  target: { kcal: 2320, proteinG: 165, carbsG: 270, fatG: 65 },
} as DayConsumption;

describe('CaloriesWidget', () => {
  it('shows a loading state', () => {
    render(<CaloriesWidget state={{ status: 'loading' }} />);

    expect(screen.getByRole('status')).toHaveTextContent('Cargando tus calorías de hoy');
  });

  it('reuses the nutrition calorie donut with real consumed and target values', () => {
    render(<CaloriesWidget state={{ status: 'ready', consumption }} />);

    expect(screen.getByRole('heading', { name: 'Calorias' })).toBeInTheDocument();
    // Real target in the caption (es-ES omits the separator for 4 digits).
    expect(screen.getAllByText('1264')).toHaveLength(1);
    expect(screen.queryByText('Consumidas')).not.toBeInTheDocument();
    expect(screen.queryByText('Restantes')).not.toBeInTheDocument();
    expect(screen.getByText('/ 2320 kcal')).toBeInTheDocument();
    expect(screen.getByRole('img', { name: /1264 de 2320 kcal consumidas/ })).toBeInTheDocument();
  });

  it('says there is no meal plan yet instead of showing calories against a zero target', async () => {
    render(
      <CaloriesWidget state={{ status: 'ready', consumption: { ...consumption, target: null } }} />,
    );

    expect(screen.getByRole('img', { name: /Tu plan no fija un objetivo/ })).toBeInTheDocument();
  });

  it('shows an error state when the request fails', async () => {
    render(<CaloriesWidget state={{ status: 'error' }} />);

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No se pudieron cargar tus calorías',
    );
  });
});
