import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { NutritionWidget } from './NutritionWidget';
import type { DayConsumption, NutritionDay } from '../../api/nutrition';

const day: NutritionDay = {
  type: 'RUNNING',
  targets: { calories: 2300, proteinG: 160, carbsG: 250, fatG: 70 },
  totals: { calories: 444, proteinG: 15.6, carbsG: 72, fatG: 8.4 },
  targetComparison: {
    caloriesReached: false,
    proteinReached: false,
    carbsReached: false,
    fatReached: false,
  },
  meals: [
    {
      id: 'm1',
      mealType: 'BREAKFAST',
      name: 'Desayuno',
      preferredTime: '08:00',
      optional: false,
      totals: { calories: 444, proteinG: 15.6, carbsG: 72, fatG: 8.4 },
      items: [
        { food: 'Copos de avena', quantityG: 80 },
        { food: 'Plátano', quantityG: 100 },
      ],
    },
  ],
};
const consumption = {
  consumed: { kcal: 444, proteinG: 15.6, carbsG: 72, fatG: 8.4 },
  target: { kcal: 2300, proteinG: 160, carbsG: 250, fatG: 70 },
} as DayConsumption;
const renderWidget = (menu: Parameters<typeof NutritionWidget>[0]['menu'], current = consumption) =>
  render(
    <MemoryRouter>
      <NutritionWidget menu={menu} consumption={current} />
    </MemoryRouter>,
  );

describe('NutritionWidget', () => {
  it('renders the real meal type, food description, meal kcal and daily consumption', () => {
    renderWidget({ status: 'ready', day });
    expect(screen.getByText('Desayuno')).toBeInTheDocument();
    expect(screen.getByText('Copos de avena, Plátano')).toBeInTheDocument();
    expect(screen.getByText('444 kcal')).toBeInTheDocument();
    expect(screen.getByText(/444 kcal \/ 2300 kcal/)).toBeInTheDocument();
  });
  it('renders honest terminal states', () => {
    const { rerender } = renderWidget({ status: 'empty' });
    expect(screen.getByText(/No hay un plan/)).toBeInTheDocument();
    rerender(
      <MemoryRouter>
        <NutritionWidget menu={{ status: 'error' }} consumption={consumption} />
      </MemoryRouter>,
    );
    expect(screen.getByRole('alert')).toHaveTextContent('No se pudo cargar');
  });

  it('shows consumed kcal without a fake zero-percent bar when there is no target', () => {
    renderWidget({ status: 'ready', day }, { ...consumption, target: null });
    expect(screen.getByText('444 kcal')).toBeInTheDocument();
    expect(screen.getByText(/Sin objetivo/)).toBeInTheDocument();
    expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
  });
});
