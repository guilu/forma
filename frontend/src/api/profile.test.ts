import { describe, expect, it, vi } from 'vitest';
import {
  getProfile,
  submitOnboardingAnswers,
  updateProfileFields,
  updateThemeMode,
} from './profile';
import { type ApiClient } from './client';

describe('profile API', () => {
  it('GETs the profile & preferences aggregate', async () => {
    const profile = {
      name: 'Ada Lovelace',
      email: 'ada@forma.app',
      unitPreferences: {
        weightUnit: 'KG',
        heightUnit: 'CM',
        distanceUnit: 'KM',
        energyUnit: 'KCAL',
      },
    };
    const request = vi.fn().mockResolvedValue(profile);
    const client: ApiClient = { baseUrl: 'http://test', request };

    const result = await getProfile(client);

    expect(request).toHaveBeenCalledWith('/api/v1/profile');
    expect(result).toBe(profile);
  });

  it('PATCHes the profile fields section', async () => {
    const updated = { name: 'Ada Lovelace' };
    const request = vi.fn().mockResolvedValue(updated);
    const client: ApiClient = { baseUrl: 'http://test', request };

    const result = await updateProfileFields(
      { name: 'Ada Lovelace', heightCm: 170, sex: 'FEMALE' },
      client,
    );

    expect(request).toHaveBeenCalledWith('/api/v1/profile', {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: 'Ada Lovelace', heightCm: 170, sex: 'FEMALE' }),
    });
    expect(result).toBe(updated);
  });

  it('omits undefined fields from the PATCH body (partial update contract, FOR-107)', async () => {
    const request = vi.fn().mockResolvedValue({});
    const client: ApiClient = { baseUrl: 'http://test', request };

    await updateProfileFields({ name: 'Ada Lovelace' }, client);

    expect(request).toHaveBeenCalledWith('/api/v1/profile', {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: 'Ada Lovelace' }),
    });
  });

  it('PATCHes the theme preference (FOR-120)', async () => {
    const updated = { themeMode: 'LIGHT' };
    const request = vi.fn().mockResolvedValue(updated);
    const client: ApiClient = { baseUrl: 'http://test', request };

    const result = await updateThemeMode({ themeMode: 'LIGHT' }, client);

    expect(request).toHaveBeenCalledWith('/api/v1/profile/theme', {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ themeMode: 'LIGHT' }),
    });
    expect(result).toBe(updated);
  });

  it('PATCHes the onboarding answers + completed flag (FOR-121)', async () => {
    const updated = { firstRunCompleted: true };
    const request = vi.fn().mockResolvedValue(updated);
    const client: ApiClient = { baseUrl: 'http://test', request };

    const result = await submitOnboardingAnswers(
      {
        profile: { name: 'Ada', birthDate: '1990-01-01', sex: 'FEMALE', heightCm: '170' },
        goal: { selected: 'COMPOSICION' },
        completed: true,
      },
      client,
    );

    expect(request).toHaveBeenCalledWith('/api/v1/profile/onboarding', {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        profile: { name: 'Ada', birthDate: '1990-01-01', sex: 'FEMALE', heightCm: '170' },
        goal: { selected: 'COMPOSICION' },
        completed: true,
      }),
    });
    expect(result).toBe(updated);
  });

  it('sends onboarding values verbatim, no enum-casing transform (verified against SubmitOnboardingAnswersRequest.toDomain — raw strings, not backend-enum-coerced)', async () => {
    const request = vi.fn().mockResolvedValue({});
    const client: ApiClient = { baseUrl: 'http://test', request };

    await submitOnboardingAnswers(
      { metrics: { choice: 'MANUAL', measurementSaved: false }, completed: false },
      client,
    );

    expect(request).toHaveBeenCalledWith(
      '/api/v1/profile/onboarding',
      expect.objectContaining({
        body: JSON.stringify({
          metrics: { choice: 'MANUAL', measurementSaved: false },
          completed: false,
        }),
      }),
    );
  });
});
