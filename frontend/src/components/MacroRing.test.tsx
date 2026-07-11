import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MacroRing } from './MacroRing';

describe('MacroRing', () => {
  it('renders an accessible summary and the grams for each macro', () => {
    render(<MacroRing proteinG={162} carbsG={236} fatG={68} />);

    expect(
      screen.getByRole('img', {
        name: 'Objetivo de macronutrientes: proteínas 162 gramos, carbohidratos 236 gramos, grasas 68 gramos',
      }),
    ).toBeInTheDocument();
    expect(screen.getByText('Proteínas')).toBeInTheDocument();
    expect(screen.getByText('162 g')).toBeInTheDocument();
    expect(screen.getByText('236 g')).toBeInTheDocument();
    expect(screen.getByText('68 g')).toBeInTheDocument();
  });

  it('handles all-zero targets without dividing by zero', () => {
    render(<MacroRing proteinG={0} carbsG={0} fatG={0} />);

    expect(
      screen.getByRole('img', {
        name: 'Objetivo de macronutrientes: proteínas 0 gramos, carbohidratos 0 gramos, grasas 0 gramos',
      }),
    ).toBeInTheDocument();
  });
});
