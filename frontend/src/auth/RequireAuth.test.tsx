import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { useAuth } from './AuthContext';
import { RequireAuth } from './RequireAuth';

vi.mock('./AuthContext', () => ({ useAuth: vi.fn() }));

describe('RequireAuth', () => {
  it('shows loading without rendering protected content', () => {
    vi.mocked(useAuth).mockReturnValue({ status: 'loading', bootstrapError: false } as ReturnType<
      typeof useAuth
    >);
    render(
      <MemoryRouter>
        <RequireAuth>
          <div>Privado</div>
        </RequireAuth>
      </MemoryRouter>,
    );
    expect(screen.getByText('Comprobando tu sesión...')).toBeInTheDocument();
    expect(screen.queryByText('Privado')).not.toBeInTheDocument();
  });

  it('redirects anonymous users and preserves their destination', () => {
    vi.mocked(useAuth).mockReturnValue({ status: 'anonymous' } as ReturnType<typeof useAuth>);
    render(
      <MemoryRouter initialEntries={['/progreso']}>
        <Routes>
          <Route
            path="/progreso"
            element={
              <RequireAuth>
                <div>Privado</div>
              </RequireAuth>
            }
          />
          <Route path="/login" element={<Destination />} />
        </Routes>
      </MemoryRouter>,
    );
    expect(screen.getByText('/progreso')).toBeInTheDocument();
  });
});

function Destination() {
  const location = useLocation();
  const state = location.state as { from?: { pathname?: string } } | null;
  return <div>{state?.from?.pathname}</div>;
}
