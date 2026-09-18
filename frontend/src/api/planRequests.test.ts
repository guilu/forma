import { describe, expect, it, vi } from 'vitest';
import { createPlanRequest, getCurrentPlanRequest, type CreatePlanRequestInput } from './planRequests';
import type { ApiClient } from './client';

describe('planRequests API', () => {
  it('POSTs a new plan request with the wizard-mapped body', async () => {
    const created = {
      id: 'r1',
      status: 'PENDING',
      mainGoal: 'COMPOSICION',
      planObjective: 'WEIGHT_LOSS',
      planKcal: 2078,
      trainingDaysPerWeek: 5,
      trainingWeekdays: ['MONDAY', 'TUESDAY'],
      equipment: ['DUMBBELLS'],
      mealsPerDay: 5,
      dietPattern: 'OMNIVORE',
      cuisineStyle: 'ESPANOLA',
      requestedAt: '2026-09-18T10:00:00Z',
    };
    const request = vi.fn().mockResolvedValue(created);
    const client: ApiClient = { baseUrl: 'http://test', request, requestBlob: vi.fn() };
    const input: CreatePlanRequestInput = {
      direction: 'LOSE_FAT',
      trainingDaysPerWeek: 5,
      trainingWeekdays: ['MONDAY', 'TUESDAY'],
      equipment: ['DUMBBELLS'],
      mealsPerDay: 5,
      dietPattern: 'OMNIVORE',
      cuisineStyle: 'ESPANOLA',
    };

    const result = await createPlanRequest(input, client);

    expect(request).toHaveBeenCalledWith('/api/v1/plan-requests', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(input),
    });
    expect(result).toBe(created);
  });

  it('GETs the caller-s currently open request', async () => {
    const current = { id: 'r1', status: 'GENERATING' };
    const request = vi.fn().mockResolvedValue(current);
    const client: ApiClient = { baseUrl: 'http://test', request, requestBlob: vi.fn() };

    const result = await getCurrentPlanRequest(client);

    expect(request).toHaveBeenCalledWith('/api/v1/plan-requests/current');
    expect(result).toBe(current);
  });
});
