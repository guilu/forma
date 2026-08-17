import { describe, expect, it, vi } from 'vitest';
import {
  getMuscleMap,
  getTrainingWeek,
  getWorkout,
  rescheduleSession,
  updateSessionStatus,
} from './training';
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
    const request = vi.fn().mockResolvedValue({ id: 'RUNNING:LONG_RUN', status: 'COMPLETED' });
    const client: ApiClient = { baseUrl: 'http://test', request, requestBlob: vi.fn() };

    await updateSessionStatus('RUNNING:LONG_RUN', 'COMPLETED', 'Hecho', client);

    expect(request).toHaveBeenCalledWith('/api/v1/training/sessions/RUNNING%3ALONG_RUN/status', {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status: 'COMPLETED', notes: 'Hecho' }),
    });
  });

  it('PATCHes a session onto another day', async () => {
    const week = { days: [] };
    const request = vi.fn().mockResolvedValue(week);
    const client: ApiClient = { baseUrl: 'http://test', request, requestBlob: vi.fn() };

    const result = await rescheduleSession('STRENGTH:PUSH', 'MONDAY', client);

    expect(request).toHaveBeenCalledWith('/api/v1/training/sessions/STRENGTH%3APUSH/schedule', {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ day: 'MONDAY' }),
    });
    // The endpoint answers with the redrawn week, not just the moved session.
    expect(result).toBe(week);
  });

  it('sends an explicit null day to restore the planned day', async () => {
    const request = vi.fn().mockResolvedValue({ days: [] });
    const client: ApiClient = { baseUrl: 'http://test', request, requestBlob: vi.fn() };

    await rescheduleSession('STRENGTH:PUSH', null, client);

    expect(request).toHaveBeenCalledWith('/api/v1/training/sessions/STRENGTH%3APUSH/schedule', {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ day: null }),
    });
  });

  it('GETs the muscle-map endpoint for a session (FOR-136)', async () => {
    const map = { sessionId: 'STRENGTH:PUSH', muscles: [{ muscle: 'pecho', load: 'HIGH' }] };
    const request = vi.fn().mockResolvedValue(map);
    const client: ApiClient = { baseUrl: 'http://test', request, requestBlob: vi.fn() };

    const result = await getMuscleMap('STRENGTH:PUSH', client);

    expect(request).toHaveBeenCalledWith('/api/v1/training/sessions/STRENGTH%3APUSH/muscle-map');
    expect(result).toBe(map);
  });

  it('GETs a strength workout template by type', async () => {
    const workout = { workoutType: 'LEGS', items: [] };
    const request = vi.fn().mockResolvedValue(workout);
    const client: ApiClient = { baseUrl: 'http://test', request, requestBlob: vi.fn() };

    const result = await getWorkout('LEGS', client);

    expect(request).toHaveBeenCalledWith('/api/v1/training/workouts/LEGS');
    expect(result).toBe(workout);
  });
});
