import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ValidationError } from './ValidationError';

describe('ValidationError', () => {
  it('renders the message with the id a field associates via aria-describedby', () => {
    render(<ValidationError id="weight-error" message="Este campo es obligatorio." />);

    const error = screen.getByText('Este campo es obligatorio.');
    expect(error).toHaveAttribute('id', 'weight-error');
  });
});
