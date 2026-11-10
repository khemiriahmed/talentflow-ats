import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { authResponse } from '../testing/fixtures';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

describe('authInterceptor', () => {
  let client: HttpClient;
  let http: HttpTestingController;
  let auth: AuthService;

  function signIn(): void {
    auth.login('a@b.fr', 'x').subscribe();
    http.expectOne('/api/v1/auth/login').flush(authResponse('CANDIDATE', 'access-1', 'refresh-1'));
  }

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([{ path: '**', children: [] }]),
      ],
    });
    client = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => http.verify());

  it('adds the access token to API calls', () => {
    signIn();
    client.get('/api/v1/applications/mine').subscribe();
    const request = http.expectOne('/api/v1/applications/mine');
    expect(request.request.headers.get('Authorization')).toBe('Bearer access-1');
    request.flush({});
  });

  it('never adds the token to authentication endpoints or to other hosts', () => {
    signIn();
    client.post('/api/v1/auth/refresh', {}).subscribe();
    expect(http.expectOne('/api/v1/auth/refresh').request.headers.has('Authorization')).toBe(false);

    client.get('https://example.org/data').subscribe();
    expect(http.expectOne('https://example.org/data').request.headers.has('Authorization')).toBe(false);
  });

  it('renews the token on a 401 and replays the request once', () => {
    signIn();
    let result: unknown;
    client.get('/api/v1/applications/mine').subscribe((value) => (result = value));

    http.expectOne('/api/v1/applications/mine').flush({}, { status: 401, statusText: 'Unauthorized' });
    const refresh = http.expectOne('/api/v1/auth/refresh');
    expect(refresh.request.body).toEqual({ refreshToken: 'refresh-1' });
    refresh.flush(authResponse('CANDIDATE', 'access-2', 'refresh-2'));

    const retry = http.expectOne('/api/v1/applications/mine');
    expect(retry.request.headers.get('Authorization')).toBe('Bearer access-2');
    retry.flush({ ok: true });
    expect(result).toEqual({ ok: true });
  });

  it('ends the session when the token cannot be renewed', () => {
    signIn();
    const expire = vi.spyOn(auth, 'expireSession').mockImplementation(() => undefined);
    let status = 0;
    client.get('/api/v1/applications/mine').subscribe({ error: (e: HttpErrorResponse) => (status = e.status) });

    http.expectOne('/api/v1/applications/mine').flush({}, { status: 401, statusText: 'Unauthorized' });
    http.expectOne('/api/v1/auth/refresh').flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(expire).toHaveBeenCalledOnce();
    expect(status).toBe(401);
  });

  it('lets an anonymous 401 through without trying to renew anything', () => {
    let status = 0;
    client.get('/api/v1/applications/mine').subscribe({ error: (e: HttpErrorResponse) => (status = e.status) });
    http.expectOne('/api/v1/applications/mine').flush({}, { status: 401, statusText: 'Unauthorized' });

    http.expectNone('/api/v1/auth/refresh');
    expect(status).toBe(401);
  });

  it('does not touch other errors', () => {
    signIn();
    let status = 0;
    client.get('/api/v1/applications/mine').subscribe({ error: (e: HttpErrorResponse) => (status = e.status) });
    http.expectOne('/api/v1/applications/mine').flush({}, { status: 500, statusText: 'Server Error' });

    http.expectNone('/api/v1/auth/refresh');
    expect(status).toBe(500);
  });
});
