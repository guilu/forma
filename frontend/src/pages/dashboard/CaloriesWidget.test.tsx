import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { CaloriesWidget } from './CaloriesWidget';
import { getNutritionDay, type NutritionDay } from '../../api/nutrition';

vi.mock('../../api/nutrition', () => ({ getNutritionDay: vi.fn() }));

const nutritionMock = vi.mocked(getNutritionDay);

const day: NutritionDay = {
  type: 'RUNNING',
  targets: { calories: 2300, proteinG: 160, carbsG: 250, fatG: 70 },
  totals: { calories: 1776, proteinG: 62.4, carbsG: 288, fatG: 33.6 },
  targetComparison: {
    caloriesReached: false,
    proteinReached: false,
    carbsReached: false,
    fatReached: false,
  },
  meals: [
    {
      mealType: 'BREAKFAST',
      name: 'Desayuno',
      preferredTime: '08:00',
      optional: false,
      totals: { calories: 444, proteinG: 15.6, carbsG: 72, fatG: 8.4 },
      items: [],
    },
  ],
};

/** No plan for today: the API answers with a day that has no meals. */
const dayWithoutPlan: NutritionDay = { ...day, targets: { ...day.targets }, meals: [] };

describe('CaloriesWidget', () => {
  beforeEach(() => {
    nutritionMock.mockReset();
  });

  it('shows a loading state while the request resolves', () => {
    nutritionMock.mockReturnValue(new Promise(() => {}));

    render(<CaloriesWidget />);

    expect(screen.getByRole('status')).toHaveTextContent('Cargando tus calorías de hoy');
  });

  it('renders the real target with a consumed-vs-target ring (consumed is placeholder)', async () => {
    nutritionMock.mockResolvedValue(day);

    render(<CaloriesWidget />);

    expect(await screen.findByRole('heading', { name: 'Calorías hoy' })).toBeInTheDocument();
    // Real target in the caption (es-ES omits the separator for 4 digits).
    expect(screen.getByText('Objetivo: 2300 kcal')).toBeInTheDocument();
    // Placeholder consumed 2120 / 2300 = 92%, shown in the ring.
    expect(screen.getByText('92%')).toBeInTheDocument();
    expect(
      screen.getByRole('img', { name: /Calorías consumidas: 92% del objetivo/ }),
    ).toBeInTheDocument();
  });

  it('says there is no meal plan yet instead of showing calories against a zero target', async () => {
    nutritionMock.mockResolvedValue(dayWithoutPlan);

    render(<CaloriesWidget />);

    expect(
      await screen.findByText('No hay un plan de comidas para hoy todavía.'),
    ).toBeInTheDocument();
    // No figure and no ring: both would be against a target the user has not set.
    expect(screen.queryByText(/kcal/)).not.toBeInTheDocument();
    expect(screen.queryByRole('img', { name: /Calorías consumidas/ })).not.toBeInTheDocument();
  });

  it('shows an error state when the request fails', async () => {
    nutritionMock.mockRejectedValue(new Error('network'));

    render(<CaloriesWidget />);

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No se pudieron cargar tus calorías',
    );
  });
});
