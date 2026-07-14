import { afterEach, describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider } from '../../theme/ThemeContext';
import { ProfileSection } from './ProfileSection';
import { MOCK_PROFILE } from './profileData';

function renderProfileSection() {
  return render(
    <ThemeProvider>
      <ProfileSection />
    </ThemeProvider>,
  );
}

describe('ProfileSection', () => {
  afterEach(() => {
    window.localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
  });

  it('shows the personal profile summary fields', () => {
    renderProfileSection();

    // Rendered as <h2> (FOR-112): direct sibling of SettingsPage's <h1>.
    expect(
      screen.getByRole('heading', { name: 'Perfil y preferencias', level: 2 }),
    ).toBeInTheDocument();
    expect(screen.getByText(MOCK_PROFILE.name)).toBeInTheDocument();
    expect(screen.getByText(MOCK_PROFILE.email)).toBeInTheDocument();
    expect(screen.getByText(MOCK_PROFILE.birthDate)).toBeInTheDocument();
    expect(screen.getByText(MOCK_PROFILE.sex)).toBeInTheDocument();
    expect(screen.getByText(`${MOCK_PROFILE.heightCm} cm`)).toBeInTheDocument();
    expect(screen.getByText(MOCK_PROFILE.activityLevel)).toBeInTheDocument();
    expect(screen.getByText(MOCK_PROFILE.mainGoal)).toBeInTheDocument();
  });

  it('renders "Editar perfil" as a visible but disabled entry point', () => {
    renderProfileSection();

    const editButton = screen.getByRole('button', { name: 'Editar perfil' });
    expect(editButton).toBeDisabled();
    expect(screen.getAllByText('Próximamente').length).toBeGreaterThan(0);
  });

  it('wires the "Tema" row to a working theme toggle (FOR-62)', async () => {
    const user = userEvent.setup();
    renderProfileSection();

    const label = screen.getByText('Tema');
    const description = label.nextElementSibling;
    // No stored preference/no system signal -> "system" mode resolves to dark.
    expect(description).toHaveTextContent('Sistema (Oscuro)');
    expect(screen.getByRole('button', { name: 'Sistema' })).toHaveAttribute('aria-pressed', 'true');

    await user.click(screen.getByRole('button', { name: 'Claro' }));

    expect(description).toHaveTextContent('Claro');
    expect(screen.getByRole('button', { name: 'Claro' })).toHaveAttribute('aria-pressed', 'true');
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
    // The row is a live control now, not FOR-58's inert placeholder.
    expect(screen.queryByText('Oscuro (predeterminado)')).not.toBeInTheDocument();
  });
});
