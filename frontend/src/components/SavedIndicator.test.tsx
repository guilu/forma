import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { SavedIndicator } from './SavedIndicator';

/**
 * SavedIndicator tests (FOR-63): the shared "saved" inline confirmation
 * (spec `specs/FOR-63/ui.md`: "Inline confirmation (field-level save) +
 * saved/unsaved indicator").
 */
describe('SavedIndicator', () => {
  it('renders the default "Guardado" message', () => {
    render(<SavedIndicator />);

    expect(screen.getByText('Guardado.')).toBeInTheDocument();
  });

  it('renders a custom message when provided', () => {
    render(<SavedIndicator message="Producto actualizado." />);

    expect(screen.getByText('Producto actualizado.')).toBeInTheDocument();
    expect(screen.queryByText('Guardado.')).not.toBeInTheDocument();
  });

  it('is announced as a status, not an interruptive alert', () => {
    render(<SavedIndicator />);

    expect(screen.getByText('Guardado.').closest('[role]')).toHaveAttribute('role', 'status');
  });
});
