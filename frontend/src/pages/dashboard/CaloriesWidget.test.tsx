import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { CaloriesWidget } from './CaloriesWidget';
import { getNutritionDay, type NutritionDay } from '../../api/nutrition';

vi.mock('../../api/nutrition', () => ({ getNutritionDay: vi.fn() }));

const nutritionMock = vi.mocked(getNutritionDay);

const day: NutritionDay = {
  type: 'RUNNING',
  targets: { calories: 2300, proteinG: 160, carbsG: 250, fatG: 70 },
  meals: [],
};

describe('CaloriesWidget', () => {
  beforeEach(() => {
    nutritionMock.mockReset();
  });

  it('shows a loading state while the request resolves', () => {
    nutritionMock.mockReturnValue(new Promise(() => {}));

    render(<CaloriesWidget />);

    expect(screen.getByRole('status')).toHaveTextContent('Cargando tus calorías de hoy');
  });

  it("renders today's calorie target (honest — no invented consumed figure)", async () => {
    nutritionMock.mockResolvedValue(day);

    render(<CaloriesWidget />);

    expect(await screen.findByRole('heading', { name: 'Calorías hoy' })).toBeInTheDocument();
    expect(screen.getByText('2300')).toBeInTheDocument();
    expect(screen.getByText('objetivo diario')).toBeInTheDocument();
  });

  it('shows an error state when the request fails', async () => {
    nutritionMock.mockRejectedValue(new Error('network'));

    render(<CaloriesWidget />);

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudieron cargar tus calorías');
  });
});
