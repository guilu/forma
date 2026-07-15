import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider } from '../../theme/ThemeContext';
import { NotificationProvider } from '../../components/NotificationProvider';
import { ApiRequestError } from '../../api/client';
import { getProfile, updateProfileFields, type UserProfile } from '../../api/profile';
import { ProfileSection } from './ProfileSection';

vi.mock('../../api/profile', () => ({
  getProfile: vi.fn(),
  updateProfileFields: vi.fn(),
}));

const getProfileMock = vi.mocked(getProfile);
const updateProfileFieldsMock = vi.mocked(updateProfileFields);

const PROFILE: UserProfile = {
  name: 'Usuario FORMA',
  email: 'usuario@forma.app',
  birthDate: '1990-05-12',
  heightCm: 178,
  activityLevel: 'MODERATE',
  mainGoal: 'COMPOSICION',
  unitPreferences: { weightUnit: 'KG', heightUnit: 'CM', distanceUnit: 'KM', energyUnit: 'KCAL' },
};

/** First-run default profile (FOR-107 Edge Cases): no profile fields saved yet. */
const DEFAULT_PROFILE: UserProfile = {
  unitPreferences: { weightUnit: 'KG', heightUnit: 'CM', distanceUnit: 'KM', energyUnit: 'KCAL' },
};

function renderProfileSection() {
  return render(
    <ThemeProvider>
      <NotificationProvider>
        <ProfileSection />
      </NotificationProvider>
    </ThemeProvider>,
  );
}

