import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { GoalsPage } from './GoalsPage';
import { NotificationProvider } from '../components/NotificationProvider';
import { ApiRequestError } from '../api/client';
import { createGoal, listGoals, updateGoal, type Goal } from '../api/goals';
import { axe } from '../test/axe';

/** GoalsPage calls `useNotify()` (FOR-63), which requires a provider. */
function renderPage() {
  return render(
    <NotificationProvider>
      <GoalsPage />
    </NotificationProvider>,
  );
}

vi.mock('../api/goals', () => ({
  listGoals: vi.fn(),
  createGoal: vi.fn(),
  updateGoal: vi.fn(),
}));

const listGoalsMock = vi.mocked(listGoals);
const createGoalMock = vi.mocked(createGoal);
const updateGoalMock = vi.mocked(updateGoal);

/**
 * Fixture spans the two documented progress shapes (FOR-125 api.md): a goal
 * with real derived progress (g1) and one whose metric has no data yet, so
 * `current`/`ratio` are explicit nulls, never fabricated as 0 (g2).
 */
const GOAL_WITH_PROGRESS: Goal = {
  id: 'g1',
  title: 'Bajar a 12% grasa',
  metric: 'BODY_FAT_PCT',
  target: 12,
  dueDate: '2026-12-31',
  status: 'ACTIVE',
  progress: { current: 16.4, target: 12, ratio: 16.4 / 12, source: 'BODY_MEASUREMENT' },
  milestones: [{ id: 'm1', title: '15%', target: 15, completed: false }],
};

const GOAL_WITHOUT_PROGRESS: Goal = {
  id: 'g2',
  title: 'Aumentar masa muscular',
  metric: 'LEAN_MASS_KG',
  target: 60,
  dueDate: null,
  status: 'ACTIVE',
  progress: { current: null, target: 60, ratio: null, source: 'BODY_MEASUREMENT' },
  milestones: [],
};

