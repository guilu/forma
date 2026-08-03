import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { NotificationProvider } from '../../components/NotificationProvider';
import { ServingsManager } from './ServingsManager';
import {
  createServing,
  deleteServing,
  listServings,
  updateServing,
  type FoodServing,
} from '../../api/servings';
import type { CatalogFood } from '../../api/foods';

vi.mock('../../api/servings', () => ({
  listServings: vi.fn(),
  createServing: vi.fn(),
  updateServing: vi.fn(),
  deleteServing: vi.fn(),
}));

const listMock = vi.mocked(listServings);
const createMock = vi.mocked(createServing);
const updateMock = vi.mocked(updateServing);
const deleteMock = vi.mocked(deleteServing);

const banana: CatalogFood = {
  id: 'banana',
  name: 'Plátano',
  kcal: 89,
  proteinG: 1.1,
  carbsG: 23,
  fatG: 0.3,
};

const plain: FoodServing = {
  id: 'banana',
  foodId: 'banana',
  grams: 120,
  isDefault: true,
  sortOrder: 0,
};

const big: FoodServing = {
  id: 's-big',
  foodId: 'banana',
  name: 'Grande',
  grams: 150,
  isDefault: false,
  sortOrder: 1,
};

function renderManager() {
  return render(
    <NotificationProvider>
      <ServingsManager food={banana} onClose={() => undefined} />
    </NotificationProvider>,
  );
}

describe('ServingsManager', () => {
  beforeEach(() => {
    listMock.mockReset();
    createMock.mockReset();
    updateMock.mockReset();
    deleteMock.mockReset();
    listMock.mockResolvedValue([plain, big]);
  });

  /** The plain portion has no name, and a list still has to call it something. */
  it('names the unnamed portion and marks which one is the default', async () => {
    renderManager();

    expect(await screen.findByText(/Ración · por defecto/)).toBeInTheDocument();
    expect(screen.getByText('Grande')).toBeInTheDocument();
    expect(screen.getByText('150 g')).toBeInTheDocument();
  });

  /** A food nobody has portioned is a real state, and says so rather than showing nothing. */
  it('says when nobody has decided a portion', async () => {
    listMock.mockResolvedValue([]);

    renderManager();

    expect(await screen.findByText(/Nadie ha decidido una porción/)).toBeInTheDocument();
  });

  it('adds a named portion', async () => {
    createMock.mockResolvedValue(big);
    const user = userEvent.setup();

    renderManager();
    await screen.findByText('Grande');

    await user.type(screen.getByLabelText('Nombre'), 'Pequeño');
    await user.type(screen.getByLabelText('Gramos'), '90');
    await user.click(screen.getByRole('button', { name: 'Añadir' }));

    await waitFor(() =>
      expect(createMock).toHaveBeenCalledWith('banana', {
        name: 'Pequeño',
        grams: 90,
        isDefault: false,
        sortOrder: 0,
      }),
    );
  });

  /** A portion with no name is the plain one; forcing a name would mean inventing "Normal". */
  it('sends no name when the field is left blank', async () => {
    listMock.mockResolvedValue([]);
    createMock.mockResolvedValue(plain);
    const user = userEvent.setup();

    renderManager();
    await screen.findByText(/Nadie ha decidido/);

    await user.type(screen.getByLabelText('Gramos'), '120');
    await user.click(screen.getByRole('button', { name: 'Añadir' }));

    await waitFor(() =>
      expect(createMock).toHaveBeenCalledWith(
        'banana',
        expect.objectContaining({ name: undefined }),
      ),
    );
  });

  /**
   * Promoting is a plain edit with the box ticked: the backend takes the marker off whichever
   * portion held it, so this screen never has to unset the old one first.
   */
  it('promotes a portion by ticking the default box', async () => {
    updateMock.mockResolvedValue({ ...big, isDefault: true });
    const user = userEvent.setup();

    renderManager();
    await user.click(await screen.findByRole('button', { name: 'Editar Grande' }));

    await user.click(screen.getByLabelText('Es la ración por defecto'));
    await user.click(screen.getByRole('button', { name: 'Guardar' }));

    await waitFor(() =>
      expect(updateMock).toHaveBeenCalledWith(
        'banana',
        's-big',
        expect.objectContaining({ isDefault: true }),
      ),
    );
  });

  it('removes a portion', async () => {
    deleteMock.mockResolvedValue(undefined);
    const user = userEvent.setup();

    renderManager();
    await user.click(await screen.findByRole('button', { name: 'Eliminar Grande' }));

    await waitFor(() => expect(deleteMock).toHaveBeenCalledWith('banana', 's-big'));
  });

  /** Two portions of one food under the same name is somebody having written it twice. */
  it('surfaces a name the food already uses', async () => {
    const { ApiRequestError } = await import('../../api/client');
    createMock.mockRejectedValue(
      new ApiRequestError(409, 'Ese alimento ya tiene una ración llamada: Grande'),
    );
    const user = userEvent.setup();

    renderManager();
    await screen.findByText('Grande');

    await user.type(screen.getByLabelText('Nombre'), 'Grande');
    await user.type(screen.getByLabelText('Gramos'), '160');
    await user.click(screen.getByRole('button', { name: 'Añadir' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/ya tiene una ración llamada/);
  });
});
