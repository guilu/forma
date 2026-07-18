import { describe, expect, it, vi, beforeEach } from 'vitest';
import type { ApiClient } from '../../api/client';
import type { UserProfile } from '../../api/profile';
import {
  clearOnboardingProgress,
  fetchOnboardingBackendState,
  fromOnboardingAnswersOutput,
  hasOnboardingProgress,
  loadOnboardingProgress,
  saveOnboardingProgress,
  syncOnboardingProgress,
  toOnboardingAnswersInput,
  INITIAL_PROGRESS,
  EMPTY_ANSWERS,
  type OnboardingProgress,
} from './onboardingStorage';

const EMPTY_ONBOARDING_ANSWERS_OUTPUT = {
  profile: { name: '', birthDate: '', sex: '', heightCm: '' },
  metrics: { measurementSaved: false },
  goal: {},
  training: { days: [] },
  equipment: { items: [] },
  nutrition: { preference: '', restrictions: '' },
};

describe('onboardingStorage', () => {
  beforeEach(() => {
    window.localStorage.clear();
  });

  it('returns the initial progress when nothing is stored', () => {
    expect(loadOnboardingProgress()).toEqual(INITIAL_PROGRESS);
  });

  it('round-trips a saved progress record', () => {
    const progress: OnboardingProgress = {
      stepIndex: 3,
      completed: false,
      answers: {
        ...INITIAL_PROGRESS.answers,
        profile: { name: 'Diego', birthDate: '', sex: '', heightCm: '' },
        goal: { selected: 'HABITO' },
      },
    };

    saveOnboardingProgress(progress);

    expect(loadOnboardingProgress()).toEqual(progress);
  });

  it('falls back to defaults when the stored value is corrupted JSON', () => {
    window.localStorage.setItem('forma.onboarding.v1', '{not-json');

    expect(loadOnboardingProgress()).toEqual(INITIAL_PROGRESS);
  });

  it('fills in missing fields from a partial/older-shaped record', () => {
    window.localStorage.setItem(
      'forma.onboarding.v1',
      JSON.stringify({ stepIndex: 2, answers: { profile: { name: 'Ana' } } }),
    );

    const progress = loadOnboardingProgress();

    expect(progress.stepIndex).toBe(2);
    expect(progress.completed).toBe(false);
    expect(progress.answers.profile.name).toBe('Ana');
    expect(progress.answers.profile.birthDate).toBe('');
    expect(progress.answers.goal).toEqual({ selected: undefined });
  });

  it('clears the stored progress', () => {
    saveOnboardingProgress({ ...INITIAL_PROGRESS, stepIndex: 5 });

    clearOnboardingProgress();

    expect(loadOnboardingProgress()).toEqual(INITIAL_PROGRESS);
  });
});

describe('hasOnboardingProgress', () => {
  it('is false for the empty answers', () => {
    expect(hasOnboardingProgress(EMPTY_ANSWERS)).toBe(false);
  });

  it('is true once any group has a real value', () => {
    expect(
      hasOnboardingProgress({
        ...EMPTY_ANSWERS,
        profile: { ...EMPTY_ANSWERS.profile, name: 'Ana' },
      }),
    ).toBe(true);
    expect(hasOnboardingProgress({ ...EMPTY_ANSWERS, training: { days: ['Lunes'] } })).toBe(true);
  });
});

describe('toOnboardingAnswersInput (FOR-121 — the swap onboardingStorage.ts anticipated)', () => {
  it('maps the local draft 1:1 onto the backend PATCH payload, values verbatim (no casing transform)', () => {
    const answers = {
      ...EMPTY_ANSWERS,
      profile: { name: 'Diego', birthDate: '1990-01-01', sex: 'MALE', heightCm: '180' },
      goal: { selected: 'HABITO' as const },
    };

    expect(toOnboardingAnswersInput(answers, true)).toEqual({
      profile: { name: 'Diego', birthDate: '1990-01-01', sex: 'MALE', heightCm: '180' },
      metrics: { choice: undefined, measurementSaved: false },
      goal: { selected: 'HABITO' },
      training: { days: [] },
      equipment: { items: [] },
      nutrition: { preference: '', restrictions: '' },
      completed: true,
    });
  });
});

