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
  ],
};

describe('NutritionWidget', () => {
  beforeEach(() => {
    nutritionMock.mockReset();
  });

  it('shows a loading state while the request resolves', () => {
    nutritionMock.mockReturnValue(new Promise(() => {}));

    renderWidget();

    expect(screen.getByRole('status')).toHaveTextContent('Cargando tu nutrición de hoy');
  });

  it("renders today's calorie and macro targets", async () => {
    nutritionMock.mockResolvedValue(day);

    renderWidget();

    expect(await screen.findByText('2300')).toBeInTheDocument();
    expect(screen.getByText('kcal objetivo hoy')).toBeInTheDocument();
    expect(screen.getByText('160 g')).toBeInTheDocument();
    expect(screen.getByText('250 g')).toBeInTheDocument();
    expect(screen.getByText('70 g')).toBeInTheDocument();
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

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No se pudo cargar tu nutrición de hoy',
    );
  });

  it('links to the nutrition feature page', async () => {
    nutritionMock.mockResolvedValue(day);

    renderWidget();

    expect(await screen.findByRole('link', { name: 'Ver más' })).toHaveAttribute(
      'href',
      '/nutricion',
    );
  });
});