describe('ProfileSection', () => {
  afterEach(() => {
    window.localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
    vi.clearAllMocks();
  });

  it('shows a loading state while the profile is being fetched', () => {
    getProfileMock.mockReturnValue(new Promise(() => {}));
    renderProfileSection();

    expect(screen.getByRole('status')).toHaveTextContent(/cargando/i);
  });

  it('loads and displays the real profile from GET /api/v1/profile', async () => {
    getProfileMock.mockResolvedValue(PROFILE);
    renderProfileSection();

    expect(
      await screen.findByRole('heading', { name: 'Perfil y preferencias', level: 2 }),
    ).toBeInTheDocument();
    expect(screen.getByText('Usuario FORMA')).toBeInTheDocument();
    expect(screen.getByText('usuario@forma.app')).toBeInTheDocument();
    expect(screen.getByText('1990-05-12')).toBeInTheDocument();
    expect(screen.getByText('178 cm')).toBeInTheDocument();
    expect(screen.getByText('Moderado')).toBeInTheDocument();
    expect(screen.getByText('Composición corporal')).toBeInTheDocument();
  });

  it('pre-fills the form with defaults on a first-run profile instead of blank/undefined fields', async () => {
    getProfileMock.mockResolvedValue(DEFAULT_PROFILE);
    renderProfileSection();

    await screen.findByRole('heading', { name: 'Perfil y preferencias', level: 2 });
    expect(screen.getAllByText('No especificado').length).toBeGreaterThan(0);
    expect(screen.queryByText('undefined')).not.toBeInTheDocument();
  });

  it('renders "Editar perfil" as a real, enabled entry point (no "Próximamente" placeholder)', async () => {
    getProfileMock.mockResolvedValue(PROFILE);
    renderProfileSection();

    const editButton = await screen.findByRole('button', { name: 'Editar perfil' });
    expect(editButton).toBeEnabled();
    expect(screen.queryByText('Próximamente')).not.toBeInTheDocument();
  });

  it('renders ErrorState with a working retry when the profile fetch fails', async () => {
    getProfileMock.mockRejectedValueOnce(new ApiRequestError(500, 'Backend unavailable'));
    getProfileMock.mockResolvedValueOnce(PROFILE);
    renderProfileSection();

    expect(await screen.findByRole('alert')).toBeInTheDocument();
    const retryButton = screen.getByRole('button', { name: 'Reintentar' });

    await userEvent.click(retryButton);

    expect(await screen.findByText('Usuario FORMA')).toBeInTheDocument();
    expect(getProfileMock).toHaveBeenCalledTimes(2);
  });

  it('opens an editable form pre-filled with the current values, saves, and re-renders with the new values plus feedback', async () => {
    const user = userEvent.setup();
    getProfileMock.mockResolvedValue(PROFILE);
    updateProfileFieldsMock.mockResolvedValue({ ...PROFILE, name: 'Ada Lovelace' });
    renderProfileSection();

    await user.click(await screen.findByRole('button', { name: 'Editar perfil' }));

    const dialog = screen.getByRole('dialog');
    const nameInput = within(dialog).getByLabelText('Nombre');
    expect(nameInput).toHaveValue('Usuario FORMA');

    await user.clear(nameInput);
    await user.type(nameInput, 'Ada Lovelace');
    await user.click(within(dialog).getByRole('button', { name: 'Guardar' }));

    await waitFor(() => {
      expect(updateProfileFieldsMock).toHaveBeenCalledWith(
        expect.objectContaining({ name: 'Ada Lovelace' }),
      );
    });
    expect(await screen.findByText('Ada Lovelace')).toBeInTheDocument();
    expect(await screen.findByText('Perfil actualizado.')).toBeInTheDocument();
  });

  it('shows a save validation error close to the offending field, without losing other entered values', async () => {
    const user = userEvent.setup();
    getProfileMock.mockResolvedValue(PROFILE);
    updateProfileFieldsMock.mockRejectedValue(
      new ApiRequestError(400, 'Validation failed', 'VALIDATION_ERROR', [
        { field: 'heightCm', message: 'must be greater than 0' },
      ]),
    );
    renderProfileSection();

    await user.click(await screen.findByRole('button', { name: 'Editar perfil' }));
    const dialog = screen.getByRole('dialog');

    const nameInput = within(dialog).getByLabelText('Nombre');
    await user.clear(nameInput);
    await user.type(nameInput, 'Nombre sin guardar');
    // A value that passes the frontend's own basic positive-number check but
    // is rejected by the backend -- exercises the ApiRequestError.details ->
    // field mapping, not the client-side check.
    const heightInput = within(dialog).getByLabelText('Altura (cm)');
    await user.clear(heightInput);
    await user.type(heightInput, '170');
    await user.click(within(dialog).getByRole('button', { name: 'Guardar' }));

    expect(await within(dialog).findByText('must be greater than 0')).toBeInTheDocument();
    // The user's other, unsaved edits are preserved -- not reset by the failed save.
    expect(nameInput).toHaveValue('Nombre sin guardar');
  });

  it('discards unsaved changes when the edit form is cancelled', async () => {
    const user = userEvent.setup();
    getProfileMock.mockResolvedValue(PROFILE);
    renderProfileSection();

    await user.click(await screen.findByRole('button', { name: 'Editar perfil' }));
    const dialog = screen.getByRole('dialog');
    const nameInput = within(dialog).getByLabelText('Nombre');
    await user.clear(nameInput);
    await user.type(nameInput, 'Cambio sin guardar');
    await user.click(within(dialog).getByRole('button', { name: 'Cancelar' }));

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(screen.getByText('Usuario FORMA')).toBeInTheDocument();
    expect(updateProfileFieldsMock).not.toHaveBeenCalled();
  });

  it('wires the "Tema" row to a working theme toggle (FOR-62, unchanged by FOR-119)', async () => {
    const user = userEvent.setup();
    getProfileMock.mockResolvedValue(PROFILE);
    renderProfileSection();

    await screen.findByRole('heading', { name: 'Perfil y preferencias', level: 2 });
    const label = screen.getByText('Tema');
    const description = label.nextElementSibling;
    expect(description).toHaveTextContent('Sistema (Oscuro)');

    await user.click(screen.getByRole('button', { name: 'Claro' }));

    expect(description).toHaveTextContent('Claro');
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
  });
});
