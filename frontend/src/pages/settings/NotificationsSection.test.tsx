import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { NotificationsSection } from './NotificationsSection';

describe('NotificationsSection', () => {
  it('previews notification categories with disabled toggles (FOR-63 not built yet)', () => {
    render(<NotificationsSection />);

    // Rendered as <h2> (FOR-112): direct sibling of SettingsPage's <h1>.
    expect(screen.getByRole('heading', { name: 'Notificaciones', level: 2 })).toBeInTheDocument();
    expect(screen.getByText('Recordatorios de entrenamientos')).toBeInTheDocument();

    const toggle = screen.getByRole('checkbox', { name: 'Recordatorios de entrenamientos' });
    expect(toggle).toBeDisabled();
    expect(toggle).toBeChecked();
  });
});
