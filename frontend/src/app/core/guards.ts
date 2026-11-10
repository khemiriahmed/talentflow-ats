import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { Role } from './models';

/** Réserve une route aux rôles donnés ; sinon : connexion (anonyme) ou page d'accueil du rôle. */
export const roleGuard =
  (...roles: Role[]): CanActivateFn =>
  (_route, state) => {
    const auth = inject(AuthService);
    const router = inject(Router);
    const user = auth.user();
    if (!user) {
      return router.createUrlTree(['/connexion'], { queryParams: { returnUrl: state.url } });
    }
    return roles.includes(user.role) ? true : router.createUrlTree([auth.homePath()]);
  };

/** Pages réservées aux visiteurs (connexion, inscription) : un utilisateur connecté est redirigé. */
export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.isAuthenticated() ? router.createUrlTree([auth.homePath()]) : true;
};
