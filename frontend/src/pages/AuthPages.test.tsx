import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { useAuth } from '../auth/AuthContext';
import { LoginPage } from './LoginPage';
import { RegisterPage } from './RegisterPage';
import authStyles from './AuthPage.module.css';
import authCss from './AuthPage.module.css?raw';

vi.mock('../auth/AuthContext', () => ({ useAuth: vi.fn() }));

describe('auth pages', () => {
  /*
   * The login card used to open with the generic "Iniciar sesión" — the same
   * string as the submit button beneath it, so a screen reader announced the
   * heading and the button as if they were the same thing. "vuelta" carries
   * the landing headline's accent gradient so the returning-user welcome
   * reads as the same brand voice as the public page.
   */
  it('welcomes a returning visitor with the accent-highlighted headline', () => {
    vi.mocked(useAuth).mockReturnValue(authState({}));
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    );

    const heading = screen.getByRole('heading', { level: 1, name: 'Bienvenido de vuelta' });
    expect(heading).toBeInTheDocument();
    expect(screen.getByText('Accede a tu cuenta para continuar preparándote')).toBeInTheDocument();

    const accent = screen.getByText('vuelta');
    expect(accent.tagName).toBe('SPAN');
    expect(accent.className).toContain(authStyles.titleAccent);
  });

  it('paints the login headline accent with the shared landing gradient token', () => {
    expect(authCss).toMatch(
      /\.titleAccent\s*{[^}]*background-image:\s*var\(--gradient-accent-text\);[^}]*background-clip:\s*text;[^}]*color:\s*transparent;/s,
    );
  });

  it('logs in from the Spanish form', async () => {
    const login = vi.fn().mockResolvedValue(undefined);
    vi.mocked(useAuth).mockReturnValue(authState({ login }));
    render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    );
    await userEvent.type(screen.getByLabelText('Correo electrónico'), 'user@example.com');
    await userEvent.type(screen.getByLabelText('Contraseña'), 'secret123');
    await userEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }));
    expect(login).toHaveBeenCalledWith({ email: 'user@example.com', password: 'secret123' });
  });

  it('blocks registration when password confirmation differs', async () => {
    const register = vi.fn();
    vi.mocked(useAuth).mockReturnValue(authState({ register }));
    render(
      <MemoryRouter>
        <RegisterPage />
      </MemoryRouter>,
    );
    await userEvent.type(screen.getByLabelText('Correo electrónico'), 'user@example.com');
    await userEvent.type(screen.getByLabelText('Contraseña'), 'secret123');
    await userEvent.type(screen.getByLabelText('Confirmar contraseña'), 'different');
    await userEvent.click(screen.getByRole('button', { name: 'Crear cuenta' }));
    expect(screen.getByText('Las contraseñas no coinciden.')).toBeInTheDocument();
    expect(register).not.toHaveBeenCalled();
  });

  it('requires the backend minimum of 12 password characters', () => {
    vi.mocked(useAuth).mockReturnValue(authState({}));
    render(
      <MemoryRouter>
        <RegisterPage />
      </MemoryRouter>,
    );
    expect(screen.getByLabelText('Contraseña')).toHaveAttribute('minlength', '12');
  });

  it.each([
    ['/login', <LoginPage />],
    ['/register', <RegisterPage />],
  ])('redirects an authenticated user away from %s', (path, page) => {
    vi.mocked(useAuth).mockReturnValue(
      authState({
        status: 'authenticated',
        user: { id: '1', email: 'user@example.com', role: 'USER' as const },
      }),
    );
    render(
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path={path} element={page} />
          <Route path="/app" element={<div>Dashboard autenticado</div>} />
        </Routes>
      </MemoryRouter>,
    );
    expect(screen.getByText('Dashboard autenticado')).toBeInTheDocument();
  });

  it('returns to the preserved destination after successful login', async () => {
    const login = vi.fn().mockResolvedValue(undefined);
    vi.mocked(useAuth).mockReturnValue(authState({ login }));
    render(
      <MemoryRouter
        initialEntries={[
          {
            pathname: '/login',
            state: {
              from: {
                pathname: '/app/progress',
                search: '?periodo=mes',
                hash: '#tendencia',
              },
            },
          },
        ]}
      >
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/app/progress" element={<LocationPath />} />
        </Routes>
      </MemoryRouter>,
    );
    await userEvent.type(screen.getByLabelText('Correo electrónico'), 'user@example.com');
    await userEvent.type(screen.getByLabelText('Contraseña'), 'password1234');
    await userEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }));
    expect(await screen.findByText('/app/progress?periodo=mes#tendencia')).toBeInTheDocument();
  });

  it('returns to the preserved destination after successful registration', async () => {
    const register = vi.fn().mockResolvedValue(undefined);
    vi.mocked(useAuth).mockReturnValue(authState({ register }));
    render(
      <MemoryRouter
        initialEntries={[
          {
            pathname: '/register',
            state: {
              from: {
                pathname: '/app/nutrition',
                search: '?dia=hoy',
                hash: '#macros',
              },
            },
          },
        ]}
      >
        <Routes>
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/app/nutrition" element={<LocationPath />} />
        </Routes>
      </MemoryRouter>,
    );
    await userEvent.type(screen.getByLabelText('Correo electrónico'), 'user@example.com');
    await userEvent.type(screen.getByLabelText('Contraseña'), 'password1234');
    await userEvent.type(screen.getByLabelText('Confirmar contraseña'), 'password1234');
    await userEvent.click(screen.getByRole('button', { name: 'Crear cuenta' }));
    expect(register).toHaveBeenCalledWith({
      email: 'user@example.com',
      password: 'password1234',
    });
    expect(await screen.findByText('/app/nutrition?dia=hoy#macros')).toBeInTheDocument();
  });

  it('rejects an external post-login destination', async () => {
    const login = vi.fn().mockResolvedValue(undefined);
    vi.mocked(useAuth).mockReturnValue(authState({ login }));
    render(
      <MemoryRouter
        initialEntries={[
          { pathname: '/login', state: { from: { pathname: '//malicious.example/path' } } },
        ]}
      >
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/app" element={<LocationPath />} />
        </Routes>
      </MemoryRouter>,
    );
    await userEvent.type(screen.getByLabelText('Correo electrónico'), 'user@example.com');
    await userEvent.type(screen.getByLabelText('Contraseña'), 'password1234');
    await userEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }));
    expect(await screen.findByText('/app')).toBeInTheDocument();
  });

  it('rejects an external post-registration destination', async () => {
    const register = vi.fn().mockResolvedValue(undefined);
    vi.mocked(useAuth).mockReturnValue(authState({ register }));
    render(
      <MemoryRouter
        initialEntries={[
          { pathname: '/register', state: { from: { pathname: '//malicious.example/path' } } },
        ]}
      >
        <Routes>
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/app" element={<LocationPath />} />
        </Routes>
      </MemoryRouter>,
    );
    await userEvent.type(screen.getByLabelText('Correo electrónico'), 'user@example.com');
    await userEvent.type(screen.getByLabelText('Contraseña'), 'password1234');
    await userEvent.type(screen.getByLabelText('Confirmar contraseña'), 'password1234');
    await userEvent.click(screen.getByRole('button', { name: 'Crear cuenta' }));
    expect(await screen.findByText('/app')).toBeInTheDocument();
  });

  it.each([
    ['/login', <LoginPage />],
    ['/register', <RegisterPage />],
  ])('sends an authenticated user from %s to a valid preserved destination', (path, page) => {
    vi.mocked(useAuth).mockReturnValue(
      authState({
        status: 'authenticated',
        user: { id: '1', email: 'user@example.com', role: 'USER' as const },
      }),
    );
    render(
      <MemoryRouter
        initialEntries={[
          {
            pathname: path,
            state: {
              from: {
                pathname: '/app/progress',
                search: '?periodo=mes',
                hash: '#tendencia',
              },
            },
          },
        ]}
      >
        <Routes>
          <Route path={path} element={page} />
          <Route path="/app/progress" element={<LocationPath />} />
        </Routes>
      </MemoryRouter>,
    );
    expect(screen.getByText('/app/progress?periodo=mes#tendencia')).toBeInTheDocument();
  });

  it('shows safe Spanish copy for backend login and registration failures', async () => {
    const login = vi.fn().mockRejectedValue(new Error('internal details'));
    const register = vi.fn().mockRejectedValue(new Error('database details'));
    vi.mocked(useAuth).mockReturnValue(authState({ login, register }));
    const loginView = render(
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>,
    );
    await userEvent.type(screen.getByLabelText('Correo electrónico'), 'user@example.com');
    await userEvent.type(screen.getByLabelText('Contraseña'), 'password1234');
    await userEvent.click(screen.getByRole('button', { name: 'Iniciar sesión' }));
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No se pudo iniciar la sesión. Inténtalo de nuevo.',
    );
    expect(screen.queryByText('internal details')).not.toBeInTheDocument();
    loginView.unmount();

    render(
      <MemoryRouter>
        <RegisterPage />
      </MemoryRouter>,
    );
    await userEvent.type(screen.getByLabelText('Correo electrónico'), 'new@example.com');
    await userEvent.type(screen.getByLabelText('Contraseña'), 'password1234');
    await userEvent.type(screen.getByLabelText('Confirmar contraseña'), 'password1234');
    await userEvent.click(screen.getByRole('button', { name: 'Crear cuenta' }));
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No se pudo crear la cuenta. Revisa los datos e inténtalo de nuevo.',
    );
    expect(screen.queryByText('database details')).not.toBeInTheDocument();
  });

  it.each([
    ['/login', <LoginPage />],
    ['/register', <RegisterPage />],
  ])(
    'offers a Google sign-in link pointing at the backend authorization endpoint on %s',
    (path, page) => {
      vi.mocked(useAuth).mockReturnValue(authState({}));
      render(
        <MemoryRouter initialEntries={[path]}>
          <Routes>
            <Route path={path} element={page} />
          </Routes>
        </MemoryRouter>,
      );

      expect(screen.getByRole('link', { name: 'Continuar con Google' })).toHaveAttribute(
        'href',
        '/api/oauth2/authorization/google',
      );
    },
  );

  it('shows a Spanish error when redirected back with ?error=google', () => {
    vi.mocked(useAuth).mockReturnValue(authState({}));
    render(
      <MemoryRouter initialEntries={['/login?error=google']}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByRole('alert')).toHaveTextContent('No se pudo iniciar sesión con Google.');
  });

  it('shows no Google error when the login page loads without the error flag', () => {
    vi.mocked(useAuth).mockReturnValue(authState({}));
    render(
      <MemoryRouter initialEntries={['/login']}>
        <LoginPage />
      </MemoryRouter>,
    );

    expect(screen.queryByText('No se pudo iniciar sesión con Google.')).not.toBeInTheDocument();
  });
});

function LocationPath() {
  const location = useLocation();
  return <div>{`${location.pathname}${location.search}${location.hash}`}</div>;
}

function authState(overrides: Partial<ReturnType<typeof useAuth>>): ReturnType<typeof useAuth> {
  return {
    status: 'anonymous',
    user: null,
    bootstrapError: false,
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    refreshCurrentUser: vi.fn(),
    ...overrides,
  };
}
