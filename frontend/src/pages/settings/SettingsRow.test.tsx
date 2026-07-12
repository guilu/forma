import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { SettingsRow } from './SettingsRow';

describe('SettingsRow', () => {
  it('renders a read-only row without the inert marker', () => {
    render(<SettingsRow label="Peso" value="Kilogramos (kg)" />);

    expect(screen.getByText('Peso')).toBeInTheDocument();
    expect(screen.getByText('Kilogramos (kg)')).toBeInTheDocument();
    expect(screen.queryByText('Próximamente')).not.toBeInTheDocument();
  });

  it('renders an inert entry point with a visible "Próximamente" marker', () => {
    render(<SettingsRow label="Eliminar cuenta" description="Acción destructiva" inert />);

    expect(screen.getByText('Eliminar cuenta')).toBeInTheDocument();
    expect(screen.getByText('Próximamente')).toBeInTheDocument();
    // Never rendered as an interactive control.
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
    expect(screen.queryByRole('link')).not.toBeInTheDocument();
  });
});
