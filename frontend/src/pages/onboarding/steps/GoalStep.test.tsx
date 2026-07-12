import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { GoalStep } from './GoalStep';
import type { OnboardingAnswers } from '../onboardingStorage';

const EMPTY: OnboardingAnswers['goal'] = { selected: undefined };

describe('GoalStep', () => {
  it('renders the three goal options as an unselected radio group', () => {
    render(<GoalStep value={EMPTY} onChange={vi.fn()} />);

    const group = screen.getByRole('radiogroup', { name: 'Objetivo principal' });
    const options = screen.getAllByRole('radio');
    expect(options).toHaveLength(3);
    expect(group).toContainElement(options[0]);
    options.forEach((option) => expect(option).toHaveAttribute('aria-checked', 'false'));

    expect(screen.getByText('Composición corporal')).toBeInTheDocument();
    expect(screen.getByText('Rendimiento')).toBeInTheDocument();
    expect(screen.getByText('Hábito')).toBeInTheDocument();
  });

  it('marks the selected option as checked', () => {
    render(<GoalStep value={{ selected: 'RENDIMIENTO' }} onChange={vi.fn()} />);

    expect(screen.getByRole('radio', { name: /Rendimiento/ })).toHaveAttribute(
      'aria-checked',
      'true',
    );
    expect(screen.getByRole('radio', { name: /Hábito/ })).toHaveAttribute('aria-checked', 'false');
  });

  it('calls onChange with the selected goal id', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<GoalStep value={EMPTY} onChange={onChange} />);

    await user.click(screen.getByRole('radio', { name: /Hábito/ }));

    expect(onChange).toHaveBeenCalledWith({ selected: 'HABITO' });
  });
});
