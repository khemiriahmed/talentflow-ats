import { Component, OnInit, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-verify-email',
  imports: [RouterLink],
  template: `
    <section class="auth">
      <h1>Vérification de l’adresse email</h1>
      @switch (state()) {
        @case ('loading') {
          <p class="status" role="status">Vérification en cours…</p>
        }
        @case ('ok') {
          <p class="notice" role="status">
            Votre adresse email est maintenant vérifiée. Merci !
          </p>
          <p class="alt">
            <a routerLink="/profil">Voir mon profil</a> ·
            <a routerLink="/offres">Parcourir les offres</a>
          </p>
        }
        @default {
          <p class="alert" role="alert">
            Ce lien de vérification est invalide ou a expiré. Connectez-vous puis demandez un nouveau lien
            depuis votre profil.
          </p>
          <p class="alt"><a routerLink="/connexion">Se connecter</a></p>
        }
      }
    </section>
  `,
  styleUrl: './auth.scss',
})
export class VerifyEmail implements OnInit {
  private readonly auth = inject(AuthService);
  readonly token = input<string | undefined>();
  protected readonly state = signal<'loading' | 'ok' | 'error'>('loading');

  ngOnInit(): void {
    const token = this.token();
    if (!token) {
      this.state.set('error');
      return;
    }
    this.auth.verifyEmail(token).subscribe({
      next: () => this.state.set('ok'),
      error: () => this.state.set('error'),
    });
  }
}
