import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { useAuth } from '../auth/AuthContext';
import { axe } from '../test/axe';
import { LandingPage } from './LandingPage';
import landingCss from './LandingPage.module.css?raw';

vi.mock('../auth/AuthContext', () => ({ useAuth: vi.fn() }));

describe('LandingPage', () => {
  it('keeps the branded gradient and gives every call to action the pill radius', () => {
    expect(landingCss).toMatch(
      /\.accentText\s*{[^}]*background-image:\s*linear-gradient\(to right, rgb\(18 122 95\), #ff9800, rgb\(125 237 92\)\);[^}]*background-clip:\s*text;[^}]*color:\s*transparent;/s,
    );
    /*
     * The buttons are pills. They were the first ones to be — the page parted
     * with the app's `--radius-lg` on its own, and every other public surface
     * then carried its own override to opt out of the app radius. `Button` is
     * fully round for the whole product now, so three of these four inherit it
     * and only restate it; `ctaSecondary` is a plain `<a>` with no `Button`
     * underneath and is the reason to keep asserting all four together, so a
     * later edit cannot leave one square among three pills.
     */
    for (const cta of ['ctaPrimary', 'ctaSecondary', 'ctaPill', 'loginSubmit']) {
      expect(landingCss, `${cta} is not a pill`).toMatch(
        new RegExp(`\\.${cta}[^{]*{[^}]*border-radius:\\s*var\\(--radius-full\\);`, 's'),
      );
    }
  });

  it('leads with the offer the product actually delivers', () => {
    mockLanding({ status: 'anonymous' });
    renderLanding();

    expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(1);
    expect(
      screen.getByRole('heading', {
        level: 1,
        name: /Entrenamiento y nutrición con la compra ya hecha\./,
      }),
    ).toBeInTheDocument();
    // One action, repeated: the hero and the closing section both point at the
    // funnel, and nothing else on the page competes with them for the click.
    const primary = screen.getAllByRole('link', { name: 'Crear mi plan gratis' });
    expect(primary).toHaveLength(2);
    for (const cta of primary) expect(cta).toHaveAttribute('href', '/plan');
  });

  it('renders every public section of the redesign', () => {
    mockLanding({ status: 'anonymous' });
    renderLanding();

    for (const name of [
      'De cuatro preguntas a la cesta de la compra',
      'Cada sesión te dice qué trabaja y qué solo acompaña.',
      'El plan no sirve de nada si no llega a la nevera.',
      'El peso solo no cuenta la película entera',
      'Lo que la gente pregunta antes de empezar',
      'Empieza por saber qué toca hoy',
    ]) {
      expect(screen.getByRole('region', { name })).toBeInTheDocument();
    }
    expect(screen.getByRole('contentinfo')).toBeInTheDocument();
    // The bar itself belongs to the global Topbar, not to this page.
    expect(
      screen.queryByRole('navigation', { name: 'Navegación pública' }),
    ).not.toBeInTheDocument();
  });

  // The public bar's anchors (`/#training`, `/#nutrition`, `/#plans`) only work
  // if this page keeps providing the matching targets.
  it.each(['training', 'nutrition', 'plans'])(
    'provides the #%s target the public navigation links to',
    (id) => {
      mockLanding({ status: 'anonymous' });
      const { container } = renderLanding();

      expect(container.querySelector(`#${id}`)).not.toBeNull();
    },
  );

  it('makes the muscle overlay the hero, drawing both views with real emphasis levels', () => {
    mockLanding({ status: 'anonymous' });
    const { container } = renderLanding();

    const hero = screen.getByRole('region', { name: /Mapa muscular/ });
    expect(within(hero).getByText(/Mapa muscular · 6 grupos activos/)).toBeInTheDocument();

    expect(container.querySelector('[data-silhouette="male/front"]')).not.toBeNull();
    expect(container.querySelector('[data-silhouette="male/back"]')).not.toBeNull();

    // Primary and secondary are what the overlay exists to distinguish; a hero
    // that only ever drew one of them would sell the feature short.
    expect(container.querySelector('[data-muscle="PECTORAL"][data-role="primary"]')).not.toBeNull();
    expect(
      container.querySelector('[data-muscle="DELTOID_FRONT"][data-role="primary"]'),
    ).not.toBeNull();
    expect(container.querySelector('[data-muscle="ABS"][data-role="secondary"]')).not.toBeNull();
    expect(container.querySelector('[data-muscle="TRICEPS"][data-role="primary"]')).not.toBeNull();
    expect(
      container.querySelector('[data-muscle="TRAPEZIUS"][data-role="secondary"]'),
    ).not.toBeNull();
  });

  /*
   * The funnel is four beats, not four paragraphs. The descriptions this
   * section used to carry restated what the sections below it already say at
   * length, and a visitor scanning the page reads the numbers and moves on.
   */
  it('reduces the funnel to four numbered steps and nothing else', () => {
    mockLanding({ status: 'anonymous' });
    renderLanding();

    const steps = screen.getByRole('region', {
      name: 'De cuatro preguntas a la cesta de la compra',
    });
    expect(
      within(steps)
        .getAllByRole('heading', { level: 3 })
        .map((heading) => heading.textContent),
    ).toEqual([
      'Tu punto de partida',
      'Tu semana de entreno',
      'Comida según el día',
      'La compra, resuelta',
    ]);
    expect(within(steps).getAllByRole('listitem')).toHaveLength(4);
    expect(steps.textContent).not.toMatch(/Edad, peso, altura/);
  });

  it('answers the six questions a visitor asks before starting', () => {
    mockLanding({ status: 'anonymous' });
    renderLanding();

    const faq = screen.getByRole('region', { name: 'Lo que la gente pregunta antes de empezar' });
    expect(within(faq).getAllByRole('heading', { level: 3 })).toHaveLength(6);
    expect(
      within(faq).getByRole('heading', {
        level: 3,
        name: '¿Necesito una cuenta para ver mi plan?',
      }),
    ).toBeInTheDocument();
  });

  it('collapses every answer until its question is opened', async () => {
    mockLanding({ status: 'anonymous' });
    renderLanding();

    const questions = within(
      screen.getByRole('region', { name: 'Lo que la gente pregunta antes de empezar' }),
    ).getAllByRole('button');
    expect(questions).toHaveLength(6);
    for (const question of questions) {
      expect(question).toHaveAttribute('aria-expanded', 'false');
    }
    expect(screen.getByText(/^No\. El generador son cuatro pasos/)).not.toBeVisible();

    await userEvent.click(questions[0]);

    expect(questions[0]).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByText(/^No\. El generador son cuatro pasos/)).toBeVisible();

    await userEvent.click(questions[0]);

    expect(questions[0]).toHaveAttribute('aria-expanded', 'false');
    expect(screen.getByText(/^No\. El generador son cuatro pasos/)).not.toBeVisible();
  });

  /*
   * A FAQ is read by comparing answers, not by working through them in order,
   * so opening one does not shut the last. This is the part of "accordion" that
   * was a deliberate choice rather than a given.
   */
  it('lets more than one answer stay open at a time', async () => {
    mockLanding({ status: 'anonymous' });
    renderLanding();

    const questions = within(
      screen.getByRole('region', { name: 'Lo que la gente pregunta antes de empezar' }),
    ).getAllByRole('button');

    await userEvent.click(questions[0]);
    await userEvent.click(questions[2]);

    expect(questions[0]).toHaveAttribute('aria-expanded', 'true');
    expect(questions[2]).toHaveAttribute('aria-expanded', 'true');
  });

  it('points each collapsed question at the panel it opens', async () => {
    mockLanding({ status: 'anonymous' });
    renderLanding();

    const question = screen.getByRole('button', { name: /¿Cuánto cuesta\?/ });
    const panelId = question.getAttribute('aria-controls');
    expect(panelId).toBeTruthy();

    await userEvent.click(question);

    const panel = document.getElementById(panelId!);
    expect(panel).not.toBeNull();
    expect(panel).toHaveTextContent('[COMPLETAR PRECIO]');
  });

  it('has no accessibility violations with an answer open', async () => {
    mockLanding({ status: 'anonymous' });
    const { container } = renderLanding();

    await userEvent.click(screen.getByRole('button', { name: /¿Qué pasa con mis datos\?/ }));

    expect(await axe(container)).toHaveNoViolations();
  });

  /*
   * The landing is where a visitor decides whether to trust FORMA. Every claim
   * below was on the page at some point and none of them is backed by anything
   * in this repository — Withings is the only integration with a real gateway,
   * there is no pricing, and no measurement supports a precision figure. This
   * test is the guard that stops them coming back.
   */
  it.each([
    /\+?10[.,]?000/,
    /98\s*%/,
    /14 días/,
    /Garmin/i,
    /Apple Health/i,
    /Google Fit/i,
    /propietario/i,
  ])('never claims %s', (claim) => {
    mockLanding({ status: 'anonymous' });
    const { container } = renderLanding();

    expect(container.textContent ?? '').not.toMatch(claim);
  });

  it('marks the missing price as a placeholder instead of inventing one', () => {
    mockLanding({ status: 'anonymous' });
    renderLanding();

    expect(screen.getByText(/\[COMPLETAR PRECIO\]/)).toBeInTheDocument();
  });

  it('labels the illustrative figures as samples', () => {
    mockLanding({ status: 'anonymous' });
    const { container } = renderLanding();

    expect(container.textContent).toMatch(/Datos de ejemplo/);
    expect(container.textContent).toMatch(/FORMA no está asociada a Mercadona/);
  });

  it('offers real login and registration actions to anonymous visitors', () => {
    mockLanding({ status: 'anonymous' });
    renderLanding();

    expect(screen.getByLabelText('Correo electrónico')).toHaveAttribute('autocomplete', 'email');
    expect(screen.getByLabelText('Contraseña')).toHaveAttribute('autocomplete', 'current-password');
    expect(screen.getByRole('link', { name: 'Crear cuenta' })).toHaveAttribute('href', '/register');
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
      user: { id: 'user-1', email: 'persona@example.com', role: 'USER' as const },
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
      screen.getByRole('region', { name: 'De cuatro preguntas a la cesta de la compra' }),
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
          status === 'authenticated'
            ? { id: 'user-1', email: 'persona@example.com', role: 'USER' as const }
            : undefined,
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
