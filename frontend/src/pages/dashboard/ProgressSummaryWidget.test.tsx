import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { ProgressSummaryWidget } from './ProgressSummaryWidget';
import { listGoals, type Goal } from '../../api/goals';

vi.mock('../../api/goals', () => ({ listGoals: vi.fn() }));

const goalsMock = vi.mocked(listGoals);

function renderWidget() {
  return render(
    <MemoryRouter>
      <ProgressSummaryWidget />
    </MemoryRouter>,
  );
}

const goal: Goal = {
  id: 'g1',
  title: 'Bajar a 68 kg',
  metric: 'WEIGHT_KG',
  target: 68,
  dueDate: '2026-09-01',
  status: 'ACTIVE',
  progress: { current: 69.2, target: 68, ratio: 0.78, source: 'BODY_MEASUREMENT' },
  milestones: [],
};

describe('ProgressSummaryWidget', () => {
  beforeEach(() => {
    goalsMock.mockReset();
  });

  it('shows the primary goal and its backend-derived progress ratio', async () => {
    goalsMock.mockResolvedValue([goal]);

    renderWidget();

    expect(await screen.findByText('Bajar a 68 kg')).toBeInTheDocument();
    expect(screen.getByText('Objetivo: 68 kg')).toBeInTheDocument();
    // ratio 0.78 -> 78%, straight from the backend (never recomputed).
    expect(screen.getByText('78%')).toBeInTheDocument();
    expect(screen.getByRole('img', { name: /Progreso del objetivo: 78%/ })).toBeInTheDocument();
  });

  it('renders a neutral "sin datos" state when the ratio is null, never a fabricated 0%', async () => {
    goalsMock.mockResolvedValue([
      { ...goal, progress: { current: null, target: 68, ratio: null, source: 'BODY_MEASUREMENT' } },
    ]);

    renderWidget();

    expect(await screen.findByText('Sin datos')).toBeInTheDocument();
    expect(screen.getByRole('img', { name: /sin datos todavía/ })).toBeInTheDocument();
  });

  it('nudges the user to define a goal when there are none', async () => {
    goalsMock.mockResolvedValue([]);

    renderWidget();

    expect(await screen.findByText(/Define un objetivo/)).toBeInTheDocument();
  });

  it('shows an error state when the request fails', async () => {
    goalsMock.mockRejectedValue(new Error('network'));

    renderWidget();

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo cargar tu progreso');
  });
});
