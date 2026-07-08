import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { NutritionPage } from './NutritionPage';
import { getNutritionDay, type NutritionDay } from '../api/nutrition';

vi.mock('../api/nutrition', () => ({
  getNutritionDay: vi.fn(),
}));

const getDayMock = vi.mocked(getNutritionDay);

const runningDay: NutritionDay = {
  type: 'RUNNING',
  targets: { calories: 1940, proteinG: 162, carbsG: 271, fatG: 25 },
  meals: [
    {
      mealType: 'BREAKFAST',
      name: 'Desayuno',
      preferredTime: '08:00',
      optional: false,
      items: [
        { food: 'Avena', quantityG: 120 },
        { food: 'Plátano', quantityG: 120 },
      ],
    },
    {
      mealType: 'PRE_WORKOUT',
      name: 'Snack pre-carrera',
      preferredTime: '18:00',
      optional: false,
      items: [{ food: 'Plátano', quantityG: 120 }],
    },
    {
      mealType: 'POST_WORKOUT',
      name: 'Recuperación (opcional)',
      preferredTime: '20:00',
      optional: true,
      items: [{ food: 'Proteína whey', quantityG: 20 }],
    },
    {
      mealType: 'DINNER',
      name: 'Cena ligera',
      preferredTime: '21:30',
      optional: false,
      items: [{ food: 'Pescado blanco', quantityG: 150 }],
    },
  ],
};

describe('NutritionPage', () => {
  beforeEach(() => {
    getDayMock.mockReset();
  });

  it('renders the running-day meal flow with the explanation', async () => {
    getDayMock.mockResolvedValue(runningDay);

    render(<NutritionPage />);

    expect(await screen.findByRole('heading', { name: 'Nutrición' })).toBeInTheDocument();
    // Ordered flow: pre-run snack and post-run recovery are present.
    expect(screen.getByRole('heading', { name: /Snack pre-carrera/ })).toBeInTheDocument();
    expect(screen.getByText('Avena')).toBeInTheDocument();
    // Explanation about front-loading carbs / lighter dinner.
    expect(screen.getByText(/carbohidratos se concentran temprano/)).toBeInTheDocument();
  });

  it('marks the post-run recovery meal as optional', async () => {
    getDayMock.mockResolvedValue(runningDay);

    render(<NutritionPage />);

    expect(await screen.findByTestId('optional-badge')).toHaveTextContent('Opcional');
  });

  it('shows an error state when the day fails to load', async () => {
    getDayMock.mockRejectedValue(new Error('network'));

    render(<NutritionPage />);

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo cargar');
  });
});
