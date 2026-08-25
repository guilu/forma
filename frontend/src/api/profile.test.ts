import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  getProfile,
  submitOnboardingAnswers,
  updateProfileFields,
  updateThemeMode,
} from './profile';
import { apiClient, type ApiClient } from './client';

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
    const client: ApiClient = { baseUrl: 'http://test', request, requestBlob: vi.fn() };

    const result = await getProfile(client);

    expect(request).toHaveBeenCalledWith('/api/v1/profile');
    expect(result).toBe(profile);
  });

  it('PATCHes the profile fields section', async () => {
    const updated = { name: 'Ada Lovelace' };
    const request = vi.fn().mockResolvedValue(updated);
    const client: ApiClient = { baseUrl: 'http://test', request, requestBlob: vi.fn() };

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
    const client: ApiClient = { baseUrl: 'http://test', request, requestBlob: vi.fn() };

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
    const client: ApiClient = { baseUrl: 'http://test', request, requestBlob: vi.fn() };

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
    const client: ApiClient = { baseUrl: 'http://test', request, requestBlob: vi.fn() };

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
    const client: ApiClient = { baseUrl: 'http://test', request, requestBlob: vi.fn() };

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

  /*
   * Tres cosas distintas piden el perfil al abrir el panel —el tema, el sexo para las
   * siluetas y el nombre del saludo— y salían tres peticiones. El panel dispara una decena
   * de llamadas de golpe y hay un limitador delante en producción: las sobrantes convertían
   * una ráfaga que cabía en una que no.
   */
  describe('cuando ya hay una petición en vuelo', () => {
    afterEach(() => {
      vi.restoreAllMocks();
    });

    it('la comparte en vez de lanzar otra', async () => {
      const request = vi.spyOn(apiClient, 'request').mockResolvedValue({ name: 'Ada' });

      const [uno, dos, tres] = await Promise.all([getProfile(), getProfile(), getProfile()]);

      expect(request).toHaveBeenCalledTimes(1);
      expect(uno).toBe(dos);
      expect(dos).toBe(tres);
    });

    /** No es una caché: si lo fuera, habría que invalidarla en cada guardado del perfil. */
    it('vuelve a preguntar en cuanto la anterior ha respondido', async () => {
      const request = vi.spyOn(apiClient, 'request').mockResolvedValue({ name: 'Ada' });

      await getProfile();
      await getProfile();

      expect(request).toHaveBeenCalledTimes(2);
    });

    /** Un fallo no puede dejar el hueco ocupado: la siguiente pantalla se quedaría sin perfil. */
    it('suelta el hueco cuando la petición falla', async () => {
      const request = vi
        .spyOn(apiClient, 'request')
        .mockRejectedValueOnce(new Error('red'))
        .mockResolvedValueOnce({ name: 'Ada' });

      await expect(getProfile()).rejects.toThrow('red');
      await expect(getProfile()).resolves.toEqual({ name: 'Ada' });
      expect(request).toHaveBeenCalledTimes(2);
    });

    /** El cliente inyectado se queda fuera: una prueba no puede heredar la respuesta de otra. */
    it('no funde las peticiones de un cliente inyectado', async () => {
      vi.spyOn(apiClient, 'request').mockResolvedValue({ name: 'Ada' });
      const request = vi.fn().mockResolvedValue({ name: 'Grace' });
      const client: ApiClient = { baseUrl: 'http://test', request, requestBlob: vi.fn() };

      await Promise.all([getProfile(client), getProfile(client)]);

      expect(request).toHaveBeenCalledTimes(2);
    });
  });
});
