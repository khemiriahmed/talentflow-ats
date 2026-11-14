import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-admin-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="container">
      <nav class="subnav" aria-label="Administration">
        <a routerLink="/admin/utilisateurs" routerLinkActive="active">Utilisateurs</a>
        <a routerLink="/admin/audit" routerLinkActive="active">Journal d’audit</a>
      </nav>
    </div>
    <router-outlet />
  `,
})
export class AdminLayout {}
