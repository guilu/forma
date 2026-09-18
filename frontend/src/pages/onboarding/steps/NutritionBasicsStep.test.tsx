import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { NutritionBasicsStep } from './NutritionBasicsStep';
import type { OnboardingAnswers } from '../onboardingStorage';

const VALUE: OnboardingAnswers['nutrition'] = {
  preference: '',
  mealsPerDay: 5,
  cuisineStyle: '',
};

describe('NutritionBasicsStep', () => {
  it('renders the preference, cuisine and meals-per-day selects, with no restrictions textarea (ADR-015 decision 12)', () => {
    render(<NutritionBasicsStep value={VALUE} onChange={vi.fn()} />);

    expect(screen.getByLabelText('Preferencia alimentaria')).toBeInTheDocument();
    expect(screen.getByLabelText('Estilo de cocina')).toBeInTheDocument();
    expect(screen.getByLabelText('Comidas al día')).toBeInTheDocument();
    expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
    expect(screen.queryByText(/Alimentos que prefieres evitar/i)).not.toBeInTheDocument();
  });

  it('defaults meals-per-day to the value passed in (5)', () => {
    render(<NutritionBasicsStep value={VALUE} onChange={vi.fn()} />);

    expect(screen.getByLabelText('Comidas al día')).toHaveValue('5');
  });

  it('calls onChange with the selected diet preference', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<NutritionBasicsStep value={VALUE} onChange={onChange} />);

    await user.selectOptions(screen.getByLabelText('Preferencia alimentaria'), 'VEGAN');

    expect(onChange).toHaveBeenCalledWith({ preference: 'VEGAN' });
  });

  it('calls onChange with the selected cuisine style', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<NutritionBasicsStep value={VALUE} onChange={onChange} />);

    await user.selectOptions(screen.getByLabelText('Estilo de cocina'), 'MEDITERRANEA');

    expect(onChange).toHaveBeenCalledWith({ cuisineStyle: 'MEDITERRANEA' });
  });

  it('calls onChange with the selected meals-per-day count, as a number', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<NutritionBasicsStep value={VALUE} onChange={onChange} />);

    await user.selectOptions(screen.getByLabelText('Comidas al día'), '3');

    expect(onChange).toHaveBeenCalledWith({ mealsPerDay: 3 });
  });
});
