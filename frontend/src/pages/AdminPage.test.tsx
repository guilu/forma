import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { AdminPage } from './AdminPage';
import { NotificationProvider } from '../components/NotificationProvider';
import { deleteFood, listFoods, updateFood, type CatalogFood } from '../api/foods';

vi.mock('../api/foods', () => ({
  listFoods: vi.fn(),
  createFood: vi.fn(),
  updateFood: vi.fn(),
  deleteFood: vi.fn(),
}));

const listMock = vi.mocked(listFoods);
const updateMock = vi.mocked(updateFood);
const deleteMock = vi.mocked(deleteFood);

const oats: CatalogFood = {
  id: 'oats',
  name: 'Copos de avena',
  servingSizeG: 60,
  kcal: 370,
  proteinG: 13,
  carbsG: 60,
  fatG: 7,
  category: 'CARBOHIDRATO',
};

const chicken: CatalogFood = {
  id: 'chicken',
  name: 'Pechuga pollo',
  servingSizeG: 200,
  kcal: 110,
  proteinG: 23,
  carbsG: 0,
  fatG: 2,
  category: 'PROTEINA',
};

function renderPage() {
  return render(
    <MemoryRouter>
      <NotificationProvider>
        <AdminPage />
      </NotificationProvider>
    </MemoryRouter>,
  );
}

describe('AdminPage', () => {
  beforeEach(() => {
    listMock.mockReset();
    updateMock.mockReset();
    deleteMock.mockReset();
  });

  it('lists the catalog with its macros per 100 g', async () => {
    listMock.mockResolvedValue([oats, chicken]);

    renderPage();

    expect(await screen.findByRole('tab', { name: 'Macros' })).toBeInTheDocument();
    const table = screen.getByRole('table');
    expect(within(table).getByText('Copos de avena')).toBeInTheDocument();
    expect(within(table).getByText('Pechuga pollo')).toBeInTheDocument();
    // The category the sheet carries, rendered as a label rather than the stored token.
    expect(within(table).getByText('Carbohidrato')).toBeInTheDocument();
  });

  it('edits a food through a form and re-reads the list', async () => {
    listMock.mockResolvedValue([oats]);
    updateMock.mockResolvedValue({ ...oats, name: 'Avena integral' });
    const user = userEvent.setup();

    renderPage();
    await screen.findByText('Copos de avena');

    await user.click(screen.getByRole('button', { name: /Editar Copos de avena/ }));
    const dialog = await screen.findByRole('dialog');
    const name = within(dialog).getByLabelText('Nombre');
    await user.clear(name);
    await user.type(name, 'Avena integral');
    await user.click(within(dialog).getByRole('button', { name: 'Guardar' }));

    await waitFor(() =>
      expect(updateMock).toHaveBeenCalledWith(
        'oats',
        expect.objectContaining({ name: 'Avena integral' }),
      ),
    );
    await waitFor(() => expect(listMock).toHaveBeenCalledTimes(2));
  });

  it('asks before deleting and removes the food once confirmed', async () => {
    listMock.mockResolvedValueOnce([oats]).mockResolvedValue([]);
    deleteMock.mockResolvedValue(undefined);
    const user = userEvent.setup();

    renderPage();
    await screen.findByText('Copos de avena');

    await user.click(screen.getByRole('button', { name: /Eliminar Copos de avena/ }));
    const dialog = await screen.findByRole('dialog');
    expect(deleteMock).not.toHaveBeenCalled();

    await user.click(within(dialog).getByRole('button', { name: 'Eliminar' }));

    await waitFor(() => expect(deleteMock).toHaveBeenCalledWith('oats'));
    await waitFor(() => expect(listMock).toHaveBeenCalledTimes(2));
  });

  /**
   * The backend refuses to delete a food a shopping product still links to. The
   * screen has to say so rather than leaving the row looking deleted.
   */
  it('surfaces a refused delete and keeps the row', async () => {
    listMock.mockResolvedValue([oats]);
    deleteMock.mockRejectedValue(new Error('en uso'));
    const user = userEvent.setup();

    renderPage();
    await screen.findByText('Copos de avena');

    await user.click(screen.getByRole('button', { name: /Eliminar Copos de avena/ }));
    await user.click(
      within(await screen.findByRole('dialog')).getByRole('button', { name: 'Eliminar' }),
    );

    expect(await screen.findByRole('alert')).toHaveTextContent('No se pudo eliminar');
    expect(screen.getByText('Copos de avena')).toBeInTheDocument();
  });
});
