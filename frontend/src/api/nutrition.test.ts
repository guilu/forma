import { describe, expect, it, vi } from 'vitest';
import { getNutritionDay } from './nutrition';
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
