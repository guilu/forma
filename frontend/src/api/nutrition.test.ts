import { describe, expect, it, vi } from 'vitest';
import { getNutritionDay, removeWaterGlass, unmarkPlannedMeal } from './nutrition';
import { type ApiClient } from './client';

describe('getNutritionDay', () => {
  it('GETs the nutrition day endpoint for a type', async () => {
    const day = { type: 'RUNNING', targets: {}, meals: [] };
    const request = vi.fn().mockResolvedValue(day);
    const client: ApiClient = { baseUrl: 'http://test', request, requestBlob: vi.fn() };

    const result = await getNutritionDay('running', client);

    expect(request).toHaveBeenCalledWith('/api/v1/nutrition/days/running');
    expect(result).toBe(day);
  });
});

describe('nutrition decrement commands', () => {
  it('deletes a planned meal only for the requested date', async () => {
    const request = vi.fn().mockResolvedValue(undefined);
    const client: ApiClient = { baseUrl: 'http://test', request, requestBlob: vi.fn() };

    await unmarkPlannedMeal('2026-08-08', 'meal/1', client);

    expect(request).toHaveBeenCalledWith('/api/v1/nutrition/log/planned/meal%2F1?date=2026-08-08', {
      method: 'DELETE',
    });
  });

  it('removes one persisted glass for the requested date', async () => {
    const request = vi.fn().mockResolvedValue({ totalMl: 0 });
    const client: ApiClient = { baseUrl: 'http://test', request, requestBlob: vi.fn() };

    await removeWaterGlass('2026-08-08', client);

    expect(request).toHaveBeenCalledWith('/api/v1/nutrition/hydration/glass?date=2026-08-08', {
      method: 'DELETE',
    });
  });
});
