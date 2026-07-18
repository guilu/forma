import { describe, expect, it, vi } from 'vitest';
import { getStreak, getWeeklyHistory } from './progress';
import { type ApiClient } from './client';

/**
 * Progress API-module tests (FOR-143). Verifies the request is built on the
 * shared client against the FOR-139 contract paths
 * (`backend/.../delivery/progress/ProgressController.java`). No real network
 * is used.
 */
describe('getStreak', () => {
  it('GETs the streak endpoint and returns the result', async () => {
    const streak = { currentStreakDays: 6, longestStreakDays: 21, asOf: '2026-07-18' };
    const request = vi.fn().mockResolvedValue(streak);
    const client: ApiClient = { baseUrl: 'http://test', request };

    const result = await getStreak(client);

    expect(request).toHaveBeenCalledWith('/api/v1/progress/streak');
    expect(result).toBe(streak);
  });
});

describe('getWeeklyHistory', () => {
  it('GETs the weekly-history endpoint and returns the result', async () => {
    const history = {
      weeks: [
        { weekStart: '2026-05-25', planned: 7, completed: 5 },
        { weekStart: '2026-06-01', planned: 7, completed: 7 },
      ],
    };
    const request = vi.fn().mockResolvedValue(history);
    const client: ApiClient = { baseUrl: 'http://test', request };

    const result = await getWeeklyHistory(client);

    expect(request).toHaveBeenCalledWith('/api/v1/progress/weekly-history');
    expect(result).toBe(history);
  });
});
