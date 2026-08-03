import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { AdminPage } from '../AdminPage';
import { NotificationProvider } from '../../components/NotificationProvider';
import { listFoods, type CatalogFood } from '../../api/foods';
import {
  createStoreProduct,
  listStoreProducts,
  listStoreSuggestions,
  updateStoreProduct,
  type StoreProduct,
} from '../../api/storeProducts';

vi.mock('../../api/foods', () => ({
  listFoods: vi.fn(),
  createFood: vi.fn(),
  updateFood: vi.fn(),
  deleteFood: vi.fn(),
}));
vi.mock('../../api/storeProducts', () => ({
  listStoreProducts: vi.fn(),
  createStoreProduct: vi.fn(),
  updateStoreProduct: vi.fn(),
  deleteStoreProduct: vi.fn(),
  refreshStoreProduct: vi.fn(),
  listStoreSuggestions: vi.fn(),
}));

const foodsMock = vi.mocked(listFoods);
const productsMock = vi.mocked(listStoreProducts);
const suggestMock = vi.mocked(listStoreSuggestions);
const createMock = vi.mocked(createStoreProduct);
const updateMock = vi.mocked(updateStoreProduct);

const oats: CatalogFood = {
  id: 'oats',
  name: 'Copos de avena',
  servingSizeG: 60,
  kcal: 370,
  proteinG: 13,
  carbsG: 60,
  fatG: 7,
  foodGroupId: 'CARBOHIDRATO',
};

const suggestion = {
  externalId: '4241',
  name: 'Copos de avena Brüggen',
  packaging: 'Caja 500 g',
  priceEur: 1.55,
  url: 'https://tienda.mercadona.es/product/4241',
  storeCategory: 'Cereales',
  imageUrl: 'https://prod-mercadona.imgix.net/images/abc.jpg?fit=crop&h=300&w=300',
};

async function openImportFor(name: string) {
  const user = userEvent.setup();
  render(
    <MemoryRouter>
      <NotificationProvider>
        <AdminPage />
      </NotificationProvider>
    </MemoryRouter>,
  );
  await screen.findByText(name);
  await user.click(screen.getByRole('button', { name: `Importar ${name} de Mercadona` }));
  return user;
}

/**
 * Importing lives on the Macros tab because it answers a question about a FOOD — "what does
 * Mercadona sell for this?" — even though what it produces is a store product. Starting from the
 * food is also what fills the link between the two by construction instead of leaving it for later.
 */
