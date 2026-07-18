import { describe, expect, it, vi } from 'vitest';
import {
  getShoppingList,
  listShoppingProducts,
  regenerateShoppingList,
  setItemChecked,
  updateItemQuantity,
  updateShoppingProduct,
} from './shopping';
import { type ApiClient } from './client';

describe('shopping API', () => {
  it('GETs the shopping list', async () => {
    const list = { items: [], budget: {} };
    const request = vi.fn().mockResolvedValue(list);
    const client: ApiClient = { baseUrl: 'http://test', request, requestBlob: vi.fn() };

    const result = await getShoppingList(client);

    expect(request).toHaveBeenCalledWith('/api/v1/shopping/list');
    expect(result).toBe(list);
  });

  it('PATCHes an item checked state', async () => {
    const request = vi.fn().mockResolvedValue({ id: 'i1', checked: true });
    const client: ApiClient = { baseUrl: 'http://test', request, requestBlob: vi.fn() };

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
    const client: ApiClient = { baseUrl: 'http://test', request, requestBlob: vi.fn() };

    const result = await listShoppingProducts(client);

    expect(request).toHaveBeenCalledWith('/api/v1/shopping/products');
    expect(result).toBe(products);
  });

  it('PUTs a product update', async () => {
    const updated = { id: 'p1', name: 'Avena 1 kg', estimatedPriceEur: 2.1 };
    const request = vi.fn().mockResolvedValue(updated);
    const client: ApiClient = { baseUrl: 'http://test', request, requestBlob: vi.fn() };

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

  it('POSTs a regenerate request and returns the rebuilt list (FOR-109/FOR-118)', async () => {
    const rebuilt = { items: [], budget: {}, generatedAt: '2026-07-13T08:00:00Z' };
    const request = vi.fn().mockResolvedValue(rebuilt);
    const client: ApiClient = { baseUrl: 'http://test', request, requestBlob: vi.fn() };

    const result = await regenerateShoppingList(client);

    expect(request).toHaveBeenCalledWith('/api/v1/shopping/list/regenerate', { method: 'POST' });
    expect(result).toBe(rebuilt);
  });

  it('PATCHes an item quantity and returns the recalculated result (FOR-109/FOR-118)', async () => {
    const updated = { id: 'i1', quantity: 3, estimatedCostEur: 5.85, unit: 'KG' };
    const request = vi.fn().mockResolvedValue(updated);
    const client: ApiClient = { baseUrl: 'http://test', request, requestBlob: vi.fn() };

    const result = await updateItemQuantity('i1', 3, client);

    expect(request).toHaveBeenCalledWith('/api/v1/shopping/list/items/i1', {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ quantity: 3 }),
    });
    expect(result).toBe(updated);
  });
});
