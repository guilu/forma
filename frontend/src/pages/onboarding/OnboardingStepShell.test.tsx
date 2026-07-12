import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { OnboardingStepShell } from './OnboardingStepShell';

describe('OnboardingStepShell', () => {
  it('renders the step title, progress text and content', () => {
    render(
      <OnboardingStepShell
        stepIndex={1}
        totalSteps={7}
        title="Métricas actuales"
        canGoBack
        skippable
        onBack={vi.fn()}
        onNext={vi.fn()}
        onSkip={vi.fn()}
      >
        <p>Contenido del paso</p>
      </OnboardingStepShell>,
    );

    expect(screen.getByRole('heading', { name: 'Métricas actuales' })).toBeInTheDocument();
    expect(screen.getByText('Paso 2 de 7')).toBeInTheDocument();
    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '29');
    expect(screen.getByText('Contenido del paso')).toBeInTheDocument();
  });

  it('disables "Atrás" when canGoBack is false and omits skip when not skippable', () => {
    render(
      <OnboardingStepShell
        stepIndex={0}
        totalSteps={7}
        title="Perfil"
        canGoBack={false}
        skippable={false}
        onBack={vi.fn()}
        onNext={vi.fn()}
        onSkip={vi.fn()}
      >
        <p>Contenido</p>
      </OnboardingStepShell>,
    );

    expect(screen.getByRole('button', { name: 'Atrás' })).toBeDisabled();
    expect(screen.queryByRole('button', { name: 'Omitir este paso' })).not.toBeInTheDocument();
  });

  it('shows a validation error near the top of the step content', () => {
    render(
      <OnboardingStepShell
        stepIndex={0}
        totalSteps={7}
        title="Perfil"
        error="Introduce tu nombre para continuar."
        canGoBack={false}
        skippable={false}
        onBack={vi.fn()}
        onNext={vi.fn()}
        onSkip={vi.fn()}
      >
        <p>Contenido</p>
      </OnboardingStepShell>,
    );

    expect(screen.getByRole('alert')).toHaveTextContent('Introduce tu nombre para continuar.');
  });

  it('calls onBack, onNext and onSkip from their respective actions', async () => {
    const user = userEvent.setup();
    const onBack = vi.fn();
    const onNext = vi.fn();
    const onSkip = vi.fn();

    render(
      <OnboardingStepShell
        stepIndex={2}
        totalSteps={7}
        title="Objetivo"
        canGoBack
        skippable
        onBack={onBack}
        onNext={onNext}
        onSkip={onSkip}
      >
        <p>Contenido</p>
      </OnboardingStepShell>,
    );

    await user.click(screen.getByRole('button', { name: 'Atrás' }));
    await user.click(screen.getByRole('button', { name: 'Omitir este paso' }));
    await user.click(screen.getByRole('button', { name: 'Siguiente' }));

    expect(onBack).toHaveBeenCalledTimes(1);
    expect(onSkip).toHaveBeenCalledTimes(1);
    expect(onNext).toHaveBeenCalledTimes(1);
  });

  it('renders a custom next label (e.g. "Finalizar" on the last step)', () => {
    render(
      <OnboardingStepShell
        stepIndex={6}
        totalSteps={7}
        title="Conectar integración"
        canGoBack
        skippable
        nextLabel="Finalizar"
        onBack={vi.fn()}
        onNext={vi.fn()}
        onSkip={vi.fn()}
      >
        <p>Contenido</p>
      </OnboardingStepShell>,
    );

    expect(screen.getByRole('button', { name: 'Finalizar' })).toBeInTheDocument();
  });
});
