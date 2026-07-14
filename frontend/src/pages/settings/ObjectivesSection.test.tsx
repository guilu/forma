import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ObjectivesSection } from './ObjectivesSection';

describe('ObjectivesSection', () => {
  it('shows default objectives as inert entry points with current values', () => {
    render(<ObjectivesSection />);

    // Rendered as <h2> (FOR-112): direct sibling of SettingsPage's <h1>.
    expect(
      screen.getByRole('heading', { name: 'Objetivos por defecto', level: 2 }),
    ).toBeInTheDocument();
    expect(screen.getByText('Déficit calórico diario')).toBeInTheDocument();
    expect(screen.getByText('300 kcal')).toBeInTheDocument();
    expect(screen.getByText('Proteínas diarias')).toBeInTheDocument();
    expect(screen.getByText('Agua diaria')).toBeInTheDocument();
    expect(screen.getAllByText('Próximamente')).toHaveLength(3);
  });
});
