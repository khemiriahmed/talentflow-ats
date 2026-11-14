import { AuthResponse, Role, User } from '../core/models';

export function userFixture(role: Role = 'CANDIDATE', overrides: Partial<User> = {}): User {
  return {
    id: 7,
    email: 'camille@exemple.fr',
    firstName: 'Camille',
    lastName: 'Martin',
    phone: null,
    linkedinUrl: null,
    photoUrl: null,
    role,
    enabled: true,
    emailVerified: true,
    createdAt: '2026-11-01T10:00:00Z',
    ...overrides,
  };
}

export function authResponse(role: Role = 'CANDIDATE', access = 'access-1', refresh = 'refresh-1'): AuthResponse {
  return {
    accessToken: access,
    refreshToken: refresh,
    tokenType: 'Bearer',
    expiresInSeconds: 900,
    user: userFixture(role),
  };
}
