import { afterEach, describe, expect, it, vi } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { useAnatomySex } from './useAnatomySex';
import { getProfile } from '../api/profile';

vi.mock('../api/profile', () => ({ getProfile: vi.fn() }));

const getProfileMock = vi.mocked(getProfile);

describe('useAnatomySex', () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it('starts on the male figure so the first paint has a body to draw', () => {
    getProfileMock.mockReturnValue(new Promise(() => {}));

    const { result } = renderHook(() => useAnatomySex());

    expect(result.current).toBe('male');
  });

  it('switches to the female figure for a FEMALE profile', async () => {
    getProfileMock.mockResolvedValue({ sex: 'FEMALE' } as never);

    const { result } = renderHook(() => useAnatomySex());

    await waitFor(() => expect(result.current).toBe('female'));
  });

  it('keeps the male figure for MALE and for the unset/OTHER sexes', async () => {
    getProfileMock.mockResolvedValue({ sex: 'OTHER' } as never);

    const { result } = renderHook(() => useAnatomySex());

    await waitFor(() => expect(getProfileMock).toHaveBeenCalled());
    expect(result.current).toBe('male');
  });

  it('falls back to the male figure when the profile cannot be read', async () => {
    getProfileMock.mockRejectedValue(new Error('offline'));

    const { result } = renderHook(() => useAnatomySex());

    await waitFor(() => expect(getProfileMock).toHaveBeenCalled());
    expect(result.current).toBe('male');
  });
});
