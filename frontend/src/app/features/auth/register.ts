import { Component, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { registerErrorMessage } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';
import { safeReturnUrl } from '../../core/return-url';
import { showError } from '../../shared/form-utils';

type Field = 'firstName' | 'lastName' | 'email' | 'phone' | 'password';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './auth.scss',
})
export class Register {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(NonNullableFormBuilder);

  readonly returnUrl = input<string | undefined>();

  protected readonly form = this.fb.group({
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    phone: ['', Validators.maxLength(30)],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
  });

  protected readonly submitted = signal(false);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly passwordVisible = signal(false);

  protected readonly showError = (name: Field) => showError(this.form.controls[name], this.submitted());

  protected submit(): void {
    this.submitted.set(true);
    if (this.form.invalid || this.loading()) {
      return;
    }
    this.error.set(null);
    this.loading.set(true);
    const { firstName, lastName, email, phone, password } = this.form.getRawValue();
    this.auth
      .register({
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        email: email.trim(),
        password,
        ...(phone.trim() ? { phone: phone.trim() } : {}),
      })
      .subscribe({
        next: () => {
          void this.router.navigateByUrl(safeReturnUrl(this.returnUrl(), '/offres'));
        },
        error: (err: unknown) => {
          this.loading.set(false);
          this.error.set(registerErrorMessage(err));
        },
      });
  }
}
