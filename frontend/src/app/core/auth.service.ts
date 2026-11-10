import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, map, of, shareReplay, tap, throwError } from 'rxjs';
import { API_BASE } from './config';
import { AuthResponse, RegisterPayload, User } from './models';

const REFRESH_KEY = 'talentflow.refresh';

/**
 * Session : le jeton d'accès reste en mémoire ; seul le jeton de renouvellement est conservé
 * (localStorage) pour survivre à un rechargement de page. Compromis connu : un cookie httpOnly
 * posé par le backend serait plus résistant au XSS.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly currentUser = signal<User | null>(null);
  private accessToken: string | null = null;
  private refreshInFlight$: Observable<string> | null = null;

  readonly user = this.currentUser.asReadonly();
  readonly isAuthenticated = computed(() => this.currentUser() !== null);
  readonly isCandidate = computed(() => this.currentUser()?.role === 'CANDIDATE');
  readonly isStaff = computed(() => {
    const role = this.currentUser()?.role;
    return role === 'HR' || role === 'MANAGER' || role === 'ADMIN';
  });

  get token(): string | null {
    return this.accessToken;
  }

  hasRefreshToken(): boolean {
    return this.readRefreshToken() !== null;
  }

  /** Page d'accueil selon le rôle. */
  homePath(): string {
    return this.isStaff() ? '/equipe' : '/offres';
  }

  login(email: string, password: string): Observable<User> {
    return this.http
      .post<AuthResponse>(`${API_BASE}/auth/login`, { email, password })
      .pipe(map((response) => this.startSession(response)));
  }

  register(payload: RegisterPayload): Observable<User> {
    return this.http
      .post<AuthResponse>(`${API_BASE}/auth/register`, payload)
      .pipe(map((response) => this.startSession(response)));
  }

  logout(): void {
    this.clearSession();
    void this.router.navigateByUrl('/offres');
  }

  /** Session expirée : on revient à la connexion en gardant la page visée. */
  expireSession(): void {
    const target = this.router.url;
    this.clearSession();
    void this.router.navigate(['/connexion'], { queryParams: { returnUrl: target, expired: 1 } });
  }

  /** Une seule requête de renouvellement à la fois, même si plusieurs appels échouent en parallèle. */
  refreshAccessToken(): Observable<string> {
    const refreshToken = this.readRefreshToken();
    if (!refreshToken) {
      return throwError(() => new Error('No refresh token'));
    }
    if (!this.refreshInFlight$) {
      this.refreshInFlight$ = this.http
        .post<AuthResponse>(`${API_BASE}/auth/refresh`, { refreshToken })
        .pipe(
          tap((response) => this.startSession(response)),
          map((response) => response.accessToken),
          finalize(() => (this.refreshInFlight$ = null)),
          shareReplay({ bufferSize: 1, refCount: false }),
        );
    }
    return this.refreshInFlight$;
  }

  /** Appelé au démarrage : retrouve la session si un jeton de renouvellement existe. */
  restoreSession(): Observable<void> {
    if (!this.hasRefreshToken()) {
      return of(undefined);
    }
    return this.refreshAccessToken().pipe(
      map(() => undefined),
      catchError(() => {
        this.clearSession();
        return of(undefined);
      }),
    );
  }

  private startSession(response: AuthResponse): User {
    this.accessToken = response.accessToken;
    this.writeRefreshToken(response.refreshToken);
    this.currentUser.set(response.user);
    return response.user;
  }

  private clearSession(): void {
    this.accessToken = null;
    this.refreshInFlight$ = null;
    this.currentUser.set(null);
    try {
      localStorage.removeItem(REFRESH_KEY);
    } catch {
      /* stockage indisponible : rien à nettoyer */
    }
  }

  private readRefreshToken(): string | null {
    try {
      return localStorage.getItem(REFRESH_KEY);
    } catch {
      return null;
    }
  }

  private writeRefreshToken(token: string): void {
    try {
      localStorage.setItem(REFRESH_KEY, token);
    } catch {
      /* stockage indisponible : la session ne survivra pas au rechargement */
    }
  }
}
