import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { useAuth } from './AuthContext';
import { RequireAdmin } from './RequireAdmin';

vi.mock('./AuthContext', () => ({ useAuth: vi.fn() }));

function mockUser(role: 'USER' | 'ADMIN') {
  vi.mocked(useAuth).mockReturnValue({
    status: 'authenticated',
    user: { id: 'u1', email: 'a@b.c', role },
  } as ReturnType<typeof useAuth>);
}

function renderGuard() {
  render(
    <MemoryRouter>
      <RequireAdmin>
        <div>Panel</div>
      </RequireAdmin>
    </MemoryRouter>,
  );
}

describe('RequireAdmin', () => {
  it('renders the panel for an admin', () => {
    mockUser('ADMIN');
    renderGuard();
    expect(screen.getByText('Panel')).toBeInTheDocument();
  });

  it('hides the panel from a non-admin who typed the URL', () => {
    mockUser('USER');
    renderGuard();
    expect(screen.queryByText('Panel')).not.toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /no tienes acceso/i })).toBeInTheDocument();
  });
});
