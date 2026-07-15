import { describe, expect, it, vi } from 'vitest';
import { createGoal, listGoals, updateGoal, type Goal, type GoalsListResponse } from './goals';
import { type ApiClient } from './client';

describe('goals API (FOR-125 delivery/goals contract, FOR-122)', () => {
  it('GETs the goals list and unwraps the {goals: [...]} envelope', async () => {
    const goal: Goal = {
      id: 'g1',
      title: 'Bajar a 12% grasa',
      metric: 'BODY_FAT_PCT',
      target: 12,
      dueDate: '2026-12-31',
      status: 'ACTIVE',
      progress: { current: 16.4, target: 12, ratio: 16.4 / 12, source: 'BODY_MEASUREMENT' },
      milestones: [{ id: 'm1', title: '15%', target: 15, completed: false }],
    };
    const response: GoalsListResponse = { goals: [goal] };
    const request = vi.fn().mockResolvedValue(response);
    const client: ApiClient = { baseUrl: 'http://test', request };

    const result = await listGoals(client);

    expect(request).toHaveBeenCalledWith('/api/v1/goals');
    expect(result).toEqual([goal]);
  });

  it('returns an empty array, never throwing, when the backend returns no goals', async () => {
    const request = vi.fn().mockResolvedValue({ goals: [] });
    const client: ApiClient = { baseUrl: 'http://test', request };

    const result = await listGoals(client);

    expect(result).toEqual([]);
  });

  it('POSTs a create request with title/metric/target and omits optional fields when absent', async () => {
    const created: Goal = {
      id: 'new-id',
      title: 'Bajar a 12% grasa',
      metric: 'BODY_FAT_PCT',
      target: 12,
      dueDate: null,
      status: 'ACTIVE',
      progress: { current: null, target: 12, ratio: null, source: 'BODY_MEASUREMENT' },
      milestones: [],
    };
    const request = vi.fn().mockResolvedValue(created);
    const client: ApiClient = { baseUrl: 'http://test', request };

    const result = await createGoal(
      { title: 'Bajar a 12% grasa', metric: 'BODY_FAT_PCT', target: 12 },
      client,
    );

    expect(request).toHaveBeenCalledWith('/api/v1/goals', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ title: 'Bajar a 12% grasa', metric: 'BODY_FAT_PCT', target: 12 }),
    });
    expect(result).toBe(created);
  });

  it('POSTs a create request including optional dueDate and milestones', async () => {
    const created: Goal = {
      id: 'new-id',
      title: 'Bajar a 12% grasa',
      metric: 'BODY_FAT_PCT',
      target: 12,
      dueDate: '2026-12-31',
      status: 'ACTIVE',
      progress: { current: null, target: 12, ratio: null, source: 'BODY_MEASUREMENT' },
      milestones: [{ id: 'm1', title: '15%', target: 15, completed: false }],
    };
    const request = vi.fn().mockResolvedValue(created);
    const client: ApiClient = { baseUrl: 'http://test', request };

    await createGoal(
      {
        title: 'Bajar a 12% grasa',
        metric: 'BODY_FAT_PCT',
        target: 12,
        dueDate: '2026-12-31',
        milestones: [{ title: '15%', target: 15 }],
      },
      client,
    );

    expect(request).toHaveBeenCalledWith('/api/v1/goals', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        title: 'Bajar a 12% grasa',
        metric: 'BODY_FAT_PCT',
        target: 12,
        dueDate: '2026-12-31',
        milestones: [{ title: '15%', target: 15 }],
      }),
    });
  });

  it('PATCHes a goal update by id', async () => {
    const updated: Goal = {
      id: 'g1',
      title: 'Bajar a 11% grasa',
      metric: 'BODY_FAT_PCT',
      target: 11,
      dueDate: null,
      status: 'ACTIVE',
      progress: { current: null, target: 11, ratio: null, source: 'BODY_MEASUREMENT' },
      milestones: [{ id: 'm1', title: '15%', target: 15, completed: true }],
    };
    const request = vi.fn().mockResolvedValue(updated);
    const client: ApiClient = { baseUrl: 'http://test', request };

    const result = await updateGoal(
      'g1',
      { title: 'Bajar a 11% grasa', target: 11, milestones: [{ id: 'm1', completed: true }] },
      client,
    );

    expect(request).toHaveBeenCalledWith('/api/v1/goals/g1', {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        title: 'Bajar a 11% grasa',
        target: 11,
        milestones: [{ id: 'm1', completed: true }],
      }),
    });
    expect(result).toBe(updated);
  });

  it('encodes the goal id in the PATCH path', async () => {
    const request = vi.fn().mockResolvedValue({});
    const client: ApiClient = { baseUrl: 'http://test', request };

    await updateGoal('g/1', { title: 'X' }, client);

    expect(request).toHaveBeenCalledWith(
      '/api/v1/goals/g%2F1',
      expect.objectContaining({ method: 'PATCH' }),
    );
  });
});
