import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { useAuth } from '../auth/AuthContext';
import { axe } from '../test/axe';
import { LandingPage } from './LandingPage';

vi.mock('../auth/AuthContext', () => ({ useAuth: vi.fn() }));

describe('LandingPage', () => {
  it('renders the complete public composition below the global bar', () => {
    mockLanding({ status: 'anonymous' });
    renderLanding();

    expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(1);
    // FOR-185: the public navigation bar is no longer part of this page — it
    // moved to the global Topbar (layout/RootLayout.tsx).
    expect(
      screen.queryByRole('navigation', { name: 'Navegación pública' }),
    ).not.toBeInTheDocument();
    expect(
      screen.getByRole('region', { name: 'Una visión completa, sin falsas promesas' }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('region', { name: 'La información importante, conectada' }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole('region', { name: 'Pon orden en tu progreso personal' }),
    ).toBeInTheDocument();
    expect(screen.getByRole('contentinfo')).toBeInTheDocument();
    expect(screen.queryByText(/\+10\.000|versión 4\.0|precio|blog/i)).not.toBeInTheDocument();
  });

  it('offers real login and registration actions to anonymous visitors', () => {
    mockLanding({ status: 'anonymous' });
    renderLanding();

    expect(screen.getByLabelText('Correo electrónico')).toHaveAttribute('autocomplete', 'email');
    expect(screen.getByLabelText('Contraseña')).toHaveAttribute('autocomplete', 'current-password');
    expect(screen.getByRole('link', { name: 'Crear cuenta' })).toHaveAttribute('href', '/registro');
  });

  it('submits login once, disables the form while pending and navigates to the app', async () => {
    const login = vi.fn().mockReturnValue(new Promise<void>(() => undefined));
    mockLanding({ status: 'anonymous', login });
    renderLanding();

    await userEvent.type(screen.getByLabelText('Correo electrónico'), 'persona@example.com');
    await userEvent.type(screen.getByLabelText('Contraseña'), 'password1234');
    const submit = screen.getByRole('button', { name: 'Iniciar sesión' });
    await userEvent.click(submit);
    await userEvent.click(submit);

    expect(login).toHaveBeenCalledTimes(1);
    expect(submit).toBeDisabled();
    expect(submit).toHaveAttribute('aria-busy', 'true');
  });

  it('announces a safe login error and allows retry', async () => {
    const login = vi
      .fn()
      .mockRejectedValueOnce(new Error('internal details'))
      .mockResolvedValueOnce(undefined);
    mockLanding({ status: 'anonymous', login });
    const { container } = renderLanding();

    await userEvent.type(screen.getByLabelText('Correo electrónico'), 'persona@example.com');
    await userEvent.type(screen.getByLabelText('Contraseña'), 'password1234');
    await userEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No se pudo iniciar la sesión. Inténtalo de nuevo.',
    );
    expect(screen.queryByText('internal details')).not.toBeInTheDocument();
    expect(await axe(container)).toHaveNoViolations();
    await userEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }));
    expect(login).toHaveBeenCalledTimes(2);
  });

  it('shows authenticated access without an unnecessary login form', () => {
    mockLanding({
      status: 'authenticated',
      user: { id: 'user-1', email: 'persona@example.com' },
    });
    renderLanding();

    expect(screen.getByText('persona@example.com')).toBeInTheDocument();
    expect(screen.getAllByRole('link', { name: 'Ir a la aplicación' })[0]).toHaveAttribute(
      'href',
      '/app',
    );
    expect(screen.queryByLabelText('Contraseña')).not.toBeInTheDocument();
  });

  it.each([
    ['loading', false, 'Comprobando tu sesión…'],
    ['loading', true, 'No pudimos comprobar tu sesión.'],
  ] as const)('keeps public content visible during %s/bootstrap error', (status, error, copy) => {
    mockLanding({ status, bootstrapError: error });
    renderLanding();

    expect(screen.getByRole('heading', { level: 1 })).toBeInTheDocument();
    expect(screen.getByText(copy)).toBeInTheDocument();
    expect(
      screen.getByRole('region', { name: 'Una visión completa, sin falsas promesas' }),
    ).toBeInTheDocument();
  });

  it.each([
    ['anonymous', false],
    ['authenticated', false],
    ['loading', false],
    ['loading', true],
  ] as const)(
    'has no automated accessibility violations for %s state (bootstrap error: %s)',
    async (status, bootstrapError) => {
      mockLanding({
        status,
        bootstrapError,
        user:
          status === 'authenticated' ? { id: 'user-1', email: 'persona@example.com' } : undefined,
      });
      const { container } = renderLanding();
      expect(await axe(container)).toHaveNoViolations();
    },
  );
});

function renderLanding() {
  return render(
    <MemoryRouter>
      <LandingPage />
    </MemoryRouter>,
  );
}

function mockLanding(overrides: Partial<ReturnType<typeof useAuth>>) {
  vi.mocked(useAuth).mockReturnValue({
    status: 'anonymous',
    user: null,
    bootstrapError: false,
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    refreshCurrentUser: vi.fn(),
    ...overrides,
  });
}
