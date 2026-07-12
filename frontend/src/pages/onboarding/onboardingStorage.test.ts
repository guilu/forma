import { describe, expect, it, beforeEach } from 'vitest';
import {
  clearOnboardingProgress,
  loadOnboardingProgress,
  saveOnboardingProgress,
  INITIAL_PROGRESS,
  type OnboardingProgress,
} from './onboardingStorage';

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
