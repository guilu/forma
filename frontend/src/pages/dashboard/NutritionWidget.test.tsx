import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { NutritionWidget } from './NutritionWidget';
import { getNutritionDay, type NutritionDay } from '../../api/nutrition';

vi.mock('../../api/nutrition', () => ({ getNutritionDay: vi.fn() }));

const nutritionMock = vi.mocked(getNutritionDay);

function renderWidget() {
  return render(
    <MemoryRouter>
      <NutritionWidget />
    </MemoryRouter>,
  );
}

const day: NutritionDay = {
  type: 'RUNNING',
  targets: { calories: 2300, proteinG: 160, carbsG: 250, fatG: 70 },
  meals: [
    { mealType: 'BREAKFAST', name: 'Desayuno', preferredTime: '08:00', optional: false, items: [] },
    { mealType: 'LUNCH', name: 'Comida', preferredTime: '14:00', optional: false, items: [] },
  ],
};

describe('NutritionWidget (Menú de hoy)', () => {
  beforeEach(() => {
    nutritionMock.mockReset();
  });

  it('shows a loading state while the request resolves', () => {
    nutritionMock.mockReturnValue(new Promise(() => {}));

    renderWidget();

    expect(screen.getByRole('status')).toHaveTextContent('Cargando tu menú de hoy');
  });

  it("renders today's meals (real names + times) and the calorie total against the real target", async () => {
    nutritionMock.mockResolvedValue(day);

    renderWidget();

    expect(await screen.findByText('Desayuno')).toBeInTheDocument();
    expect(screen.getByText('08:00')).toBeInTheDocument();
    expect(screen.getByText('Comida')).toBeInTheDocument();
    expect(screen.getByText('14:00')).toBeInTheDocument();
    // Placeholder per-meal kcal badges (FOR-164 hybrid) render for each meal.
    expect(screen.getByText('560 kcal')).toBeInTheDocument();
    expect(screen.getByText('230 kcal')).toBeInTheDocument();
    // Total line: placeholder consumed / real target. es-ES omits the thousands
    // separator for 4-digit numbers, so "2320 / 2300".
    expect(screen.getByText('2320 / 2300 kcal')).toBeInTheDocument();
  });

  it('shows an empty state when there are no meals planned', async () => {
    nutritionMock.mockResolvedValue({ ...day, meals: [] });

    renderWidget();

    // Loading and empty are both announced via role="status" (FOR-60 shared
    // states), so wait for the terminal content instead of the first match.
    await waitFor(() => {
      expect(screen.getByRole('status')).toHaveTextContent('No hay un plan de comidas para hoy');
    });
  });

  it('shows an error state when the request fails', async () => {
    nutritionMock.mockRejectedValue(new Error('network'));

    renderWidget();

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo cargar tu menú de hoy');
  });

  it('links to the nutrition feature page via "Ver plan"', async () => {
    nutritionMock.mockResolvedValue(day);

    renderWidget();

    expect(await screen.findByRole('link', { name: 'Ver plan' })).toHaveAttribute(
      'href',
      '/nutricion',
    );
  });
});
