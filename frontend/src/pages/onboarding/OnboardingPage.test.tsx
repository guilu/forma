import { describe, expect, it, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { OnboardingPage } from './OnboardingPage';
import { saveOnboardingProgress, INITIAL_PROGRESS } from './onboardingStorage';
import { NotificationProvider } from '../../components/NotificationProvider';
import { getProfile, submitOnboardingAnswers, type UserProfile } from '../../api/profile';
import { createPlanRequest, getCurrentPlanRequest, type PlanRequest } from '../../api/planRequests';
import { ApiRequestError } from '../../api/client';
import { axe } from '../../test/axe';
import { baseProfile } from '../../test/profileFixtures';

/**
 * Covers `specs/FOR-59/tests.md` UI Tests: ordered steps with progress,
 * next/back navigation, skip past non-critical steps, validation blocking
 * advance, completion routing to the dashboard, and resume restoring saved
 * progress. FOR-121 (`specs/FOR-121/tests.md`) extends this with backend
 * persistence + the backend-sourced first-run gate — see the dedicated
 * describe block below.
 */
vi.mock('../../api/profile', async () => {
  const actual = await vi.importActual<typeof import('../../api/profile')>('../../api/profile');
  return { ...actual, getProfile: vi.fn(), submitOnboardingAnswers: vi.fn() };
});

vi.mock('../../api/planRequests', async () => {
  const actual =
    await vi.importActual<typeof import('../../api/planRequests')>('../../api/planRequests');
  return { ...actual, createPlanRequest: vi.fn(), getCurrentPlanRequest: vi.fn() };
});

const getProfileMock = vi.mocked(getProfile);
const submitOnboardingAnswersMock = vi.mocked(submitOnboardingAnswers);
const createPlanRequestMock = vi.mocked(createPlanRequest);
const getCurrentPlanRequestMock = vi.mocked(getCurrentPlanRequest);

/** Default fixture: fresh/first-run profile (FOR-107 Edge Cases default), never completed. */
const FRESH_PROFILE: UserProfile = baseProfile();

const PLAN_REQUEST: PlanRequest = {
  id: 'r1',
  status: 'PENDING',
  mainGoal: 'COMPOSICION',
  planObjective: 'WEIGHT_LOSS',
  planKcal: 2078,
  trainingDaysPerWeek: 0,
  trainingWeekdays: [],
  equipment: [],
  mealsPerDay: 5,
  dietPattern: 'UNSPECIFIED',
  cuisineStyle: 'UNSPECIFIED',
  requestedAt: '2026-09-18T10:00:00Z',
};

function renderOnboarding() {
  return render(
    <MemoryRouter initialEntries={['/onboarding']}>
      <NotificationProvider>
        <Routes>
          <Route path="/onboarding" element={<OnboardingPage />} />
          <Route path="/app" element={<div>Panel principal</div>} />
          <Route path="/app/settings" element={<div>Ajustes</div>} />
          <Route path="/app/measurements" element={<div>Mediciones</div>} />
        </Routes>
      </NotificationProvider>
    </MemoryRouter>,
  );
}

async function fillName(user: ReturnType<typeof userEvent.setup>, name: string) {
  await user.type(screen.getByLabelText('Nombre'), name);
}

/**
 * Walks from the profile step to the completion screen: fills the one
 * required field, advances past it, then skips every remaining optional
 * step — including direction, so `answers.direction.selected` stays
 * `undefined` and no plan-request is ever sent by this helper. Shared by
 * every test whose interest starts *at* the completion screen rather than in
 * getting there — each keeps its own assertions (including the "Todo listo"
 * heading check), only the navigation moves.
 */
async function goToCompletionScreen(user: ReturnType<typeof userEvent.setup>) {
  await fillName(user, 'Diego');
  await user.click(screen.getByRole('button', { name: 'Siguiente' })); // metrics -> goal
  for (let i = 0; i < 7; i += 1) {
    await user.click(screen.getByRole('button', { name: 'Omitir este paso' }));
  }
}

/**
 * Walks all the way to the completion screen with a direction chosen — the
 * one answer `buildPlanRequestInput` requires — so the plan-request
 * submission (ADR-015 slice 3) actually fires.
 */
async function goToCompletionScreenWithDirection(
  user: ReturnType<typeof userEvent.setup>,
  direction: 'Perder grasa' | 'Ganar músculo' | 'Mantenerme' = 'Perder grasa',
) {
  await fillName(user, 'Diego');
  await user.click(screen.getByRole('button', { name: 'Siguiente' })); // profile -> metrics
  await user.click(screen.getByRole('button', { name: 'Omitir este paso' })); // metrics -> goal
  await user.click(screen.getByRole('button', { name: 'Omitir este paso' })); // goal -> direction
  await user.click(screen.getByRole('radio', { name: new RegExp(direction) }));
  await user.click(screen.getByRole('button', { name: 'Siguiente' })); // direction -> training
  for (let i = 0; i < 4; i += 1) {
    await user.click(screen.getByRole('button', { name: 'Omitir este paso' }));
  }
}

describe('OnboardingPage', () => {
  beforeEach(() => {
    window.localStorage.clear();
    vi.clearAllMocks();
    getProfileMock.mockResolvedValue(FRESH_PROFILE);
    submitOnboardingAnswersMock.mockResolvedValue(FRESH_PROFILE);
    createPlanRequestMock.mockResolvedValue(PLAN_REQUEST);
    getCurrentPlanRequestMock.mockResolvedValue(PLAN_REQUEST);
  });

  it('renders steps in order with progress indication as the user advances', async () => {
    const user = userEvent.setup();
    renderOnboarding();

    expect(screen.getByRole('heading', { name: 'Perfil' })).toBeInTheDocument();
    expect(screen.getByText('Paso 1 de 8')).toBeInTheDocument();

    await fillName(user, 'Diego');
    await user.click(screen.getByRole('button', { name: 'Siguiente' }));
    expect(screen.getByRole('heading', { name: 'Métricas actuales' })).toBeInTheDocument();
    expect(screen.getByText('Paso 2 de 8')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Omitir este paso' }));
    expect(screen.getByRole('heading', { name: 'Objetivo' })).toBeInTheDocument();
    expect(screen.getByText('Paso 3 de 8')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Omitir este paso' }));
    expect(screen.getByRole('heading', { name: 'Dirección del plan' })).toBeInTheDocument();
    expect(screen.getByText('Paso 4 de 8')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Omitir este paso' }));
    expect(
      screen.getByRole('heading', { name: 'Disponibilidad de entrenamiento' }),
    ).toBeInTheDocument();
    expect(screen.getByText('Paso 5 de 8')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Omitir este paso' }));
    expect(screen.getByRole('heading', { name: 'Equipamiento' })).toBeInTheDocument();
    expect(screen.getByText('Paso 6 de 8')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Omitir este paso' }));
    expect(screen.getByRole('heading', { name: 'Preferencias de nutrición' })).toBeInTheDocument();
    expect(screen.getByText('Paso 7 de 8')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Omitir este paso' }));
    expect(screen.getByRole('heading', { name: 'Conectar integración' })).toBeInTheDocument();
    expect(screen.getByText('Paso 8 de 8')).toBeInTheDocument();
  });

  it('navigates back while preserving previously entered answers', async () => {
    const user = userEvent.setup();
    renderOnboarding();

    await fillName(user, 'Diego');
    await user.click(screen.getByRole('button', { name: 'Siguiente' }));
    expect(screen.getByRole('heading', { name: 'Métricas actuales' })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Atrás' }));

    expect(screen.getByRole('heading', { name: 'Perfil' })).toBeInTheDocument();
    expect(screen.getByLabelText('Nombre')).toHaveValue('Diego');
  });

  it('does not offer a skip action on the critical profile step', async () => {
    renderOnboarding();
    await waitFor(() => expect(getProfileMock).toHaveBeenCalled());

    expect(screen.queryByRole('button', { name: 'Omitir este paso' })).not.toBeInTheDocument();
  });

  it('blocks advancing on the profile step with an empty name and shows a clear error', async () => {
    const user = userEvent.setup();
    renderOnboarding();

    await user.click(screen.getByRole('button', { name: 'Siguiente' }));

    expect(screen.getByRole('alert')).toHaveTextContent('Introduce tu nombre para continuar.');
    expect(screen.getByRole('heading', { name: 'Perfil' })).toBeInTheDocument();
  });

  it('blocks "Siguiente" on the goal step without a selection, but "Omitir" still advances', async () => {
    const user = userEvent.setup();
    renderOnboarding();

    await fillName(user, 'Diego');
    await user.click(screen.getByRole('button', { name: 'Siguiente' })); // -> metrics
    await user.click(screen.getByRole('button', { name: 'Omitir este paso' })); // -> goal
    expect(screen.getByRole('heading', { name: 'Objetivo' })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Siguiente' }));
    expect(screen.getByRole('alert')).toHaveTextContent(
      'Selecciona un objetivo o pulsa "Omitir este paso".',
    );
    expect(screen.getByRole('heading', { name: 'Objetivo' })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Omitir este paso' }));
    expect(screen.getByRole('heading', { name: 'Dirección del plan' })).toBeInTheDocument();
  });

  it('advances the goal step via "Siguiente" once an option is selected', async () => {
    const user = userEvent.setup();
    renderOnboarding();

    await fillName(user, 'Diego');
    await user.click(screen.getByRole('button', { name: 'Siguiente' })); // -> metrics
    await user.click(screen.getByRole('button', { name: 'Omitir este paso' })); // -> goal

    await user.click(screen.getByRole('radio', { name: /Hábito/ }));
    await user.click(screen.getByRole('button', { name: 'Siguiente' }));

    expect(screen.getByRole('heading', { name: 'Dirección del plan' })).toBeInTheDocument();
  });

  it('blocks "Siguiente" on the direction step without a selection, but "Omitir" still advances', async () => {
    const user = userEvent.setup();
    renderOnboarding();

    await fillName(user, 'Diego');
    await user.click(screen.getByRole('button', { name: 'Siguiente' })); // -> metrics
    await user.click(screen.getByRole('button', { name: 'Omitir este paso' })); // -> goal
    await user.click(screen.getByRole('button', { name: 'Omitir este paso' })); // -> direction
    expect(screen.getByRole('heading', { name: 'Dirección del plan' })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Siguiente' }));
    expect(screen.getByRole('alert')).toHaveTextContent(
      'Selecciona una dirección o pulsa "Omitir este paso".',
    );
    expect(screen.getByRole('heading', { name: 'Dirección del plan' })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Omitir este paso' }));
    expect(
      screen.getByRole('heading', { name: 'Disponibilidad de entrenamiento' }),
    ).toBeInTheDocument();
  });

  it('advances the direction step via "Siguiente" once an option is selected', async () => {
    const user = userEvent.setup();
    renderOnboarding();

    await fillName(user, 'Diego');
    await user.click(screen.getByRole('button', { name: 'Siguiente' })); // -> metrics
    await user.click(screen.getByRole('button', { name: 'Omitir este paso' })); // -> goal
    await user.click(screen.getByRole('button', { name: 'Omitir este paso' })); // -> direction

    await user.click(screen.getByRole('radio', { name: /Ganar músculo/ }));
    await user.click(screen.getByRole('button', { name: 'Siguiente' }));

    expect(
      screen.getByRole('heading', { name: 'Disponibilidad de entrenamiento' }),
    ).toBeInTheDocument();
  });

  it('completes the flow and routes to the dashboard with a clear next action', async () => {
    const user = userEvent.setup();
    renderOnboarding();

    await goToCompletionScreen(user);

    expect(screen.getByRole('heading', { name: 'Todo listo' })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Ir al panel' }));

    expect(await screen.findByText('Panel principal')).toBeInTheDocument();
  });

  it('renders exactly one page-level <h1> that persists across step navigation (FOR-113)', async () => {
    const user = userEvent.setup();
    renderOnboarding();

    expect(
      screen.getByRole('heading', { level: 1, name: 'Configuración inicial' }),
    ).toBeInTheDocument();
    expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(1);

    await fillName(user, 'Diego');
    await user.click(screen.getByRole('button', { name: 'Siguiente' }));

    // The page-level <h1> must not reset/duplicate when the step-level <h2>
    // changes and receives focus (FOR-61 focus management).
    expect(
      screen.getByRole('heading', { level: 1, name: 'Configuración inicial' }),
    ).toBeInTheDocument();
    expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(1);
  });

  it('resumes mid-flow progress from local storage', async () => {
    saveOnboardingProgress({
      stepIndex: 4,
      completed: false,
      answers: {
        ...INITIAL_PROGRESS.answers,
        profile: { name: 'Diego', birthDate: '', sex: '', heightCm: '' },
      },
    });

    renderOnboarding();
    await waitFor(() => expect(getProfileMock).toHaveBeenCalled());

    expect(
      screen.getByRole('heading', { name: 'Disponibilidad de entrenamiento' }),
    ).toBeInTheDocument();
    expect(screen.getByText('Paso 5 de 8')).toBeInTheDocument();
  });

  it('shows the already-completed gate and lets the user restart on a return visit', async () => {
    const user = userEvent.setup();
    saveOnboardingProgress({ ...INITIAL_PROGRESS, stepIndex: 7, completed: true });

    renderOnboarding();

    expect(
      screen.getByRole('heading', { name: 'Ya completaste la configuración inicial' }),
    ).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Volver a empezar' }));

    expect(screen.getByRole('heading', { name: 'Perfil' })).toBeInTheDocument();
    expect(screen.getByText('Paso 1 de 8')).toBeInTheDocument();
  });

  /*
   * feat/onboarding-primera-vez: AppShell now redirects an unfinished first
   * run into this page (see app/OnboardingGate.tsx). A forced entry with no
   * way out would be a trap — this is that way out, reachable from step one,
   * without having to click/skip through the rest of the wizard first.
   */
  it('offers an explicit exit on every step that skips the wizard and returns to the dashboard', async () => {
    const user = userEvent.setup();
    renderOnboarding();
    await waitFor(() => expect(getProfileMock).toHaveBeenCalled());

    expect(screen.getByRole('heading', { name: 'Perfil' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Ahora no, ir al panel' }));

    expect(await screen.findByText('Panel principal')).toBeInTheDocument();
    await waitFor(() => {
      const lastCall = submitOnboardingAnswersMock.mock.calls.at(-1);
      expect(lastCall?.[0]).toMatchObject({ completed: true });
    });
  });

  it('does not offer the step-view exit again on the completion screen, which already has its own', async () => {
    const user = userEvent.setup();
    renderOnboarding();

    await goToCompletionScreen(user);

    expect(screen.getByRole('heading', { name: 'Todo listo' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Ahora no, ir al panel' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Ir al panel' })).toBeInTheDocument();
  });

  it('has no accessibility violations on the first step (FOR-114)', async () => {
    const { container } = renderOnboarding();
    await waitFor(() => expect(getProfileMock).toHaveBeenCalled());

    expect(screen.getByRole('heading', { name: 'Perfil' })).toBeInTheDocument();
    expect(await axe(container)).toHaveNoViolations();
  });

  it('has no accessibility violations after advancing to a later step (FOR-114)', async () => {
    const user = userEvent.setup();
    const { container } = renderOnboarding();

    await fillName(user, 'Diego');
    await user.click(screen.getByRole('button', { name: 'Siguiente' }));
    expect(screen.getByRole('heading', { name: 'Métricas actuales' })).toBeInTheDocument();

    expect(await axe(container)).toHaveNoViolations();
  });
});

/** Covers `specs/FOR-121/tests.md` UI Tests + Edge Cases. */
describe('OnboardingPage — backend persistence (FOR-121)', () => {
  beforeEach(() => {
    window.localStorage.clear();
    vi.clearAllMocks();
    getProfileMock.mockResolvedValue(FRESH_PROFILE);
    submitOnboardingAnswersMock.mockResolvedValue(FRESH_PROFILE);
    createPlanRequestMock.mockResolvedValue(PLAN_REQUEST);
    getCurrentPlanRequestMock.mockResolvedValue(PLAN_REQUEST);
  });

  it("persists that step's answers to the backend at each step boundary", async () => {
    const user = userEvent.setup();
    renderOnboarding();
    await waitFor(() => expect(getProfileMock).toHaveBeenCalled());

    await fillName(user, 'Diego');
    await user.click(screen.getByRole('button', { name: 'Siguiente' }));

    await waitFor(() => expect(submitOnboardingAnswersMock).toHaveBeenCalled());
    const [input] = submitOnboardingAnswersMock.mock.calls[0];
    expect(input).toMatchObject({
      profile: { name: 'Diego' },
      completed: false,
    });
  });

  it('sets the backend firstRunCompleted flag when the flow is completed', async () => {
    const user = userEvent.setup();
    renderOnboarding();
    await waitFor(() => expect(getProfileMock).toHaveBeenCalled());

    await goToCompletionScreen(user);
    expect(screen.getByRole('heading', { name: 'Todo listo' })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Ir al panel' }));

    await waitFor(() => {
      const lastCall = submitOnboardingAnswersMock.mock.calls.at(-1);
      expect(lastCall?.[0]).toMatchObject({ completed: true });
    });
  });

  it('shows the already-completed gate sourced from the backend flag, overriding a stale local flag', async () => {
    getProfileMock.mockResolvedValue({ ...FRESH_PROFILE, firstRunCompleted: true });
    renderOnboarding();

    // No flash: the local flag (fresh/false) drives the very first paint.
    expect(screen.getByRole('heading', { name: 'Perfil' })).toBeInTheDocument();

    expect(
      await screen.findByRole('heading', { name: 'Ya completaste la configuración inicial' }),
    ).toBeInTheDocument();
  });

  it('does not block navigation when a backend save fails mid-flow, and shows a non-blocking message', async () => {
    submitOnboardingAnswersMock.mockRejectedValue(new Error('network down'));
    const user = userEvent.setup();
    renderOnboarding();
    await waitFor(() => expect(getProfileMock).toHaveBeenCalled());

    await fillName(user, 'Diego');
    await user.click(screen.getByRole('button', { name: 'Siguiente' }));

    expect(screen.getByRole('heading', { name: 'Métricas actuales' })).toBeInTheDocument();
    expect(await screen.findByText(/no se pudieron guardar/i)).toBeInTheDocument();
  });

  it('recovers answers from the backend when local storage is empty but the backend already has prior progress', async () => {
    getProfileMock.mockResolvedValue({
      ...FRESH_PROFILE,
      onboardingAnswers: {
        ...FRESH_PROFILE.onboardingAnswers,
        profile: { name: 'Diego', birthDate: '', sex: '', heightCm: '' },
      },
    });
    renderOnboarding();

    expect(await screen.findByLabelText('Nombre')).toHaveValue('Diego');
  });

  it('lets the user complete onboarding locally when the backend is unreachable for the whole flow', async () => {
    getProfileMock.mockRejectedValue(new Error('network down'));
    submitOnboardingAnswersMock.mockRejectedValue(new Error('network down'));
    const user = userEvent.setup();
    renderOnboarding();

    await fillName(user, 'Diego');
    await user.click(screen.getByRole('button', { name: 'Siguiente' })); // -> metrics
    expect(await screen.findByText(/no se pudieron guardar/i)).toBeInTheDocument();

    for (let i = 0; i < 7; i += 1) {
      await user.click(screen.getByRole('button', { name: 'Omitir este paso' }));
    }
    expect(screen.getByRole('heading', { name: 'Todo listo' })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Ir al panel' }));
    expect(await screen.findByText('Panel principal')).toBeInTheDocument();
  });

  it('does not trap a returning user out of the already-completed gate when the backend fetch fails (graceful fallback to local)', async () => {
    getProfileMock.mockRejectedValue(new Error('network down'));
    saveOnboardingProgress({ ...INITIAL_PROGRESS, stepIndex: 7, completed: true });

    renderOnboarding();

    expect(
      screen.getByRole('heading', { name: 'Ya completaste la configuración inicial' }),
    ).toBeInTheDocument();
  });

  it('does not trap a new user into the already-completed gate when the backend fetch fails (graceful fallback to local)', async () => {
    getProfileMock.mockRejectedValue(new Error('network down'));

    renderOnboarding();

    expect(screen.getByRole('heading', { name: 'Perfil' })).toBeInTheDocument();
  });

  it('allows re-submitting onboarding after a restart (mirrors the backend’s allowed re-submission)', async () => {
    saveOnboardingProgress({ ...INITIAL_PROGRESS, stepIndex: 7, completed: true });
    const user = userEvent.setup();
    renderOnboarding();

    expect(
      screen.getByRole('heading', { name: 'Ya completaste la configuración inicial' }),
    ).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Volver a empezar' }));
    expect(screen.getByRole('heading', { name: 'Perfil' })).toBeInTheDocument();

    await fillName(user, 'Diego');
    await user.click(screen.getByRole('button', { name: 'Siguiente' }));

    await waitFor(() => expect(submitOnboardingAnswersMock).toHaveBeenCalled());
    expect(screen.getByRole('heading', { name: 'Métricas actuales' })).toBeInTheDocument();
  });
});

/**
 * Covers ADR-015 slice 3: the wizard's final, additional submission — `POST
 * /api/v1/plan-requests` — fired once the flow is genuinely finished (never
 * on the "Ahora no, ir al panel" early exit, which `goToCompletionScreen`/
 * `goToCompletionScreenWithDirection` never take).
 */
describe('OnboardingPage — plan-request submission (ADR-015 slice 3)', () => {
  beforeEach(() => {
    window.localStorage.clear();
    vi.clearAllMocks();
    getProfileMock.mockResolvedValue(FRESH_PROFILE);
    submitOnboardingAnswersMock.mockResolvedValue(FRESH_PROFILE);
    createPlanRequestMock.mockResolvedValue(PLAN_REQUEST);
    getCurrentPlanRequestMock.mockResolvedValue(PLAN_REQUEST);
  });

  it('does not submit a plan request when the wizard finishes without a direction', async () => {
    const user = userEvent.setup();
    renderOnboarding();

    await goToCompletionScreen(user);

    expect(screen.getByRole('heading', { name: 'Todo listo' })).toBeInTheDocument();
    expect(createPlanRequestMock).not.toHaveBeenCalled();
    expect(screen.getByRole('alert')).toHaveTextContent(/dirección/i);
  });

  it('lets the user jump back to the direction step from the missing-direction notice', async () => {
    const user = userEvent.setup();
    renderOnboarding();

    await goToCompletionScreen(user);
    await user.click(screen.getByRole('button', { name: 'Elegir dirección' }));

    expect(screen.getByRole('heading', { name: 'Dirección del plan' })).toBeInTheDocument();
  });

  it('submits the mapped plan request on completion and shows a calm confirmation', async () => {
    const user = userEvent.setup();
    renderOnboarding();

    await goToCompletionScreenWithDirection(user, 'Perder grasa');

    await waitFor(() => expect(createPlanRequestMock).toHaveBeenCalled());
    const [input] = createPlanRequestMock.mock.calls[0];
    expect(input).toMatchObject({ direction: 'LOSE_FAT', trainingDaysPerWeek: 0 });

    expect(await screen.findByRole('status')).toHaveTextContent(/petición/i);
    // "Ir al panel" is never blocked by the submission outcome.
    await user.click(screen.getByRole('button', { name: 'Ir al panel' }));
    expect(await screen.findByText('Panel principal')).toBeInTheDocument();
  });

  it('shows that a request is already open on a 409, using GET /plan-requests/current instead of a raw error', async () => {
    createPlanRequestMock.mockRejectedValue(
      new ApiRequestError(409, 'Ya tienes una petición de plan en curso.', 'CONFLICT'),
    );
    getCurrentPlanRequestMock.mockResolvedValue(PLAN_REQUEST);
    const user = userEvent.setup();
    renderOnboarding();

    await goToCompletionScreenWithDirection(user);

    await waitFor(() => expect(getCurrentPlanRequestMock).toHaveBeenCalled());
    const notice = await screen.findByRole('status');
    expect(notice).toHaveTextContent(/ya tienes una petición de plan en curso/i);
  });

  it('names the missing profile field on a validation failure and links to Ajustes', async () => {
    createPlanRequestMock.mockRejectedValue(
      new ApiRequestError(
        400,
        'Completa tu altura en tu perfil antes de pedir un plan.',
        'VALIDATION_ERROR',
      ),
    );
    const user = userEvent.setup();
    renderOnboarding();

    await goToCompletionScreenWithDirection(user);

    const notice = await screen.findByRole('alert');
    expect(notice).toHaveTextContent('Completa tu altura en tu perfil antes de pedir un plan.');
    expect(screen.getByRole('link', { name: 'Ir a Ajustes' })).toHaveAttribute(
      'href',
      '/app/settings',
    );
  });

  it('points to Mediciones when the missing piece is the body measurement', async () => {
    createPlanRequestMock.mockRejectedValue(
      new ApiRequestError(
        400,
        'Necesitas registrar al menos una medición corporal antes de pedir un plan.',
        'VALIDATION_ERROR',
      ),
    );
    const user = userEvent.setup();
    renderOnboarding();

    await goToCompletionScreenWithDirection(user);

    expect(screen.getByRole('link', { name: 'Registrar medición' })).toHaveAttribute(
      'href',
      '/app/measurements',
    );
  });

  it('jumps back to the goal step when the missing piece is the objective', async () => {
    createPlanRequestMock.mockRejectedValue(
      new ApiRequestError(
        400,
        'Necesitas indicar tu objetivo antes de pedir un plan.',
        'VALIDATION_ERROR',
      ),
    );
    const user = userEvent.setup();
    renderOnboarding();

    await goToCompletionScreenWithDirection(user);
    await user.click(screen.getByRole('button', { name: 'Elegir objetivo' }));

    expect(screen.getByRole('heading', { name: 'Objetivo' })).toBeInTheDocument();
  });

  it('shows the backend-safe message for any other failure, never a generic "algo ha ido mal"', async () => {
    createPlanRequestMock.mockRejectedValue(
      new ApiRequestError(500, 'Ha ocurrido un error inesperado.', 'INTERNAL_ERROR'),
    );
    const user = userEvent.setup();
    renderOnboarding();

    await goToCompletionScreenWithDirection(user);

    expect(await screen.findByRole('alert')).toHaveTextContent('Ha ocurrido un error inesperado.');
  });

  it('re-attempts the submission if the user fixes the direction and finishes the wizard again', async () => {
    const user = userEvent.setup();
    renderOnboarding();

    await goToCompletionScreen(user);
    expect(createPlanRequestMock).not.toHaveBeenCalled();

    await user.click(screen.getByRole('button', { name: 'Elegir dirección' }));
    await user.click(screen.getByRole('radio', { name: /Mantenerme/ }));
    await user.click(screen.getByRole('button', { name: 'Siguiente' })); // direction -> training
    for (let i = 0; i < 4; i += 1) {
      await user.click(screen.getByRole('button', { name: 'Omitir este paso' }));
    }

    await waitFor(() => expect(createPlanRequestMock).toHaveBeenCalledTimes(1));
    const [input] = createPlanRequestMock.mock.calls[0];
    expect(input).toMatchObject({ direction: 'MAINTAIN' });
  });
});
