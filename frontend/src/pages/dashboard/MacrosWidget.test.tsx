import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MacrosWidget } from './MacrosWidget';
import { getNutritionDay, type NutritionDay } from '../../api/nutrition';

vi.mock('../../api/nutrition', () => ({ getNutritionDay: vi.fn() }));

const nutritionMock = vi.mocked(getNutritionDay);

const day: NutritionDay = {
  type: 'RUNNING',
  targets: { calories: 2300, proteinG: 160, carbsG: 250, fatG: 70 },
  meals: [
    { mealType: 'BREAKFAST', name: 'Desayuno', preferredTime: '08:00', optional: false, items: [] },
  ],
};

describe('MacrosWidget', () => {
  beforeEach(() => {
    nutritionMock.mockReset();
  });

  it('renders the macro target ring and the "Objetivo diario" summary line', async () => {
    nutritionMock.mockResolvedValue(day);

    render(<MacrosWidget />);

    expect(
      await screen.findByRole('img', { name: /Objetivo de macronutrientes/ }),
    ).toBeInTheDocument();
    expect(screen.getByText(/Objetivo diario:/)).toHaveTextContent(
      '160 g proteínas · 250 g carbohidratos · 70 g grasas',
    );
  });

  it('shows an error state when the request fails', async () => {
    nutritionMock.mockRejectedValue(new Error('network'));

    render(<MacrosWidget />);

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No se pudieron cargar tus macronutrientes',
    );
  });
});
