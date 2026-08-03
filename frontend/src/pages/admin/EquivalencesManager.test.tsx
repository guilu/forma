import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { NotificationProvider } from '../../components/NotificationProvider';
import { EquivalencesManager } from './EquivalencesManager';
import {
  createEquivalence,
  deleteEquivalence,
  listEquivalences,
  type FoodEquivalence,
} from '../../api/equivalences';
import type { CatalogFood } from '../../api/foods';

vi.mock('../../api/equivalences', () => ({
  listEquivalences: vi.fn(),
  createEquivalence: vi.fn(),
  deleteEquivalence: vi.fn(),
}));

const listMock = vi.mocked(listEquivalences);
const createMock = vi.mocked(createEquivalence);
const deleteMock = vi.mocked(deleteEquivalence);

const rice: CatalogFood = {
  id: 'rice',
  name: 'Arroz',
  kcal: 360,
  proteinG: 7,
  carbsG: 79,
  fatG: 1,
};

const potato: CatalogFood = {
  id: 'potato',
  name: 'Patata',
  kcal: 77,
  proteinG: 2,
  carbsG: 17,
  fatG: 0.1,
};

/**
 * Rice for potato on carbohydrate, with the real figures: 100 g of rice carries
 * 79 g of carbohydrate and potato carries 17 g per 100 g. Carbs are the nutrient
 * held equal, so their drift is absent rather than zero.
 */
const riceToPotato: FoodEquivalence = {
  id: 'e1',
  sourceFoodId: 'rice',
  targetFoodId: 'potato',
  targetName: 'Patata',
  basis: 'CARBS',
  sourceReferenceG: 100,
  targetReferenceG: 464.7,
  caloriesDeviationPct: -0.6,
  proteinDeviationPct: 32.8,
  fatDeviationPct: -53.5,
  maxMacroDeviationPct: 25,
  exceedsTolerance: true,
};

function renderManager() {
  return render(
    <NotificationProvider>
      <EquivalencesManager food={rice} foods={[rice, potato]} onClose={() => undefined} />
    </NotificationProvider>,
  );
}

describe('EquivalencesManager', () => {
  beforeEach(() => {
    listMock.mockReset();
    createMock.mockReset();
    deleteMock.mockReset();
    listMock.mockResolvedValue([riceToPotato]);
  });

  /** The grams are a number nobody typed: the backend works them out from the catalog. */
  it('shows the grams the backend worked out', async () => {
    renderManager();

    // Scoped to the row: "Patata" is also one of the options in the picker below.
    const row = (await screen.findByText('100 g → 464.7 g')).closest('li');
    expect(row).not.toBeNull();
    expect(within(row as HTMLElement).getByText('Patata')).toBeInTheDocument();
    expect(within(row as HTMLElement).getByText('Hidratos')).toBeInTheDocument();
  });

  /**
   * The distinction the backend went to trouble to make. Carbs are held equal, so
   * their drift is absent — and absent is not zero. Printing "hidratos 0 %" would
   * claim a measurement nobody made.
   */
  it('leaves out the macro being held equal rather than showing it as zero', async () => {
    renderManager();

    await screen.findByText('100 g → 464.7 g');
    expect(screen.getByText(/proteína \+32.8 %/)).toBeInTheDocument();
    expect(screen.getByText(/grasa -53.5 %/)).toBeInTheDocument();
    expect(screen.queryByText(/hidratos .*%/)).not.toBeInTheDocument();
  });

  /** A food nobody has given substitutes for says so rather than showing an empty box. */
  it('says when nobody has stated a substitution', async () => {
    listMock.mockResolvedValue([]);

    renderManager();

    expect(await screen.findByText(/Nadie ha dicho todavía/)).toBeInTheDocument();
  });

  /** A food cannot stand in for itself, so it is not offered rather than offered and refused. */
  it('does not offer the food as its own substitute', async () => {
    renderManager();
    await screen.findByText('100 g → 464.7 g');

    const select = screen.getByLabelText('Se puede sustituir por');
    expect(select).not.toHaveTextContent('Arroz');
    expect(select).toHaveTextContent('Patata');
  });

  it('states a substitution', async () => {
    createMock.mockResolvedValue(riceToPotato);
    const user = userEvent.setup();

    renderManager();
    await screen.findByText('100 g → 464.7 g');

    await user.selectOptions(screen.getByLabelText('Se puede sustituir por'), 'potato');
    await user.selectOptions(screen.getByLabelText('Igualando'), 'CARBS');
    await user.click(screen.getByRole('button', { name: 'Añadir' }));

    await waitFor(() =>
      expect(createMock).toHaveBeenCalledWith({
        sourceFoodId: 'rice',
        targetFoodId: 'potato',
        basis: 'CARBS',
        sourceReferenceG: 100,
        maxMacroDeviationPct: undefined,
      }),
    );
  });

  /**
   * No amount of hake makes up the carbohydrate in rice. The backend says which
   * nutrient is missing and from which food, and that sentence beats anything
   * this screen could invent.
   */
  it('surfaces the backend refusal verbatim', async () => {
    const { ApiRequestError } = await import('../../api/client');
    createMock.mockRejectedValue(
      new ApiRequestError(400, 'both foods must carry CARBS: rice has 79.0, fish has 0.0'),
    );
    const user = userEvent.setup();

    renderManager();
    await screen.findByText('100 g → 464.7 g');

    await user.selectOptions(screen.getByLabelText('Se puede sustituir por'), 'potato');
    await user.click(screen.getByRole('button', { name: 'Añadir' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/must carry CARBS/);
  });

  it('removes a substitution', async () => {
    deleteMock.mockResolvedValue(undefined);
    const user = userEvent.setup();

    renderManager();
    await user.click(
      await screen.findByRole('button', { name: 'Eliminar equivalencia con Patata' }),
    );

    await waitFor(() => expect(deleteMock).toHaveBeenCalledWith('e1'));
  });
});
