import { act, renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { StrictMode, type ReactNode } from 'react';
import { ApiRequestError } from '../api/client';
import * as authApi from '../api/auth';
import { AuthProvider, useAuth } from './AuthContext';

vi.mock('../api/auth');
const wrapper = ({ children }: { children: ReactNode }) => <AuthProvider>{children}</AuthProvider>;

describe('AuthProvider', () => {
  beforeEach(() => vi.resetAllMocks());

  it('bootstraps authenticated and anonymous sessions', async () => {
    vi.mocked(authApi.getCurrentUser).mockResolvedValue({
      id: '1',
      email: 'user@example.com',
      role: 'USER' as const,
    });
    const first = renderHook(useAuth, { wrapper });
    await waitFor(() => expect(first.result.current.status).toBe('authenticated'));
    first.unmount();

    vi.mocked(authApi.getCurrentUser).mockRejectedValue(new ApiRequestError(401, 'Unauthorized'));
    const second = renderHook(useAuth, { wrapper });
    await waitFor(() => expect(second.result.current.status).toBe('anonymous'));
  });

  it('bootstraps exactly once under React StrictMode', async () => {
    vi.mocked(authApi.getCurrentUser).mockResolvedValue({
      id: '1',
      email: 'user@example.com',
      role: 'USER' as const,
    });
    const strictWrapper = ({ children }: { children: ReactNode }) => (
      <StrictMode>
        <AuthProvider>{children}</AuthProvider>
      </StrictMode>
    );
    const { result } = renderHook(useAuth, { wrapper: strictWrapper });
    await waitFor(() => expect(result.current.status).toBe('authenticated'));
    expect(authApi.getCurrentUser).toHaveBeenCalledTimes(1);
  });

  it('keeps non-401 bootstrap failures retryable', async () => {
    vi.mocked(authApi.getCurrentUser).mockRejectedValueOnce(
      new ApiRequestError(503, 'Unavailable'),
    );
    const { result } = renderHook(useAuth, { wrapper });
    await waitFor(() => expect(result.current.bootstrapError).toBe(true));
    vi.mocked(authApi.getCurrentUser).mockResolvedValue({
      id: '1',
      email: 'user@example.com',
      role: 'USER' as const,
    });
    await act(() => result.current.refreshCurrentUser());
    expect(result.current.status).toBe('authenticated');
  });

  it('updates state for login, register and logout', async () => {
    vi.mocked(authApi.getCurrentUser).mockRejectedValue(new ApiRequestError(401, 'Unauthorized'));
    vi.mocked(authApi.login)
      .mockResolvedValueOnce({ id: '1', email: 'login@example.com', role: 'USER' as const })
      .mockResolvedValueOnce({ id: '2', email: 'new@example.com', role: 'USER' as const });
    vi.mocked(authApi.register).mockResolvedValue({
      id: '2',
      email: 'new@example.com',
      role: 'USER' as const,
    });
    const { result } = renderHook(useAuth, { wrapper });
    await waitFor(() => expect(result.current.status).toBe('anonymous'));
    await act(() => result.current.login({ email: 'login@example.com', password: 'secret123' }));
    expect(result.current.user?.email).toBe('login@example.com');
    await act(() => result.current.register({ email: 'new@example.com', password: 'secret123' }));
    expect(result.current.user?.email).toBe('new@example.com');
    expect(vi.mocked(authApi.register).mock.invocationCallOrder.at(-1)).toBeLessThan(
      vi.mocked(authApi.login).mock.invocationCallOrder.at(-1) ?? 0,
    );
    expect(authApi.login).toHaveBeenLastCalledWith({
      email: 'new@example.com',
      password: 'secret123',
    });
    vi.mocked(authApi.logout).mockResolvedValue();
    await act(() => result.current.logout());
    expect(result.current.status).toBe('anonymous');
  });

  it('ignores a stale bootstrap result after login succeeds', async () => {
    let resolveBootstrap!: (user: authApi.AuthUser) => void;
    vi.mocked(authApi.getCurrentUser).mockReturnValue(
      new Promise((resolve) => {
        resolveBootstrap = resolve;
      }),
    );
    vi.mocked(authApi.login).mockResolvedValue({
      id: 'new',
      email: 'new@example.com',
      role: 'USER' as const,
    });
    const { result } = renderHook(useAuth, { wrapper });

    await act(() => result.current.login({ email: 'new@example.com', password: 'password1234' }));
    expect(result.current.user?.id).toBe('new');
    await act(async () =>
      resolveBootstrap({ id: 'old', email: 'old@example.com', role: 'USER' as const }),
    );
    expect(result.current.user?.id).toBe('new');
  });

  it('keeps the authenticated state when logout fails', async () => {
    vi.mocked(authApi.getCurrentUser).mockResolvedValue({
      id: '1',
      email: 'user@example.com',
      role: 'USER' as const,
    });
    vi.mocked(authApi.logout).mockRejectedValue(new Error('offline'));
    const { result } = renderHook(useAuth, { wrapper });
    await waitFor(() => expect(result.current.status).toBe('authenticated'));
    await expect(act(() => result.current.logout())).rejects.toThrow('offline');
    expect(result.current.status).toBe('authenticated');
    expect(result.current.user?.email).toBe('user@example.com');
  });

  it('allows a pending bootstrap to settle after login fails', async () => {
    let resolveBootstrap!: (user: authApi.AuthUser) => void;
    vi.mocked(authApi.getCurrentUser).mockReturnValue(
      new Promise((resolve) => {
        resolveBootstrap = resolve;
      }),
    );
    vi.mocked(authApi.login).mockRejectedValue(new Error('invalid credentials'));
    const { result } = renderHook(useAuth, { wrapper });

    await expect(
      result.current.login({ email: 'user@example.com', password: 'password1234' }),
    ).rejects.toThrow('invalid credentials');
    await act(async () =>
      resolveBootstrap({ id: 'existing', email: 'existing@example.com', role: 'USER' as const }),
    );

    expect(result.current.status).toBe('authenticated');
    expect(result.current.user?.id).toBe('existing');
  });

  it('allows a pending bootstrap to settle after register login fails', async () => {
    let resolveBootstrap!: (user: authApi.AuthUser) => void;
    vi.mocked(authApi.getCurrentUser).mockReturnValue(
      new Promise((resolve) => {
        resolveBootstrap = resolve;
      }),
    );
    vi.mocked(authApi.register).mockResolvedValue({
      id: 'created',
      email: 'new@example.com',
      role: 'USER' as const,
    });
    vi.mocked(authApi.login).mockRejectedValue(new Error('login unavailable'));
    const { result } = renderHook(useAuth, { wrapper });

    await expect(
      result.current.register({ email: 'new@example.com', password: 'password1234' }),
    ).rejects.toThrow('login unavailable');
    await act(async () =>
      resolveBootstrap({ id: 'existing', email: 'existing@example.com', role: 'USER' as const }),
    );

    expect(result.current.status).toBe('authenticated');
    expect(result.current.user?.id).toBe('existing');
  });
});
