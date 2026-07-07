import { describe, expect, it, vi } from 'vitest';
import { getTrainingWeek } from './training';
import { type ApiClient } from './client';

describe('getTrainingWeek', () => {
  it('GETs the training week endpoint', async () => {
    const week = { days: [] };
    const request = vi.fn().mockResolvedValue(week);
    const client: ApiClient = { baseUrl: 'http://test', request };

    const result = await getTrainingWeek(client);

    expect(request).toHaveBeenCalledWith('/api/v1/training/week');
    expect(result).toBe(week);
  });
});
