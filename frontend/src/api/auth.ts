import { apiClient } from './client';

export interface AuthUser {
  readonly id: string;
  readonly email: string;
}

export interface AuthCredentials {
  readonly email: string;
  readonly password: string;
}

function postCredentials(path: string, credentials: AuthCredentials): Promise<AuthUser> {
  return apiClient.request<AuthUser>(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(credentials),
  });
}

export const login = (credentials: AuthCredentials) =>
  postCredentials('/api/v1/auth/login', credentials);
export const register = (credentials: AuthCredentials) =>
  postCredentials('/api/v1/auth/register', credentials);
export const getCurrentUser = () => apiClient.request<AuthUser>('/api/v1/auth/me');
export const logout = () => apiClient.request<void>('/api/v1/auth/logout', { method: 'POST' });
