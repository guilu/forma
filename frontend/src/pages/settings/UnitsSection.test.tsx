import { afterEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ApiRequestError } from '../../api/client';
import { getProfile, type UserProfile } from '../../api/profile';
import { UnitsSection } from './UnitsSection';

vi.mock('../../api/profile', () => ({
  getProfile: vi.fn(),
}));

const getProfileMock = vi.mocked(getProfile);

const PROFILE: UserProfile = {
  unitPreferences: { weightUnit: 'KG', heightUnit: 'CM', distanceUnit: 'KM', energyUnit: 'KCAL' },
};

describe('UnitsSection', () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it('shows a loading state while the persisted preference is being fetched', () => {
    getProfileMock.mockReturnValue(new Promise(() => {}));
    render(<UnitsSection />);

    expect(screen.getByRole('status')).toHaveTextContent(/cargando/i);
  });

  it('reflects the real persisted unit preference from GET /api/v1/profile', async () => {
    getProfileMock.mockResolvedValue(PROFILE);
    render(<UnitsSection />);

    expect(await screen.findByRole('heading', { name: 'Unidades', level: 2 })).toBeInTheDocument();
    expect(screen.getByText('Peso')).toBeInTheDocument();
    expect(screen.getByText('Kilogramos (kg)')).toBeInTheDocument();
    expect(screen.getByText('Altura')).toBeInTheDocument();
    expect(screen.getByText('Centímetros (cm)')).toBeInTheDocument();
    expect(screen.getByText('Distancia')).toBeInTheDocument();
    expect(screen.getByText('Kilómetros (km)')).toBeInTheDocument();
    expect(screen.getByText('Energía')).toBeInTheDocument();
    expect(screen.getByText('Kilocalorías (kcal)')).toBeInTheDocument();
    // Read-only, not an unsupported flow -- no inert marker (FOR-107 exposes a
    // single supported value per dimension, so there is nothing to select).
    expect(screen.queryByText('Próximamente')).not.toBeInTheDocument();
  });

  it('renders ErrorState with a working retry when the fetch fails', async () => {
    getProfileMock.mockRejectedValueOnce(new ApiRequestError(500, 'Backend unavailable'));
    getProfileMock.mockResolvedValueOnce(PROFILE);
    render(<UnitsSection />);

    expect(await screen.findByRole('alert')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByText('Kilogramos (kg)')).toBeInTheDocument();
    expect(getProfileMock).toHaveBeenCalledTimes(2);
  });
});
