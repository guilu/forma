import { describe, expect, it, vi } from 'vitest';
import {
  getShoppingList,
  listShoppingProducts,
  setItemChecked,
  updateShoppingProduct,
} from './shopping';
import { type ApiClient } from './client';

describe('shopping API', () => {
  it('GETs the shopping list', async () => {
    const list = { items: [], budget: {} };
    const request = vi.fn().mockResolvedValue(list);
    const client: ApiClient = { baseUrl: 'http://test', request };

    const result = await getShoppingList(client);

    expect(request).toHaveBeenCalledWith('/api/v1/shopping/list');
    expect(result).toBe(list);
  });

  it('PATCHes an item checked state', async () => {
    const request = vi.fn().mockResolvedValue({ id: 'i1', checked: true });
    const client: ApiClient = { baseUrl: 'http://test', request };

    await setItemChecked('i1', true, client);

    expect(request).toHaveBeenCalledWith('/api/v1/shopping/list/items/i1/checked', {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ checked: true }),
    });
  });

  it('GETs the shopping products list', async () => {
    const products = [{ id: 'p1', name: 'Avena 1 kg', estimatedPriceEur: 1.95 }];
    const request = vi.fn().mockResolvedValue(products);
    const client: ApiClient = { baseUrl: 'http://test', request };

    const result = await listShoppingProducts(client);

    expect(request).toHaveBeenCalledWith('/api/v1/shopping/products');
    expect(result).toBe(products);
  });

  it('PUTs a product update', async () => {
    const updated = { id: 'p1', name: 'Avena 1 kg', estimatedPriceEur: 2.1 };
    const request = vi.fn().mockResolvedValue(updated);
    const client: ApiClient = { baseUrl: 'http://test', request };

    const result = await updateShoppingProduct(
      'p1',
      { name: 'Avena 1 kg', estimatedPriceEur: 2.1, url: 'https://tienda.example/avena' },
      client,
    );

    expect(request).toHaveBeenCalledWith('/api/v1/shopping/products/p1', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: 'Avena 1 kg',
        estimatedPriceEur: 2.1,
        url: 'https://tienda.example/avena',
      }),
    });
    expect(result).toBe(updated);
  });
});
