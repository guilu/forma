import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { SupportSection } from './SupportSection';

describe('SupportSection', () => {
  it('shows the mockup entries as inert entry points', () => {
    render(<SupportSection />);

    // Rendered as <h2> (FOR-112): direct sibling of SettingsPage's <h1>.
    expect(screen.getByRole('heading', { name: 'Soporte y ayuda', level: 2 })).toBeInTheDocument();
    expect(screen.getByText('Centro de ayuda')).toBeInTheDocument();
    expect(screen.getByText('Contactar con soporte')).toBeInTheDocument();
    expect(screen.getByText('Enviar sugerencia')).toBeInTheDocument();
    // No help-center page, support channel or feedback endpoint exists yet,
    // so every entry stays a visible-but-inert entry point.
    expect(screen.getAllByText('Próximamente')).toHaveLength(3);
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
    expect(screen.queryByRole('link')).not.toBeInTheDocument();
  });
});
