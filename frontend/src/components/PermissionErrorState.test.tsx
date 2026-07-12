import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { PermissionErrorState } from './PermissionErrorState';

describe('PermissionErrorState', () => {
  it('renders a default, distinct permission message announced via role="alert"', () => {
    render(<PermissionErrorState />);

    const alert = screen.getByRole('alert');
    expect(alert).toHaveTextContent('Acceso restringido');
    expect(alert).toHaveTextContent('No tienes permiso para ver este contenido.');
  });

  it('renders no retry action, since retrying a permission error does not resolve it', () => {
    render(<PermissionErrorState />);

    expect(screen.queryByRole('button', { name: 'Reintentar' })).not.toBeInTheDocument();
  });

  it('accepts caller-supplied title, message and action', () => {
    render(
      <PermissionErrorState
        title="Sin acceso a este plan"
        message="Este plan de entrenamiento pertenece a otra cuenta."
        action={<a href="/">Volver</a>}
      />,
    );

    expect(screen.getByRole('alert')).toHaveTextContent('Sin acceso a este plan');
    expect(screen.getByRole('alert')).toHaveTextContent(
      'Este plan de entrenamiento pertenece a otra cuenta.',
    );
    expect(screen.getByRole('link', { name: 'Volver' })).toBeInTheDocument();
  });
});