describe('AdminPage — importing a store product for a food', () => {
  beforeEach(() => {
    foodsMock.mockReset();
    productsMock.mockReset();
    suggestMock.mockReset();
    createMock.mockReset();
    updateMock.mockReset();
    foodsMock.mockResolvedValue([oats]);
    productsMock.mockResolvedValue([]);
    suggestMock.mockResolvedValue([suggestion]);
  });

  it("offers the shop's products for the food in that row", async () => {
    await openImportFor('Copos de avena');

    await waitFor(() => expect(suggestMock).toHaveBeenCalledWith('oats', 'MERCADONA'));
    const picker = await screen.findByRole('dialog', { name: /Importar de Mercadona/ });
    expect(within(picker).getByText('Copos de avena Brüggen')).toBeInTheDocument();
    expect(within(picker).getByText(/1,55/)).toBeInTheDocument();
    // The shop's photo, asked of their CDN at the size it is drawn.
    expect(within(picker).getByRole('presentation', { hidden: true })).toHaveAttribute(
      'src',
      'https://prod-mercadona.imgix.net/images/abc.jpg?fit=crop&h=40&w=40',
    );
  });

  it('prefills a new product with the shop values and the food already linked', async () => {
    const user = await openImportFor('Copos de avena');
    const picker = await screen.findByRole('dialog', { name: /Importar de Mercadona/ });

    await user.click(within(picker).getByRole('button', { name: /Usar Copos de avena Brüggen/ }));

    const form = await screen.findByRole('dialog', { name: /Nuevo producto/ });
    expect(within(form).getByLabelText('Identificador')).toHaveValue('mercadona-4241');
    expect(within(form).getByLabelText('Nombre')).toHaveValue('Copos de avena Brüggen');
    expect(within(form).getByLabelText('Precio (€)')).toHaveValue(1.55);
    expect(within(form).getByLabelText('Alimento enlazado')).toHaveValue('oats');
  });

  /**
   * The same product imported twice used to fail on its id, because create is the only thing the
   * picker knew how to do. It updates the row that exists instead — and keeps the aisle and notes
   * an admin filed it under, which the shop knows nothing about.
   */
  /**
   * The shop's id and photo are provenance, not fields: the form never shows them, so it used to
   * drop them on save. Every imported product was then born unable to refresh itself and without a
   * picture — which is exactly what the catalog looked like.
   */
  it('keeps the shop id and photo when saving an imported product', async () => {
    createMock.mockResolvedValue({
      id: 'mercadona-4241',
      store: 'MERCADONA',
      name: 'Copos de avena Brüggen',
      category: 'OTROS',
    });
    const user = await openImportFor('Copos de avena');
    const picker = await screen.findByRole('dialog', { name: /Importar de Mercadona/ });
    await user.click(within(picker).getByRole('button', { name: /Usar Copos de avena Brüggen/ }));

    const form = await screen.findByRole('dialog', { name: /Nuevo producto/ });
    await user.click(within(form).getByRole('button', { name: 'Guardar' }));

    await waitFor(() =>
      expect(createMock).toHaveBeenCalledWith(
        expect.objectContaining({ externalId: '4241', imageUrl: suggestion.imageUrl }),
      ),
    );
  });

  it('updates the product when it is already in the catalog', async () => {
    const stored: StoreProduct = {
      id: 'mercadona-4241',
      store: 'MERCADONA',
      name: 'Copos de avena Brüggen',
      foodId: 'oats',
      packageSize: 'Caja 500 g',
      priceEur: 1.35,
      category: 'CEREALES_Y_LEGUMBRES',
      notes: 'Comprar dos si hay oferta',
      externalId: '4241',
    };
    productsMock.mockResolvedValue([stored]);
    updateMock.mockResolvedValue(stored);
    const user = await openImportFor('Copos de avena');
    const picker = await screen.findByRole('dialog', { name: /Importar de Mercadona/ });

    await user.click(within(picker).getByRole('button', { name: /Usar Copos de avena Brüggen/ }));

    const form = await screen.findByRole('dialog', { name: /Actualizar Copos de avena Brüggen/ });
    // The shop's current price, not the stored one.
    expect(within(form).getByLabelText('Precio (€)')).toHaveValue(1.55);
    // Ours, kept.
    expect(within(form).getByLabelText('Categoría')).toHaveValue('CEREALES_Y_LEGUMBRES');

    await user.click(within(form).getByRole('button', { name: 'Guardar' }));

    await waitFor(() =>
      expect(updateMock).toHaveBeenCalledWith(
        'mercadona-4241',
        expect.objectContaining({ priceEur: 1.55, notes: 'Comprar dos si hay oferta' }),
      ),
    );
    expect(createMock).not.toHaveBeenCalled();
  });

  it('says so when the shop has nothing that matches', async () => {
    suggestMock.mockResolvedValue([]);

    await openImportFor('Copos de avena');

    expect(await screen.findByText(/no ha encontrado/i)).toBeInTheDocument();
  });

  /** The shop being down must read as "vuelve a intentarlo", not as "ese alimento no existe". */
  it('reports a shop that cannot be reached', async () => {
    suggestMock.mockRejectedValue(new Error('502'));

    await openImportFor('Copos de avena');

    expect(await screen.findByRole('alert')).toHaveTextContent(/Mercadona/);
  });
});
