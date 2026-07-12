import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { UnitsSection } from './UnitsSection';

describe('UnitsSection', () => {
  it('shows units & locale as read-only display values', () => {
    render(<UnitsSection />);

    expect(screen.getByRole('heading', { name: 'Unidades' })).toBeInTheDocument();
    expect(screen.getByText('Peso')).toBeInTheDocument();
    expect(screen.getByText('Kilogramos (kg)')).toBeInTheDocument();
    expect(screen.getByText('Altura')).toBeInTheDocument();
    expect(screen.getByText('Distancia')).toBeInTheDocument();
    expect(screen.getByText('Energía')).toBeInTheDocument();
    // Read-only, not an unsupported flow — no inert marker.
    expect(screen.queryByText('Próximamente')).not.toBeInTheDocument();
  });
});
