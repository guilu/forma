import { beforeEach, describe, expect, it, vi } from 'vitest';
import { apiClient } from './client';
import { acceptPlan, getPlanAcceptance } from './planAcceptance';

vi.mock('./client', () => ({
  apiClient: { request: vi.fn() },
}));

describe('plan acceptance API', () => {
  beforeEach(() => vi.mocked(apiClient.request).mockReset());

  it('reads whether a plan is waiting to be started', async () => {
    vi.mocked(apiClient.request).mockResolvedValue({ pending: true, planName: 'Dieta semanal' });

    await expect(getPlanAcceptance()).resolves.toEqual({
      pending: true,
      planName: 'Dieta semanal',
    });
    expect(apiClient.request).toHaveBeenCalledWith('/api/v1/plan-acceptance');
  });

  it('posts to start the plan', async () => {
    vi.mocked(apiClient.request).mockResolvedValue(undefined);

    await acceptPlan();

    expect(apiClient.request).toHaveBeenCalledWith(
      '/api/v1/plan-acceptance',
      expect.objectContaining({ method: 'POST' }),
    );
  });
});
