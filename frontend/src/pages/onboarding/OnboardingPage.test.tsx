import { describe, expect, it, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { OnboardingPage } from './OnboardingPage';
import { saveOnboardingProgress, INITIAL_PROGRESS } from './onboardingStorage';

/**
 * Covers `specs/FOR-59/tests.md` UI Tests: ordered steps with progress,
 * next/back navigation, skip past non-critical steps, validation blocking
 * advance, completion routing to the dashboard, and resume restoring saved
 * progress.
 */
function renderOnboarding() {
  return render(
    <MemoryRouter initialEntries={['/onboarding']}>
      <Routes>
        <Route path="/onboarding" element={<OnboardingPage />} />
        <Route path="/" element={<div>Panel principal</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

async function fillName(user: ReturnType<typeof userEvent.setup>, name: string) {
  await user.type(screen.getByLabelText('Nombre'), name);
}

describe('OnboardingPage', () => {
  beforeEach(() => {
    window.localStorage.clear();
  });

  it('renders steps in order with progress indication as the user advances', async () => {
    const user = userEvent.setup();
    renderOnboarding();

    expect(screen.getByRole('heading', { name: 'Perfil' })).toBeInTheDocument();
    expect(screen.getByText('Paso 1 de 7')).toBeInTheDocument();

    await fillName(user, 'Diego');
    await user.click(screen.getByRole('button', { name: 'Siguiente' }));
    expect(screen.getByRole('heading', { name: 'Métricas actuales' })).toBeInTheDocument();
    expect(screen.getByText('Paso 2 de 7')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Omitir este paso' }));
    expect(screen.getByRole('heading', { name: 'Objetivo' })).toBeInTheDocument();
    expect(screen.getByText('Paso 3 de 7')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Omitir este paso' }));
    expect(
      screen.getByRole('heading', { name: 'Disponibilidad de entrenamiento' }),
    ).toBeInTheDocument();
    expect(screen.getByText('Paso 4 de 7')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Omitir este paso' }));
    expect(screen.getByRole('heading', { name: 'Equipamiento' })).toBeInTheDocument();
    expect(screen.getByText('Paso 5 de 7')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Omitir este paso' }));
    expect(screen.getByRole('heading', { name: 'Preferencias de nutrición' })).toBeInTheDocument();
    expect(screen.getByText('Paso 6 de 7')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Omitir este paso' }));
    expect(screen.getByRole('heading', { name: 'Conectar integración' })).toBeInTheDocument();
    expect(screen.getByText('Paso 7 de 7')).toBeInTheDocument();
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

  it('does not offer a skip action on the critical profile step', () => {
    renderOnboarding();

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
    expect(
      screen.getByRole('heading', { name: 'Disponibilidad de entrenamiento' }),
    ).toBeInTheDocument();
  });

  it('advances the goal step via "Siguiente" once an option is selected', async () => {
    const user = userEvent.setup();
    renderOnboarding();

    await fillName(user, 'Diego');
    await user.click(screen.getByRole('button', { name: 'Siguiente' })); // -> metrics
    await user.click(screen.getByRole('button', { name: 'Omitir este paso' })); // -> goal

    await user.click(screen.getByRole('radio', { name: /Hábito/ }));
    await user.click(screen.getByRole('button', { name: 'Siguiente' }));

    expect(
      screen.getByRole('heading', { name: 'Disponibilidad de entrenamiento' }),
    ).toBeInTheDocument();
  });

  it('completes the flow and routes to the dashboard with a clear next action', async () => {
    const user = userEvent.setup();
    renderOnboarding();

    await fillName(user, 'Diego');
    await user.click(screen.getByRole('button', { name: 'Siguiente' })); // metrics -> goal
    for (let i = 0; i < 6; i += 1) {
      await user.click(screen.getByRole('button', { name: 'Omitir este paso' }));
    }

    expect(screen.getByRole('heading', { name: 'Todo listo' })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Ir al panel' }));

    expect(await screen.findByText('Panel principal')).toBeInTheDocument();
  });

  it('resumes mid-flow progress from local storage', () => {
    saveOnboardingProgress({
      stepIndex: 3,
      completed: false,
      answers: {
        ...INITIAL_PROGRESS.answers,
        profile: { name: 'Diego', birthDate: '', sex: '', heightCm: '' },
      },
    });

    renderOnboarding();

    expect(
      screen.getByRole('heading', { name: 'Disponibilidad de entrenamiento' }),
    ).toBeInTheDocument();
    expect(screen.getByText('Paso 4 de 7')).toBeInTheDocument();
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
    expect(screen.getByText('Paso 1 de 7')).toBeInTheDocument();
  });
});