describe('fromOnboardingAnswersOutput', () => {
  it('maps the backend read model back onto the local draft shape', () => {
    const output = {
      profile: { name: 'Ada', birthDate: '', sex: '', heightCm: '' },
      metrics: { choice: 'MANUAL', measurementSaved: true },
      goal: { selected: 'COMPOSICION' },
      training: { days: ['Lunes'] },
      equipment: { items: ['Mancuernas'] },
      nutrition: { preference: 'VEGAN', restrictions: 'Frutos secos' },
    };

    expect(fromOnboardingAnswersOutput(output)).toEqual({
      profile: { name: 'Ada', birthDate: '', sex: '', heightCm: '' },
      metrics: { choice: 'MANUAL', measurementSaved: true },
      goal: { selected: 'COMPOSICION' },
      training: { days: ['Lunes'] },
      equipment: { items: ['Mancuernas'] },
      nutrition: { preference: 'VEGAN', restrictions: 'Frutos secos' },
    });
  });

  it('falls back to undefined for an unrecognized choice/goal value rather than trusting an unvalidated backend string', () => {
    const output = {
      ...EMPTY_ONBOARDING_ANSWERS_OUTPUT,
      metrics: { choice: 'bogus', measurementSaved: false },
    };

    expect(fromOnboardingAnswersOutput(output).metrics.choice).toBeUndefined();
  });
});

describe('syncOnboardingProgress (FOR-121)', () => {
  it('resolves true and submits the mapped payload on success', async () => {
    const request = vi.fn().mockResolvedValue({ firstRunCompleted: false });
    const client: ApiClient = { baseUrl: 'http://test', request, requestBlob: vi.fn() };
    const progress: OnboardingProgress = { ...INITIAL_PROGRESS, completed: false };

    const ok = await syncOnboardingProgress(progress, client);

    expect(ok).toBe(true);
    expect(request).toHaveBeenCalledWith(
      '/api/v1/profile/onboarding',
      expect.objectContaining({ method: 'PATCH' }),
    );
  });

  it('resolves false (never throws) when the backend call fails — a save failure must not block the flow', async () => {
    const request = vi.fn().mockRejectedValue(new Error('network down'));
    const client: ApiClient = { baseUrl: 'http://test', request, requestBlob: vi.fn() };

    const ok = await syncOnboardingProgress(INITIAL_PROGRESS, client);

    expect(ok).toBe(false);
  });
});

describe('fetchOnboardingBackendState (FOR-121)', () => {
  it('returns firstRunCompleted + recovered answers on success', async () => {
    const profile: UserProfile = {
      unitPreferences: {
        weightUnit: 'KG',
        heightUnit: 'CM',
        distanceUnit: 'KM',
        energyUnit: 'KCAL',
      },
      themeMode: 'DARK',
      onboardingAnswers: {
        ...EMPTY_ONBOARDING_ANSWERS_OUTPUT,
        profile: { name: 'Diego', birthDate: '', sex: '', heightCm: '' },
      },
      firstRunCompleted: true,
    };
    const request = vi.fn().mockResolvedValue(profile);
    const client: ApiClient = { baseUrl: 'http://test', request, requestBlob: vi.fn() };

    const state = await fetchOnboardingBackendState(client);

    expect(state).toEqual({
      firstRunCompleted: true,
      answers: {
        ...EMPTY_ANSWERS,
        profile: { name: 'Diego', birthDate: '', sex: '', heightCm: '' },
      },
    });
  });

  it('returns undefined when the backend is unreachable — graceful fallback, never throws', async () => {
    const request = vi.fn().mockRejectedValue(new Error('network down'));
    const client: ApiClient = { baseUrl: 'http://test', request, requestBlob: vi.fn() };

    const state = await fetchOnboardingBackendState(client);

    expect(state).toBeUndefined();
  });
});
