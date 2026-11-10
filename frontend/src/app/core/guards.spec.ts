import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { authResponse } from '../testing/fixtures';
import { AuthService } from './auth.service';
import { guestGuard, roleGuard } from './guards';
import { Role } from './models';

describe('route guards', () => {
  let router: Router;
  let auth: AuthService;
  let http: HttpTestingController;

  const route = {} as ActivatedRouteSnapshot;
  const state = (url: string) => ({ url }) as RouterStateSnapshot;

  function signInAs(role: Role): void {
    auth.login('a@b.fr', 'x').subscribe();
    http.expectOne('/api/v1/auth/login').flush(authResponse(role));
  }

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    router = TestBed.inject(Router);
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  const run = (guard: ReturnType<typeof roleGuard>, url = '/equipe') =>
    TestBed.runInInjectionContext(() => guard(route, state(url)));

  it('sends an anonymous visitor to the login page and remembers the target', () => {
    const result = run(roleGuard('HR')) as UrlTree;
    expect(router.serializeUrl(result)).toBe('/connexion?returnUrl=%2Fequipe');
  });

  it('lets the allowed roles through', () => {
    signInAs('HR');
    expect(run(roleGuard('HR', 'ADMIN'))).toBe(true);
  });

  it('sends a candidate away from staff pages, back to the job offers', () => {
    signInAs('CANDIDATE');
    const result = run(roleGuard('HR', 'MANAGER', 'ADMIN')) as UrlTree;
    expect(router.serializeUrl(result)).toBe('/offres');
  });

  it('sends staff away from candidate pages, back to their own space', () => {
    signInAs('MANAGER');
    const result = run(roleGuard('CANDIDATE'), '/mes-candidatures') as UrlTree;
    expect(router.serializeUrl(result)).toBe('/equipe');
  });

  it('keeps signed-in users away from the login and sign-up pages', () => {
    expect(TestBed.runInInjectionContext(() => guestGuard(route, state('/connexion')))).toBe(true);
    signInAs('CANDIDATE');
    const result = TestBed.runInInjectionContext(() => guestGuard(route, state('/connexion'))) as UrlTree;
    expect(router.serializeUrl(result)).toBe('/offres');
  });
});
