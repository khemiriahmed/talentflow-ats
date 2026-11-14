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
    path: 'mot-de-passe-oublie',
    title: 'Mot de passe oublié',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/forgot-password').then((m) => m.ForgotPassword),
  },
  {
    path: 'reinitialiser-mot-de-passe',
    title: 'Nouveau mot de passe',
    loadComponent: () => import('./features/auth/reset-password').then((m) => m.ResetPassword),
  },
  {
    path: 'verifier-email',
    title: 'Vérification de l’adresse email',
    loadComponent: () => import('./features/auth/verify-email').then((m) => m.VerifyEmail),
  },
  {
    path: 'mes-candidatures',
    title: 'Mes candidatures',
    canActivate: [roleGuard('CANDIDATE')],
    loadComponent: () =>
      import('./features/applications/my-applications').then((m) => m.MyApplications),
  },
  {
    path: 'profil',
    title: 'Mon profil',
    canActivate: [roleGuard('CANDIDATE', 'HR', 'MANAGER', 'ADMIN')],
    loadComponent: () => import('./features/account/profile').then((m) => m.Profile),
  },
  {
    path: 'notifications',
    title: 'Notifications',
    canActivate: [roleGuard('CANDIDATE', 'HR', 'MANAGER', 'ADMIN')],
    loadComponent: () => import('./features/notifications/notifications').then((m) => m.Notifications),
  },
  {
    path: 'equipe',
    canActivate: [roleGuard('HR', 'MANAGER', 'ADMIN')],
    loadComponent: () => import('./features/staff/staff-layout').then((m) => m.StaffLayout),
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'candidatures',
      },
      {
        path: 'tableau-de-bord',
        title: 'Tableau de bord',
        canActivate: [roleGuard('HR', 'ADMIN')],
        loadComponent: () => import('./features/staff/dashboard').then((m) => m.Dashboard),
      },
      {
        path: 'offres',
        title: 'Gestion des offres',
        loadComponent: () => import('./features/staff/jobs-manage').then((m) => m.JobsManage),
      },
      {
        path: 'offres/nouvelle',
        title: 'Nouvelle offre',
        canActivate: [roleGuard('HR', 'ADMIN')],
        loadComponent: () => import('./features/staff/job-form').then((m) => m.JobForm),
      },
      {
        path: 'offres/:id/modifier',
        title: 'Modifier une offre',
        canActivate: [roleGuard('HR', 'ADMIN')],
        loadComponent: () => import('./features/staff/job-form').then((m) => m.JobForm),
      },
      {
        path: 'candidatures',
        title: 'Candidatures',
        loadComponent: () => import('./features/staff/applications-manage').then((m) => m.ApplicationsManage),
      },
      {
        path: 'candidatures/:id',
        title: 'Dossier de candidature',
        loadComponent: () => import('./features/staff/application-detail').then((m) => m.ApplicationDetail),
      },
      {
        path: 'pipeline',
        title: 'Pipeline de recrutement',
        loadComponent: () => import('./features/staff/pipeline').then((m) => m.Pipeline),
      },
      {
        path: 'entretiens',
        title: 'Calendrier des entretiens',
        loadComponent: () => import('./features/staff/interviews-calendar').then((m) => m.InterviewsCalendar),
      },
    ],
  },
  {
    path: 'admin',
    canActivate: [roleGuard('ADMIN')],
    loadComponent: () => import('./features/admin/admin-layout').then((m) => m.AdminLayout),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'utilisateurs' },
      {
        path: 'utilisateurs',
        title: 'Utilisateurs',
        loadComponent: () => import('./features/admin/users-admin').then((m) => m.UsersAdmin),
      },
      {
        path: 'audit',
        title: 'Journal d’audit',
        loadComponent: () => import('./features/admin/audit-log').then((m) => m.AuditLogPage),
      },
    ],
  },
  {
    path: '**',
    title: 'Page introuvable',
    loadComponent: () => import('./features/common/not-found').then((m) => m.NotFound),
  },
];
