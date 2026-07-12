import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { AboutSection } from './AboutSection';
import { APP_VERSION } from './profileData';

describe('AboutSection', () => {
  it('shows the static version as read-only and legal links as inert', () => {
    render(<AboutSection />);

    expect(screen.getByText('Versión')).toBeInTheDocument();
    expect(screen.getByText(APP_VERSION)).toBeInTheDocument();
    expect(screen.getByText('Términos y condiciones')).toBeInTheDocument();
    expect(screen.getByText('Política de privacidad')).toBeInTheDocument();
    // Only the two unsupported rows carry the inert marker — version does not.
    expect(screen.getAllByText('Próximamente')).toHaveLength(2);
  });
});
