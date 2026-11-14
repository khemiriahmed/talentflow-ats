import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-forgot-password',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './forgot-password.html',
  styleUrl: './auth.scss',
})
export class ForgotPassword {
  private readonly auth = inject(AuthService);
  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
  });
  protected readonly submitted = signal(false);
  protected readonly loading = signal(false);
  protected readonly sent = signal(false);

  protected submit(): void {
    this.submitted.set(true);
    if (this.form.invalid || this.loading()) {
      return;
    }
    this.loading.set(true);
    this.auth.forgotPassword(this.form.getRawValue().email.trim()).subscribe({
      next: () => {
        this.loading.set(false);
        this.sent.set(true);
      },
      error: () => {
        this.loading.set(false);
        this.sent.set(true);
      },
    });
  }
}
