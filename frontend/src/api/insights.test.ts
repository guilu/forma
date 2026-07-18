import { describe, expect, it, vi } from 'vitest';
import { getInsightsHistory, getWeeklyInsights } from './insights';
import { type ApiClient } from './client';

/**
 * Insights API-module tests (FOR-51). Verifies the request is built on the shared
 * client against the FOR-45 contract path. No real network is used.
 */
describe('getWeeklyInsights', () => {
  it('GETs the weekly insights endpoint and returns the result', async () => {
    const insights = {
      checkIn: { weekStartDate: '2026-07-06', plannedRunningSessions: 3 },
      main: { category: 'BODY', severity: 'INFO', message: 'm', reason: 'r', createdAt: 'now' },
      secondary: [],
      generatedAt: 'now',
    };
    const request = vi.fn().mockResolvedValue(insights);
    const client: ApiClient = { baseUrl: 'http://test', request, requestBlob: vi.fn() };

    const result = await getWeeklyInsights(client);

    expect(request).toHaveBeenCalledWith('/api/v1/insights/weekly');
    expect(result).toBe(insights);
  });
});

/**
 * Insights history API-module tests (FOR-124). Verifies the request is built
 * on the shared client against the FOR-110 history contract path.
 */
describe('getInsightsHistory', () => {
  it('GETs the insights history endpoint and returns the result', async () => {
    const history = [
      {
        checkIn: { weekStartDate: '2026-07-06', plannedRunningSessions: 3 },
        main: { category: 'BODY', severity: 'INFO', message: 'm', reason: 'r', createdAt: 'now' },
        secondary: [],
        generatedAt: 'now',
        deltas: { weightDeltaKg: -0.4 },
      },
    ];
    const request = vi.fn().mockResolvedValue(history);
    const client: ApiClient = { baseUrl: 'http://test', request, requestBlob: vi.fn() };

    const result = await getInsightsHistory(client);

    expect(request).toHaveBeenCalledWith('/api/v1/insights/history');
    expect(result).toBe(history);
  });
});
