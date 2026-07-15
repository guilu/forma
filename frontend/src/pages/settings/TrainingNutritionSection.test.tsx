import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ObjectivesSection } from './ObjectivesSection';
import { TrainingNutritionSection } from './TrainingNutritionSection';

describe('TrainingNutritionSection', () => {
  it('is a distinct, reachable entry point, separate from ObjectivesSection', () => {
    render(
      <>
        <ObjectivesSection />
        <TrainingNutritionSection />
      </>,
    );

    // Its own heading, not folded into "Objetivos por defecto" (FOR-58's
    // documented folded-in assumption -- FOR-119 resolves it).
    expect(
      screen.getByRole('heading', { name: 'Preferencias de entrenamiento y nutrición', level: 2 }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { name: 'Objetivos por defecto', level: 2 }),
    ).toBeInTheDocument();
    expect(screen.getByText('Preferencias de entrenamiento')).toBeInTheDocument();
    expect(screen.getByText('Preferencias de nutrición')).toBeInTheDocument();
  });

  it('marks itself inert -- FOR-107 has no dedicated training/nutrition-preference endpoint yet', () => {
    render(<TrainingNutritionSection />);

    expect(screen.getAllByText('Próximamente')).toHaveLength(2);
  });
});
