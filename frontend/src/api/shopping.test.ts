import { describe, expect, it, vi } from 'vitest';
import { getShoppingList, setItemChecked } from './shopping';
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
});
