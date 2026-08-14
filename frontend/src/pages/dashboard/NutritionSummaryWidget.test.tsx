import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import type { DayConsumption } from '../../api/nutrition';
import { NutritionSummaryWidget } from './NutritionSummaryWidget';

const consumption = {
  consumed: { kcal: 1264, proteinG: 111.4, carbsG: 156, fatG: 22.4 },
  target: { kcal: 2320, proteinG: 165, carbsG: 270, fatG: 65 },
} as DayConsumption;

describe('NutritionSummaryWidget', () => {
  it('combines the calorie donut and the three real macro progress bars', () => {
    render(<NutritionSummaryWidget state={{ status: 'ready', consumption }} />);

    expect(screen.getByRole('heading', { name: 'Nutrición' })).toBeInTheDocument();
    expect(screen.getByRole('img', { name: /1264 de 2320 kcal consumidas/ })).toBeInTheDocument();
    expect(screen.getByText('/ 2320 kcal')).toBeInTheDocument();
    expect(
      screen.getByRole('progressbar', { name: /Proteínas: 111.4 de 165 gramos/ }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('progressbar', { name: /Carbohidratos: 156 de 270 gramos/ }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('progressbar', { name: /Grasas: 22.4 de 65 gramos/ }),
    ).toBeInTheDocument();
  });

  it('keeps consumption visible and avoids fake progress when there is no target', () => {
    render(
      <NutritionSummaryWidget
        state={{ status: 'ready', consumption: { ...consumption, target: null } }}
      />,
    );

    expect(screen.getByRole('img', { name: /Tu plan no fija un objetivo/ })).toBeInTheDocument();
    expect(screen.getAllByText(/Sin objetivo/)).toHaveLength(3);
    expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
  });

  it('has one honest loading or error state for the whole card', () => {
    const { rerender } = render(<NutritionSummaryWidget state={{ status: 'loading' }} />);
    expect(screen.getByRole('status')).toHaveTextContent('Cargando tu nutrición de hoy');

    rerender(<NutritionSummaryWidget state={{ status: 'error' }} />);
    expect(screen.getByRole('alert')).toHaveTextContent('No se pudo cargar tu nutrición de hoy');
  });
});
