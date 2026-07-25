import { beforeEach, describe, expect, it, vi } from 'vitest';
import { apiClient } from './client';
import { getCurrentUser, login, logout, register } from './auth';

vi.mock('./client', () => ({
  apiClient: { request: vi.fn() },
}));

describe('auth API', () => {
  beforeEach(() => vi.mocked(apiClient.request).mockReset());

  it.each([
    ['login', login, '/api/v1/auth/login'],
    ['register', register, '/api/v1/auth/register'],
  ] as const)('%s posts credentials', async (_, action, path) => {
    vi.mocked(apiClient.request).mockResolvedValue({ id: '1', email: 'user@example.com' });
    await action({ email: 'user@example.com', password: 'secret123' });
    expect(apiClient.request).toHaveBeenCalledWith(
      path,
      expect.objectContaining({ method: 'POST' }),
    );
  });

  it('reads the current user and logs out', async () => {
    vi.mocked(apiClient.request)
      .mockResolvedValueOnce({ id: '1', email: 'user@example.com' })
      .mockResolvedValueOnce(undefined);
    await getCurrentUser();
    await logout();
    expect(apiClient.request).toHaveBeenNthCalledWith(1, '/api/v1/auth/me');
    expect(apiClient.request).toHaveBeenNthCalledWith(
      2,
      '/api/v1/auth/logout',
      expect.objectContaining({ method: 'POST' }),
    );
  });
});
