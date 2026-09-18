import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { DirectionStep } from './DirectionStep';
import type { OnboardingAnswers } from '../onboardingStorage';

const EMPTY: OnboardingAnswers['direction'] = { selected: undefined };

describe('DirectionStep', () => {
  it('renders the three direction options as an unselected radio group', () => {
    render(<DirectionStep value={EMPTY} onChange={vi.fn()} />);

    const group = screen.getByRole('radiogroup', { name: 'Dirección del plan' });
    const options = screen.getAllByRole('radio');
    expect(options).toHaveLength(3);
    expect(group).toContainElement(options[0]);
    options.forEach((option) => expect(option).toHaveAttribute('aria-checked', 'false'));

    expect(screen.getByText('Perder grasa')).toBeInTheDocument();
    expect(screen.getByText('Ganar músculo')).toBeInTheDocument();
    expect(screen.getByText('Mantenerme')).toBeInTheDocument();
  });

  it('marks the selected option as checked', () => {
    render(<DirectionStep value={{ selected: 'GAIN_MUSCLE' }} onChange={vi.fn()} />);

    expect(screen.getByRole('radio', { name: /Ganar músculo/ })).toHaveAttribute(
      'aria-checked',
      'true',
    );
    expect(screen.getByRole('radio', { name: /Perder grasa/ })).toHaveAttribute(
      'aria-checked',
      'false',
    );
  });

  it('calls onChange with the selected direction id', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<DirectionStep value={EMPTY} onChange={onChange} />);

    await user.click(screen.getByRole('radio', { name: /Mantenerme/ }));

    expect(onChange).toHaveBeenCalledWith({ selected: 'MAINTAIN' });
  });
});
