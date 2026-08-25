import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
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
   * Tres cosas distintas piden el perfil al abrir la aplicación —el tema, el sexo para las
   * siluetas y el nombre del saludo— y salían dos peticiones. El panel dispara una decena de
   * llamadas de golpe y hay un limitador delante en producción: las sobrantes convertían una
   * ráfaga que cabía en una que no.
   */
  describe('la copia compartida del perfil', () => {
    /*
     * Cada prueba empieza un minuto después de donde acabó la anterior. La copia vive en el
     * módulo y le sobrevive, así que sin esto la que deja una prueba seguiría fresca en la
     * siguiente y varias pasarían sin pedir nada.
     *
     * <p>El punto de partida se lee al TERMINAR y no se acumula a ciegas: hay pruebas que
     * adelantan el reloj por dentro —la de la caducidad, 31 s— y sumar un minuto al valor de
     * antes dejaba a la siguiente arrancando dentro de la ventana que quería evitar.
     */
    let reloj = Date.now();

    beforeEach(() => {
      vi.useFakeTimers();
      reloj += 60_000;
      vi.setSystemTime(reloj);
    });

    afterEach(() => {
      reloj = Date.now();
      vi.useRealTimers();
      vi.restoreAllMocks();
    });

    /** Las que salen a la vez: comparten la petición en vuelo. */
    it('funde las peticiones simultáneas en una', async () => {
      const request = vi.spyOn(apiClient, 'request').mockResolvedValue({ name: 'Ada' });

      const [uno, dos, tres] = await Promise.all([getProfile(), getProfile(), getProfile()]);

      expect(request).toHaveBeenCalledTimes(1);
      expect(uno).toBe(dos);
      expect(dos).toBe(tres);
    });

    /*
     * EL CASO QUE IMPORTA, y el que se escapó al arreglo anterior. `ThemeProvider` pide el
     * perfil nada más montar; la página va en un trozo cargado con `lazy` y pide el suyo
     * cuando ese trozo llega, segundos después. Para entonces la primera ya respondió, así
     * que fundir solo las simultáneas no juntaba nada — en producción seguían saliendo dos.
     */
    it('reutiliza la copia cuando la segunda llega después de responder la primera', async () => {
      const request = vi.spyOn(apiClient, 'request').mockResolvedValue({ name: 'Ada' });

      const primera = await getProfile();
      vi.advanceTimersByTime(5_000);
      const segunda = await getProfile();

      expect(request).toHaveBeenCalledTimes(1);
      expect(segunda).toBe(primera);
    });

    /** Pero caduca: otra pestaña puede haber tocado el perfil, y esto no se entera. */
    it('vuelve a preguntar cuando la copia ha caducado', async () => {
      const request = vi.spyOn(apiClient, 'request').mockResolvedValue({ name: 'Ada' });

      await getProfile();
      vi.advanceTimersByTime(31_000);
      await getProfile();

      expect(request).toHaveBeenCalledTimes(2);
    });

    /** Un guardado no invalida la copia: la sustituye por lo que acaba de devolver el servidor. */
    it('deja la copia al día tras guardar, sin un viaje de más', async () => {
      const request = vi
        .spyOn(apiClient, 'request')
        .mockResolvedValueOnce({ name: 'Ada' })
        .mockResolvedValueOnce({ name: 'Grace' });

      await getProfile();
      await updateProfileFields({ name: 'Grace' });

      expect(await getProfile()).toEqual({ name: 'Grace' });
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
    it('no comparte nada con un cliente inyectado', async () => {
      vi.spyOn(apiClient, 'request').mockResolvedValue({ name: 'Ada' });
      const request = vi.fn().mockResolvedValue({ name: 'Grace' });
      const client: ApiClient = { baseUrl: 'http://test', request, requestBlob: vi.fn() };

      await getProfile(client);
      await getProfile(client);

      expect(request).toHaveBeenCalledTimes(2);
    });
  });
});
