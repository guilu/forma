import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ProfileSection } from './ProfileSection';
import { MOCK_PROFILE } from './profileData';

describe('ProfileSection', () => {
  it('shows the personal profile summary fields', () => {
    render(<ProfileSection />);

    expect(screen.getByText(MOCK_PROFILE.name)).toBeInTheDocument();
    expect(screen.getByText(MOCK_PROFILE.email)).toBeInTheDocument();
    expect(screen.getByText(MOCK_PROFILE.birthDate)).toBeInTheDocument();
    expect(screen.getByText(MOCK_PROFILE.sex)).toBeInTheDocument();
    expect(screen.getByText(`${MOCK_PROFILE.heightCm} cm`)).toBeInTheDocument();
    expect(screen.getByText(MOCK_PROFILE.activityLevel)).toBeInTheDocument();
    expect(screen.getByText(MOCK_PROFILE.mainGoal)).toBeInTheDocument();
  });

  it('renders "Editar perfil" as a visible but disabled entry point', () => {
    render(<ProfileSection />);

    const editButton = screen.getByRole('button', { name: 'Editar perfil' });
    expect(editButton).toBeDisabled();
    expect(screen.getAllByText('Próximamente').length).toBeGreaterThan(0);
  });

  it('marks the theme preference (FOR-62) as an inert entry point', () => {
    render(<ProfileSection />);

    expect(screen.getByText('Tema')).toBeInTheDocument();
    expect(screen.getByText('Oscuro (predeterminado)')).toBeInTheDocument();
  });
});
