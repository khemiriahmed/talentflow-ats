import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { genericErrorMessage, toApiError } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';
import { ROLE_LABELS } from '../../core/labels';
import { User } from '../../core/models';

@Component({
  selector: 'app-profile',
  imports: [ReactiveFormsModule],
  templateUrl: './profile.html',
  styleUrl: './profile.scss',
})
export class Profile {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly user = this.auth.user;
  protected readonly roleLabels = ROLE_LABELS;
  protected readonly form = this.fb.group({
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    phone: ['', Validators.maxLength(30)],
    linkedinUrl: ['', [Validators.pattern(/^https?:\/\/.+/)]],
    photoUrl: ['', [Validators.pattern(/^https?:\/\/.+/)]],
  });

  protected readonly submitted = signal(false);
  protected readonly saving = signal(false);
  protected readonly saved = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly verificationSent = signal(false);
  protected readonly deleting = signal(false);

  constructor() {
    const user = this.auth.user();
    if (user) {
      this.form.patchValue({
        firstName: user.firstName,
        lastName: user.lastName,
        phone: user.phone ?? '',
        linkedinUrl: user.linkedinUrl ?? '',
        photoUrl: user.photoUrl ?? '',
      });
    }
  }

  protected initials(user: User): string {
    return (user.firstName.charAt(0) + user.lastName.charAt(0)).toUpperCase();
  }

  protected save(): void {
    this.submitted.set(true);
    this.saved.set(false);
    if (this.form.invalid || this.saving()) {
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    const raw = this.form.getRawValue();
    this.auth
      .updateProfile({
        firstName: raw.firstName.trim(),
        lastName: raw.lastName.trim(),
        phone: raw.phone.trim() || null,
        linkedinUrl: raw.linkedinUrl.trim() || null,
        photoUrl: raw.photoUrl.trim() || null,
      })
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.saved.set(true);
        },
        error: (err: unknown) => {
          this.saving.set(false);
          this.error.set(toApiError(err)?.message ?? genericErrorMessage(err));
        },
      });
  }

  protected resendVerification(): void {
    const user = this.auth.user();
    if (!user) return;
    this.verificationSent.set(false);
    this.auth.resendVerification(user.email).subscribe({
      next: () => this.verificationSent.set(true),
      error: () => this.verificationSent.set(true),
    });
  }

  protected deleteAccount(): void {
    if (this.deleting()) return;
    this.deleting.set(true);
    this.auth.deleteAccount().subscribe({
      next: () => this.router.navigate(['/offres'], { queryParams: { deleted: 1 } }),
      error: (err: unknown) => {
        this.deleting.set(false);
        this.error.set(genericErrorMessage(err));
      },
    });
  }
}