describe('GoalsPage (FOR-122)', () => {
  beforeEach(() => {
    listGoalsMock.mockReset();
    createGoalMock.mockReset();
    updateGoalMock.mockReset();
  });

  it('shows a loading state while goals are fetched', () => {
    listGoalsMock.mockReturnValue(new Promise(() => {}));

    renderPage();

    expect(screen.getByText('Cargando tus objetivos…')).toBeInTheDocument();
  });

  it('shows an empty state with a create entry point when there are no goals', async () => {
    listGoalsMock.mockResolvedValue([]);

    renderPage();

    expect(await screen.findByText('Aún no tienes objetivos.')).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: 'Crear objetivo' }).length).toBeGreaterThan(0);
  });

  it('shows an error state with a working retry on load failure', async () => {
    listGoalsMock.mockRejectedValueOnce(new Error('network down'));
    listGoalsMock.mockResolvedValueOnce([GOAL_WITH_PROGRESS]);
    const user = userEvent.setup();

    renderPage();

    expect(
      await screen.findByText('No se pudieron cargar tus objetivos. Inténtalo de nuevo más tarde.'),
    ).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByRole('heading', { name: 'Bajar a 12% grasa' })).toBeInTheDocument();
    expect(listGoalsMock).toHaveBeenCalledTimes(2);
  });

  it('renders goal cards with derived progress, status, due date and milestones', async () => {
    listGoalsMock.mockResolvedValue([GOAL_WITH_PROGRESS, GOAL_WITHOUT_PROGRESS]);

    renderPage();

    expect(await screen.findByRole('heading', { name: 'Bajar a 12% grasa' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Aumentar masa muscular' })).toBeInTheDocument();

    // Metric label (display-only mapping, not client-side math).
    expect(screen.getByText('Grasa corporal')).toBeInTheDocument();
    expect(screen.getByText('Masa magra')).toBeInTheDocument();

    // Real progress: renders the backend's own current/target values verbatim.
    expect(screen.getByText('16.4 % / 12 %')).toBeInTheDocument();

    // Unlinked/no-data progress: a calm neutral message, never a fabricated 0%.
    expect(screen.getByText('Sin datos de progreso todavía.')).toBeInTheDocument();
    expect(screen.queryByText('0 kg / 60 kg')).not.toBeInTheDocument();

    // Due dates, including the "no due date" case.
    expect(screen.getByText('Meta: 31 dic 2026')).toBeInTheDocument();
    expect(screen.getByText('Meta: Sin fecha límite')).toBeInTheDocument();

    // Status pills (both ACTIVE here).
    expect(screen.getAllByText('Activo')).toHaveLength(2);

    // Milestone for g1, none for g2.
    expect(screen.getByRole('checkbox', { name: '15%' })).toBeInTheDocument();
  });

  it("toggles a milestone's completed state via PATCH and reflects the update", async () => {
    listGoalsMock.mockResolvedValue([GOAL_WITH_PROGRESS]);
    updateGoalMock.mockResolvedValue({
      ...GOAL_WITH_PROGRESS,
      milestones: [{ id: 'm1', title: '15%', target: 15, completed: true }],
    });
    const user = userEvent.setup();

    renderPage();
    const checkbox = await screen.findByRole('checkbox', { name: '15%' });

    await user.click(checkbox);

    expect(updateGoalMock).toHaveBeenCalledWith('g1', {
      milestones: [{ id: 'm1', completed: true }],
    });
    await waitFor(() => expect(checkbox).toBeChecked());
    expect(await screen.findByText('Hito actualizado.')).toBeInTheDocument();
  });

  it('creates a goal through the "Crear objetivo" form', async () => {
    listGoalsMock.mockResolvedValue([]);
    const created: Goal = {
      id: 'new-id',
      title: 'Correr 10K',
      metric: 'WEIGHT_KG',
      target: 70,
      dueDate: null,
      status: 'ACTIVE',
      progress: { current: null, target: 70, ratio: null, source: 'BODY_MEASUREMENT' },
      milestones: [],
    };
    createGoalMock.mockResolvedValue(created);
    const user = userEvent.setup();

    renderPage();
    await user.click((await screen.findAllByRole('button', { name: 'Crear objetivo' }))[0]);

    const dialog = await screen.findByRole('dialog', { name: 'Crear objetivo' });
    await user.type(within(dialog).getByLabelText('Título'), 'Correr 10K');
    await user.selectOptions(within(dialog).getByLabelText('Métrica'), 'WEIGHT_KG');
    await user.type(within(dialog).getByLabelText(/Objetivo/), '70');
    await user.click(within(dialog).getByRole('button', { name: 'Crear' }));

    await waitFor(() =>
      expect(createGoalMock).toHaveBeenCalledWith({
        title: 'Correr 10K',
        metric: 'WEIGHT_KG',
        target: 70,
      }),
    );
    expect(await screen.findByRole('heading', { name: 'Correr 10K' })).toBeInTheDocument();
    expect(await screen.findByText('Objetivo creado.')).toBeInTheDocument();
  });

  it('validates required fields before submitting a new goal, without calling the API', async () => {
    listGoalsMock.mockResolvedValue([]);
    const user = userEvent.setup();

    renderPage();
    await user.click((await screen.findAllByRole('button', { name: 'Crear objetivo' }))[0]);
    const dialog = await screen.findByRole('dialog', { name: 'Crear objetivo' });

    await user.click(within(dialog).getByRole('button', { name: 'Crear' }));

    expect(await within(dialog).findByText('Introduce un título.')).toBeInTheDocument();
    expect(createGoalMock).not.toHaveBeenCalled();
  });

  it('maps backend field validation errors onto the create form', async () => {
    listGoalsMock.mockResolvedValue([]);
    createGoalMock.mockRejectedValue(
      new ApiRequestError(400, 'Validación fallida', 'VALIDATION_ERROR', [
        { field: 'title', message: 'El título es obligatorio.' },
      ]),
    );
    const user = userEvent.setup();

    renderPage();
    await user.click((await screen.findAllByRole('button', { name: 'Crear objetivo' }))[0]);
    const dialog = await screen.findByRole('dialog', { name: 'Crear objetivo' });
    await user.type(within(dialog).getByLabelText('Título'), 'X');
    await user.selectOptions(within(dialog).getByLabelText('Métrica'), 'WEIGHT_KG');
    await user.type(within(dialog).getByLabelText(/Objetivo/), '70');
    await user.click(within(dialog).getByRole('button', { name: 'Crear' }));

    expect(await within(dialog).findByText('El título es obligatorio.')).toBeInTheDocument();
  });

  it('edits a goal through the edit form, pre-filled with its current values', async () => {
    listGoalsMock.mockResolvedValue([GOAL_WITH_PROGRESS]);
    updateGoalMock.mockResolvedValue({
      ...GOAL_WITH_PROGRESS,
      title: 'Bajar a 11% grasa',
      target: 11,
    });
    const user = userEvent.setup();

    renderPage();
    await user.click(
      await screen.findByRole('button', { name: 'Editar objetivo Bajar a 12% grasa' }),
    );

    const dialog = await screen.findByRole('dialog', { name: 'Editar objetivo' });
    const titleField = within(dialog).getByLabelText('Título') as HTMLInputElement;
    expect(titleField.value).toBe('Bajar a 12% grasa');

    await user.clear(titleField);
    await user.type(titleField, 'Bajar a 11% grasa');
    await user.click(within(dialog).getByRole('button', { name: 'Guardar' }));

    await waitFor(() =>
      expect(updateGoalMock).toHaveBeenCalledWith(
        'g1',
        expect.objectContaining({ title: 'Bajar a 11% grasa', target: 12 }),
      ),
    );
    expect(await screen.findByRole('heading', { name: 'Bajar a 11% grasa' })).toBeInTheDocument();
    expect(await screen.findByText('Objetivo actualizado.')).toBeInTheDocument();
  });

  it('has no detectable accessibility violations in the ready state', async () => {
    listGoalsMock.mockResolvedValue([GOAL_WITH_PROGRESS, GOAL_WITHOUT_PROGRESS]);

    const { container } = renderPage();
    await screen.findByRole('heading', { name: 'Bajar a 12% grasa' });

    expect(await axe(container)).toHaveNoViolations();
  });
});
