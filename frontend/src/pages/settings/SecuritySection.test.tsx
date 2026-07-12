import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { SecuritySection } from './SecuritySection';

describe('SecuritySection', () => {
  it('shows change password, 2FA, delete account and export/import as inert', () => {
    render(<SecuritySection />);

    expect(screen.getByText('Cambiar contraseña')).toBeInTheDocument();
    expect(screen.getByText('Autenticación en dos pasos')).toBeInTheDocument();
    expect(screen.getByText('Eliminar cuenta')).toBeInTheDocument();
    expect(screen.getByText('Exportar mis datos')).toBeInTheDocument();
    expect(screen.getByText('Importar datos')).toBeInTheDocument();

    // Every action is inert: no working button/link for any of them.
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
    expect(screen.queryByRole('link')).not.toBeInTheDocument();
    expect(screen.getAllByText('Próximamente')).toHaveLength(5);
  });
});
