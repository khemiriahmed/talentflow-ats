import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { authResponse } from '../testing/fixtures';
import { AuthService } from './auth.service';

const REFRESH_KEY = 'talentflow.refresh';

describe('AuthService', () => {
  let auth: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([{ path: '**', children: [] }])],
    });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('starts a session on login and keeps only the refresh token in storage', () => {
    auth.login('camille@exemple.fr', 'Password123!').subscribe();
    const request = http.expectOne('/api/v1/auth/login');
    expect(request.request.body).toEqual({ email: 'camille@exemple.fr', password: 'Password123!' });
    request.flush(authResponse());

    expect(auth.user()?.firstName).toBe('Camille');
    expect(auth.isAuthenticated()).toBe(true);
    expect(auth.isCandidate()).toBe(true);
    expect(auth.token).toBe('access-1');
    expect(localStorage.getItem(REFRESH_KEY)).toBe('refresh-1');
    expect(JSON.stringify({ ...localStorage })).not.toContain('access-1');
  });

  it('sends users to the right home page for their role', () => {
    auth.login('a@b.fr', 'x').subscribe();
    http.expectOne('/api/v1/auth/login').flush(authResponse('HR'));
    expect(auth.isStaff()).toBe(true);
    expect(auth.homePath()).toBe('/equipe');
  });

  it('does not call the server on startup when there is no refresh token', () => {
    let completed = false;
    auth.restoreSession().subscribe({ complete: () => (completed = true) });
    http.expectNone('/api/v1/auth/refresh');
    expect(completed).toBe(true);
    expect(auth.isAuthenticated()).toBe(false);
  });

  it('restores the session from the refresh token on startup', () => {
    localStorage.setItem(REFRESH_KEY, 'refresh-1');
    auth.restoreSession().subscribe();
    const request = http.expectOne('/api/v1/auth/refresh');
    expect(request.request.body).toEqual({ refreshToken: 'refresh-1' });
    request.flush(authResponse('CANDIDATE', 'access-2', 'refresh-2'));

    expect(auth.isAuthenticated()).toBe(true);
    expect(auth.token).toBe('access-2');
    expect(localStorage.getItem(REFRESH_KEY)).toBe('refresh-2');
  });

  it('forgets an invalid refresh token instead of failing the startup', () => {
    localStorage.setItem(REFRESH_KEY, 'expired');
    let completed = false;
    auth.restoreSession().subscribe({ complete: () => (completed = true) });
    http.expectOne('/api/v1/auth/refresh').flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(completed).toBe(true);
    expect(auth.isAuthenticated()).toBe(false);
    expect(localStorage.getItem(REFRESH_KEY)).toBeNull();
  });

  it('shares a single refresh request between parallel callers', () => {
    localStorage.setItem(REFRESH_KEY, 'refresh-1');
    const tokens: string[] = [];
    auth.refreshAccessToken().subscribe((token) => tokens.push(token));
    auth.refreshAccessToken().subscribe((token) => tokens.push(token));

    http.expectOne('/api/v1/auth/refresh').flush(authResponse('CANDIDATE', 'access-9', 'refresh-9'));
    expect(tokens).toEqual(['access-9', 'access-9']);
  });

  it('clears everything on logout', () => {
    auth.login('a@b.fr', 'x').subscribe();
    http.expectOne('/api/v1/auth/login').flush(authResponse());
    auth.logout();

    expect(auth.user()).toBeNull();
    expect(auth.token).toBeNull();
    expect(auth.hasRefreshToken()).toBe(false);
  });
});
