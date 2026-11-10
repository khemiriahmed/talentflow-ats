import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { AuthService } from './core/auth.service';
import { Role } from './core/models';
import { authResponse } from './testing/fixtures';

describe('App shell', () => {
  async function render(role?: Role): Promise<HTMLElement> {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    if (role) {
      TestBed.inject(AuthService).login('a@b.fr', 'x').subscribe();
      TestBed.inject(HttpTestingController).expectOne('/api/v1/auth/login').flush(authResponse(role));
    }
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  const links = (root: HTMLElement) =>
    Array.from(root.querySelectorAll('nav a, .account a')).map((a) => a.textContent?.trim());

  it('invites a visitor to sign in or create an account', async () => {
    const root = await render();
    expect(links(root)).toEqual(['Offres', 'Se connecter', 'Créer un compte']);
    expect(root.querySelector('.skip-link')?.getAttribute('href')).toBe('#contenu');
  });

  it('shows "Mes candidatures" to a candidate, with their name and a sign-out button', async () => {
    const root = await render('CANDIDATE');
    expect(links(root)).toEqual(['Offres', 'Mes candidatures']);
    expect(root.querySelector('.who')?.textContent).toContain('Camille Martin');
    expect(root.querySelector('.account button')?.textContent).toContain('Se déconnecter');
  });

  it('shows the staff space to staff only', async () => {
    const root = await render('HR');
    expect(links(root)).toEqual(['Offres', 'Espace équipe']);
  });
});
