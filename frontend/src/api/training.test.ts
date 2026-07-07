import { describe, expect, it, vi } from 'vitest';
import { getTrainingWeek, updateSessionStatus } from './training';
import { type ApiClient } from './client';

describe('training API', () => {
  it('GETs the training week endpoint', async () => {
    const week = { days: [] };
    const request = vi.fn().mockResolvedValue(week);
    const client: ApiClient = { baseUrl: 'http://test', request };

    const result = await getTrainingWeek(client);

    expect(request).toHaveBeenCalledWith('/api/v1/training/week');
    expect(result).toBe(week);
  });

  it('PATCHes a session status', async () => {
    const request = vi.fn().mockResolvedValue({ id: 'SATURDAY:RUNNING', status: 'COMPLETED' });
    const client: ApiClient = { baseUrl: 'http://test', request };

    await updateSessionStatus('SATURDAY:RUNNING', 'COMPLETED', 'Hecho', client);

    expect(request).toHaveBeenCalledWith('/api/v1/training/sessions/SATURDAY%3ARUNNING/status', {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status: 'COMPLETED', notes: 'Hecho' }),
    });
  });
});
