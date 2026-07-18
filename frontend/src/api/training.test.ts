import { describe, expect, it, vi } from 'vitest';
import { getMuscleMap, getTrainingWeek, updateSessionStatus } from './training';
import { type ApiClient } from './client';

describe('training API', () => {
  it('GETs the training week endpoint', async () => {
    const week = { days: [] };
    const request = vi.fn().mockResolvedValue(week);
    const client: ApiClient = { baseUrl: 'http://test', request, requestBlob: vi.fn() };

    const result = await getTrainingWeek(client);

    expect(request).toHaveBeenCalledWith('/api/v1/training/week');
    expect(result).toBe(week);
  });

  it('PATCHes a session status', async () => {
    const request = vi.fn().mockResolvedValue({ id: 'SATURDAY:RUNNING', status: 'COMPLETED' });
    const client: ApiClient = { baseUrl: 'http://test', request, requestBlob: vi.fn() };

    await updateSessionStatus('SATURDAY:RUNNING', 'COMPLETED', 'Hecho', client);

    expect(request).toHaveBeenCalledWith('/api/v1/training/sessions/SATURDAY%3ARUNNING/status', {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status: 'COMPLETED', notes: 'Hecho' }),
    });
  });

  it('GETs the muscle-map endpoint for a session (FOR-136)', async () => {
    const map = { sessionId: 'MONDAY:STRENGTH', muscles: [{ muscle: 'pecho', load: 'HIGH' }] };
    const request = vi.fn().mockResolvedValue(map);
    const client: ApiClient = { baseUrl: 'http://test', request, requestBlob: vi.fn() };

    const result = await getMuscleMap('MONDAY:STRENGTH', client);

    expect(request).toHaveBeenCalledWith('/api/v1/training/sessions/MONDAY%3ASTRENGTH/muscle-map');
    expect(result).toBe(map);
  });
});
