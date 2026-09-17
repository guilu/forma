import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Link, MemoryRouter, Outlet, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { OnboardingGate } from './OnboardingGate';
import { getProfile, type UserProfile } from '../api/profile';
import { baseProfile } from '../test/profileFixtures';

vi.mock('../api/profile', async () => {
  const actual = await vi.importActual<typeof import('../api/profile')>('../api/profile');
  return { ...actual, getProfile: vi.fn() };
});

const getProfileMock = vi.mocked(getProfile);

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
    getProfileMock.mockResolvedValue(baseProfile({ firstRunCompleted: false }));

    renderGate();

    expect(await screen.findByText('Configuración inicial')).toBeInTheDocument();
  });

  it('does not redirect a user who already finished the first run', async () => {
    getProfileMock.mockResolvedValue(baseProfile({ firstRunCompleted: true }));

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

    resolveProfile(baseProfile({ firstRunCompleted: false }));

    expect(await screen.findByText('Configuración inicial')).toBeInTheDocument();
  });

  it('never redirects while already on /onboarding', async () => {
    getProfileMock.mockResolvedValue(baseProfile({ firstRunCompleted: false }));

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

  /**
   * AppShell mounts OnboardingGate once per session, not once per route — the
   * check must run once on mount, not again on every in-app navigation
   * (`/app` -> `/app/nutrition` -> ...). This mirrors AppShell's real
   * structure: the gate and a persistent shell sit above an `<Outlet />`
   * that swaps only the nested child route.
   */
  it('checks the profile once on mount, not again on every in-app navigation', async () => {
    getProfileMock.mockResolvedValue(baseProfile({ firstRunCompleted: true }));
    const user = userEvent.setup();

    function Shell() {
      return (
        <>
          <OnboardingGate />
          <Link to="/app/nutrition">Ir a nutrición</Link>
          <Outlet />
        </>
      );
    }

    render(
      <MemoryRouter initialEntries={['/app']}>
        <Routes>
          <Route path="/app" element={<Shell />}>
            <Route index element={<div>Panel principal</div>} />
            <Route path="nutrition" element={<div>Vista de nutrición</div>} />
          </Route>
          <Route path="/onboarding" element={<div>Configuración inicial</div>} />
        </Routes>
      </MemoryRouter>,
    );

    await waitFor(() => expect(getProfileMock).toHaveBeenCalledTimes(1));

    await user.click(screen.getByRole('link', { name: 'Ir a nutrición' }));

    expect(await screen.findByText('Vista de nutrición')).toBeInTheDocument();
    expect(getProfileMock).toHaveBeenCalledTimes(1);
  });
});
