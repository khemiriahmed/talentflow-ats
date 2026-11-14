import { Component, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { showError } from '../../shared/form-utils';

@Component({
  selector: 'app-reset-password',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './reset-password.html',
  styleUrl: './auth.scss',
})
export class ResetPassword {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(NonNullableFormBuilder);

  readonly token = input<string | undefined>();

  protected readonly form = this.fb.group({
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
    confirm: ['', Validators.required],
  });
  protected readonly submitted = signal(false);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly showError = (name: 'password' | 'confirm') =>
    showError(this.form.controls[name], this.submitted());

  protected submit(): void {
    this.submitted.set(true);
    if (this.form.invalid || this.loading()) {
      return;
    }
    if (!this.token()) {
      this.error.set('Ce lien de réinitialisation est incomplet. Demandez un nouveau lien.');
      return;
    }
    const { password, confirm } = this.form.getRawValue();
    if (password !== confirm) {
      this.error.set('Les deux mots de passe ne correspondent pas.');
      return;
    }
    this.error.set(null);
    this.loading.set(true);
    this.auth.resetPassword(this.token()!, password).subscribe({
      next: () => {
        void this.router.navigate(['/connexion'], { queryParams: { reset: 1 } });
      },
      error: (err: unknown) => {
        this.loading.set(false);
        this.error.set(
          'Ce lien est invalide ou a expiré. Demandez un nouveau lien de réinitialisation.',
        );
      },
    });
  }
}
