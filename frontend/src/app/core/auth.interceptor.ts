import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { API_BASE } from './config';

const AUTH_ENDPOINTS = `${API_BASE}/auth/`;

/** Ajoute le jeton aux appels API ; sur 401, renouvelle le jeton une fois puis rejoue la requête. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);

  if (!req.url.startsWith(API_BASE) || req.url.startsWith(AUTH_ENDPOINTS)) {
    return next(req);
  }

  const withToken = (request: HttpRequest<unknown>, token: string | null) =>
    token ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request;

  return next(withToken(req, auth.token)).pipe(
    catchError((error: unknown) => {
      const unauthorized = error instanceof HttpErrorResponse && error.status === 401;
      if (!unauthorized || !auth.hasRefreshToken()) {
        return throwError(() => error);
      }
      return auth.refreshAccessToken().pipe(
        switchMap((token) => next(withToken(req, token))),
        catchError((refreshError: unknown) => {
          auth.expireSession();
          return throwError(() => refreshError);
        }),
      );
    }),
  );
};
