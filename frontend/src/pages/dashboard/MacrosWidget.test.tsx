import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MacrosWidget } from './MacrosWidget';
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
      id: 'meal-desayuno',
      mealType: 'BREAKFAST',
      name: 'Desayuno',
      preferredTime: '08:00',
      optional: false,
      totals: { calories: 444, proteinG: 15.6, carbsG: 72, fatG: 8.4 },
      items: [],
    },
  ],
};

describe('MacrosWidget', () => {
  beforeEach(() => {
    nutritionMock.mockReset();
  });

  it('renders the macro ring above a progress bar per macro', async () => {
    nutritionMock.mockResolvedValue(day);

    render(<MacrosWidget />);

    expect(
      await screen.findByRole('img', { name: /Objetivo de macronutrientes/ }),
    ).toBeInTheDocument();

    // One bar per macro, each labelled with its own "consumido / objetivo".
    expect(screen.getByRole('progressbar', { name: /Proteínas/ })).toBeInTheDocument();
    expect(screen.getByRole('progressbar', { name: /Carbohidratos/ })).toBeInTheDocument();
    expect(screen.getByRole('progressbar', { name: /Grasas/ })).toBeInTheDocument();
    // Targets are real (160 / 250 / 70 g); the consumed halves are placeholders.
    expect(screen.getByText('162 / 160 g')).toBeInTheDocument();
    expect(screen.getByText('236 / 250 g')).toBeInTheDocument();
    expect(screen.getByText('68 / 70 g')).toBeInTheDocument();
  });

  it('says there is no meal plan yet when the day has no meals', async () => {
    nutritionMock.mockResolvedValue({ ...day, meals: [] });

    render(<MacrosWidget />);

    expect(
      await screen.findByText('No hay un plan de comidas para hoy todavía.'),
    ).toBeInTheDocument();
  });

  it('shows an error state when the request fails', async () => {
    nutritionMock.mockRejectedValue(new Error('network'));

    render(<MacrosWidget />);

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No se pudieron cargar tus macronutrientes',
    );
  });
});
