import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-found',
  imports: [RouterLink],
  template: `
    <section class="container state">
      <h1>Page introuvable</h1>
      <p>L’adresse saisie n’existe pas ou la page a été déplacée.</p>
      <a class="btn btn-primary" routerLink="/offres">Voir les offres</a>
    </section>
  `,
  styles: `
    .state {
      max-width: 36rem;
      padding-block: 4rem;
    }
    h1 {
      margin-bottom: 0.75rem;
      font-size: var(--step-3);
    }
  `,
})
export class NotFound {}
