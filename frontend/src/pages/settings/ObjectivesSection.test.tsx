import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ObjectivesSection } from './ObjectivesSection';

describe('ObjectivesSection', () => {
  it('shows default objectives as inert entry points with current values', () => {
    render(<ObjectivesSection />);

    expect(screen.getByRole('heading', { name: 'Objetivos por defecto' })).toBeInTheDocument();
    expect(screen.getByText('Déficit calórico diario')).toBeInTheDocument();
    expect(screen.getByText('300 kcal')).toBeInTheDocument();
    expect(screen.getByText('Proteínas diarias')).toBeInTheDocument();
    expect(screen.getByText('Agua diaria')).toBeInTheDocument();
    expect(screen.getAllByText('Próximamente')).toHaveLength(3);
  });
});
