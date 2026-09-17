import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { OnboardingGate } from './OnboardingGate';
import { getProfile, type UserProfile } from '../api/profile';

vi.mock('../api/profile', async () => {
  const actual = await vi.importActual<typeof import('../api/profile')>('../api/profile');
  return { ...actual, getProfile: vi.fn() };
});

const getProfileMock = vi.mocked(getProfile);

const BASE_PROFILE: UserProfile = {
  unitPreferences: { weightUnit: 'KG', heightUnit: 'CM', distanceUnit: 'KM', energyUnit: 'KCAL' },
  themeMode: 'DARK',
  onboardingAnswers: {
    profile: { name: '', birthDate: '', sex: '', heightCm: '' },
    metrics: { measurementSaved: false },
    goal: {},
    training: { days: [] },
    equipment: { items: [] },
    nutrition: { preference: '', restrictions: '' },
  },
  firstRunCompleted: false,
};

function renderGate(initialEntry = '/app') {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <Routes>
        <Route
          path="/app"
          element={
            <>
              <OnboardingGate />
              <div>Panel principal</div>
            </>
          }
        />
        <Route path="/onboarding" element={<div>Configuración inicial</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('OnboardingGate', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('sends a user who has not finished the first run to /onboarding', async () => {
    getProfileMock.mockResolvedValue({ ...BASE_PROFILE, firstRunCompleted: false });

    renderGate();

    expect(await screen.findByText('Configuración inicial')).toBeInTheDocument();
  });

  it('does not redirect a user who already finished the first run', async () => {
    getProfileMock.mockResolvedValue({ ...BASE_PROFILE, firstRunCompleted: true });

    renderGate();

    await waitFor(() => expect(getProfileMock).toHaveBeenCalled());
    expect(screen.getByText('Panel principal')).toBeInTheDocument();
    expect(screen.queryByText('Configuración inicial')).not.toBeInTheDocument();
  });

  it('does not bounce the user while the profile check is still in flight', async () => {
    let resolveProfile: (profile: UserProfile) => void = () => {};
    getProfileMock.mockReturnValue(
      new Promise((resolve) => {
        resolveProfile = resolve;
      }),
    );

    renderGate();

    // Nothing redirected yet — the pending fetch must never bounce the user.
    expect(screen.getByText('Panel principal')).toBeInTheDocument();

    resolveProfile({ ...BASE_PROFILE, firstRunCompleted: false });

    expect(await screen.findByText('Configuración inicial')).toBeInTheDocument();
  });

  it('never redirects while already on /onboarding', async () => {
    getProfileMock.mockResolvedValue({ ...BASE_PROFILE, firstRunCompleted: false });

    render(
      <MemoryRouter initialEntries={['/onboarding']}>
        <Routes>
          <Route
            path="/onboarding"
            element={
              <>
                <OnboardingGate />
                <div>Configuración inicial</div>
              </>
            }
          />
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByText('Configuración inicial')).toBeInTheDocument();
    expect(getProfileMock).not.toHaveBeenCalled();
  });

  /** A broken check must not trap the user — it must fail open, same as PlanActivationGate. */
  it('fails open and leaves the user on /app when the profile check fails', async () => {
    getProfileMock.mockRejectedValue(new Error('network down'));

    renderGate();

    await waitFor(() => expect(getProfileMock).toHaveBeenCalled());
    expect(screen.getByText('Panel principal')).toBeInTheDocument();
    expect(screen.queryByText('Configuración inicial')).not.toBeInTheDocument();
  });
});
