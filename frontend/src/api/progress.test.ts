import { describe, expect, it, vi } from 'vitest';
import {
  deleteProgressPhoto,
  fetchProgressPhotoBlob,
  getStreak,
  getWeeklyHistory,
  listProgressPhotos,
  uploadProgressPhoto,
} from './progress';
import { type ApiClient } from './client';

/**
 * Progress API-module tests (FOR-143). Verifies the request is built on the
 * shared client against the FOR-139 contract paths
 * (`backend/.../delivery/progress/ProgressController.java`). No real network
 * is used.
 */
describe('getStreak', () => {
  it('GETs the streak endpoint and returns the result', async () => {
    const streak = { currentStreakDays: 6, longestStreakDays: 21, asOf: '2026-07-18' };
    const request = vi.fn().mockResolvedValue(streak);
    const client: ApiClient = { baseUrl: 'http://test', request, requestBlob: vi.fn() };

    const result = await getStreak(client);

    expect(request).toHaveBeenCalledWith('/api/v1/progress/streak');
    expect(result).toBe(streak);
  });
});

describe('getWeeklyHistory', () => {
  it('GETs the weekly-history endpoint and returns the result', async () => {
    const history = {
      weeks: [
        { weekStart: '2026-05-25', planned: 7, completed: 5 },
        { weekStart: '2026-06-01', planned: 7, completed: 7 },
      ],
    };
    const request = vi.fn().mockResolvedValue(history);
    const client: ApiClient = { baseUrl: 'http://test', request, requestBlob: vi.fn() };

    const result = await getWeeklyHistory(client);

    expect(request).toHaveBeenCalledWith('/api/v1/progress/weekly-history');
    expect(result).toBe(history);
  });
});

/**
 * Progress-photos API tests (FOR-144, consuming the FOR-140 backend —
 * `ProgressPhotoController`/`ProgressPhotoResponse`/`ProgressPhotoListResponse`,
 * verified directly against `backend/.../delivery/progress/*`). No real
 * network or binary I/O is used; `requestBlob` is a separate client method
 * mocked independently from the JSON `request` method.
 */
function fakeClient(overrides: Partial<ApiClient> = {}): ApiClient {
  return {
    baseUrl: 'http://test',
    request: vi.fn(),
    requestBlob: vi.fn(),
    ...overrides,
  };
}

describe('listProgressPhotos', () => {
  it('GETs the photos endpoint and returns the metadata array', async () => {
    const photos = [
      { id: 'p1', contentType: 'image/jpeg', sizeBytes: 20480, createdAt: '2026-07-18T10:00:00Z' },
    ];
    const request = vi.fn().mockResolvedValue({ photos });
    const client = fakeClient({ request });

    const result = await listProgressPhotos(client);

    expect(request).toHaveBeenCalledWith('/api/v1/progress/photos');
    expect(result).toBe(photos);
  });

  it('resolves an empty array when the owner has no photos yet (normal, not an error)', async () => {
    const request = vi.fn().mockResolvedValue({ photos: [] });
    const client = fakeClient({ request });

    const result = await listProgressPhotos(client);

    expect(result).toEqual([]);
  });
});

describe('uploadProgressPhoto', () => {
  it('POSTs the file as multipart/form-data under the "file" part and returns the created metadata', async () => {
    const created = {
      id: 'p2',
      contentType: 'image/png',
      sizeBytes: 1024,
      createdAt: '2026-07-18T11:00:00Z',
    };
    const request = vi.fn().mockResolvedValue(created);
    const client = fakeClient({ request });
    const file = new File(['fake-bytes'], 'photo.png', { type: 'image/png' });

    const result = await uploadProgressPhoto(file, client);

    expect(request).toHaveBeenCalledTimes(1);
    const [path, init] = request.mock.calls[0];
    expect(path).toBe('/api/v1/progress/photos');
    expect(init.method).toBe('POST');
    expect(init.body).toBeInstanceOf(FormData);
    expect((init.body as FormData).get('file')).toBe(file);
    // No explicit Content-Type header: the browser must set the multipart
    // boundary itself, matching FormData bodies (never hardcode it).
    expect(init.headers).toBeUndefined();
    expect(result).toBe(created);
  });

  it('propagates ApiRequestError (e.g. 400 VALIDATION_ERROR for oversize/wrong type) from the client', async () => {
    const { ApiRequestError } = await import('./client');
    const request = vi
      .fn()
      .mockRejectedValue(new ApiRequestError(400, 'El archivo supera 5 MB', 'VALIDATION_ERROR'));
    const client = fakeClient({ request });
    const file = new File(['x'], 'big.jpg', { type: 'image/jpeg' });

    await expect(uploadProgressPhoto(file, client)).rejects.toMatchObject({
      code: 'VALIDATION_ERROR',
      message: 'El archivo supera 5 MB',
    });
  });
});

describe('deleteProgressPhoto', () => {
  it('DELETEs the photo by id', async () => {
    const request = vi.fn().mockResolvedValue(undefined);
    const client = fakeClient({ request });

    await deleteProgressPhoto('p1', client);

    expect(request).toHaveBeenCalledWith('/api/v1/progress/photos/p1', { method: 'DELETE' });
  });
});

describe('fetchProgressPhotoBlob', () => {
  it('fetches the owner-scoped binary endpoint as a Blob via the client boundary', async () => {
    const blob = new Blob(['fake-bytes'], { type: 'image/jpeg' });
    const requestBlob = vi.fn().mockResolvedValue(blob);
    const client = fakeClient({ requestBlob });

    const result = await fetchProgressPhotoBlob('p1', client);

    expect(requestBlob).toHaveBeenCalledWith('/api/v1/progress/photos/p1');
    expect(result).toBe(blob);
  });
});
