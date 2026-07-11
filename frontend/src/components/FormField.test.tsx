import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { SelectField, TextField } from './FormField';

describe('TextField', () => {
  it('associates the label with the input', () => {
    render(<TextField id="weight" label="Peso (kg)" value="" onChange={() => {}} />);

    expect(screen.getByLabelText('Peso (kg)')).toBeInTheDocument();
  });

  it('associates the inline error with the input via aria-describedby', () => {
    render(
      <TextField
        id="weight"
        label="Peso (kg)"
        value=""
        onChange={() => {}}
        error="Introduce un número válido."
      />,
    );

    const input = screen.getByLabelText('Peso (kg)');
    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByText('Introduce un número válido.')).toHaveAttribute(
      'id',
      input.getAttribute('aria-describedby'),
    );
  });

  it('renders no error state when no error is given', () => {
    render(<TextField id="weight" label="Peso (kg)" value="" onChange={() => {}} />);

    expect(screen.getByLabelText('Peso (kg)')).not.toHaveAttribute('aria-invalid');
  });
});

describe('SelectField', () => {
  it('associates the label with the select and renders its options', () => {
    render(
      <SelectField id="unit" label="Unidad" value="kg" onChange={() => {}}>
        <option value="kg">kg</option>
        <option value="lb">lb</option>
      </SelectField>,
    );

    const select = screen.getByLabelText('Unidad');
    expect(select).toBeInTheDocument();
    expect(screen.getByRole('option', { name: 'lb' })).toBeInTheDocument();
  });

  it('associates the inline error with the select', () => {
    render(
      <SelectField id="unit" label="Unidad" value="" onChange={() => {}} error="Campo requerido.">
        <option value="">--</option>
      </SelectField>,
    );

    const select = screen.getByLabelText('Unidad');
    expect(select).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByText('Campo requerido.')).toHaveAttribute(
      'id',
      select.getAttribute('aria-describedby'),
    );
  });
});
