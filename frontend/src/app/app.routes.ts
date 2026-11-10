import { Routes } from '@angular/router';
import { guestGuard, roleGuard } from './core/guards';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'offres' },
  {
    path: 'offres',
    title: 'Offres ouvertes',
    loadComponent: () => import('./features/jobs/jobs-list').then((m) => m.JobsList),
  },
  {
    path: 'offres/:id',
    title: 'Offre d’emploi',
    loadComponent: () => import('./features/jobs/job-detail').then((m) => m.JobDetail),
  },
  {
    path: 'connexion',
    title: 'Connexion',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login').then((m) => m.Login),
  },
  {
    path: 'inscription',
    title: 'Créer un compte',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/register').then((m) => m.Register),
  },
  {
    path: 'mes-candidatures',
    title: 'Mes candidatures',
    canActivate: [roleGuard('CANDIDATE')],
    loadComponent: () =>
      import('./features/applications/my-applications').then((m) => m.MyApplications),
  },
  {
    path: 'equipe',
    title: 'Espace équipe',
    canActivate: [roleGuard('HR', 'MANAGER', 'ADMIN')],
    loadComponent: () => import('./features/staff/staff-home').then((m) => m.StaffHome),
  },
  {
    path: '**',
    title: 'Page introuvable',
    loadComponent: () => import('./features/common/not-found').then((m) => m.NotFound),
  },
];
