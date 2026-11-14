import { DatePipe } from '@angular/common';
import { HttpClient, httpResource } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { genericErrorMessage, toApiError } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';
import { API_BASE, PAGE_SIZE } from '../../core/config';
import { ROLE_LABELS } from '../../core/labels';
import { Page, Role, User } from '../../core/models';

@Component({
  selector: 'app-users-admin',
  imports: [DatePipe, ReactiveFormsModule],
  templateUrl: './users-admin.html',
  styleUrl: './users-admin.scss',
})
export class UsersAdmin {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly roleLabels = ROLE_LABELS;
  protected readonly roles: Role[] = ['CANDIDATE', 'HR', 'MANAGER', 'ADMIN'];
  protected readonly page = signal(0);
  protected readonly keyword = signal('');
  protected readonly showCreate = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly saved = signal<string | null>(null);
  protected readonly busy = signal<number | null>(null);

  protected readonly users = httpResource<Page<User>>(() => ({
    url: `${API_BASE}/admin/users`,
    params: { page: this.page(), size: PAGE_SIZE, sort: 'createdAt,desc' },
  }));

  protected readonly filtered = computed(() => {
    const term = this.keyword().trim().toLowerCase();
    const content = this.users.value()?.content ?? [];
    if (!term) return content;
    return content.filter(
      (user) =>
        user.email.toLowerCase().includes(term) ||
        `${user.firstName} ${user.lastName}`.toLowerCase().includes(term),
    );
  });

  protected readonly currentUserId = computed(() => this.auth.user()?.id);

  protected readonly form = this.fb.group({
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    phone: ['', Validators.maxLength(30)],
    role: ['HR' as Role, Validators.required],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
  });

  protected readonly submitted = signal(false);

  protected toggleCreate(): void {
    this.showCreate.update((value) => !value);
    this.submitted.set(false);
    this.error.set(null);
    this.saved.set(null);
  }

  protected create(): void {
    this.submitted.set(true);
    this.error.set(null);
    this.saved.set(null);
    if (this.form.invalid) return;
    const raw = this.form.getRawValue();
    this.http
      .post<User>(`${API_BASE}/admin/users`, {
        email: raw.email.trim(),
        password: raw.password,
        firstName: raw.firstName.trim(),
        lastName: raw.lastName.trim(),
        phone: raw.phone.trim() || null,
        role: raw.role,
      })
      .subscribe({
        next: () => {
          this.saved.set('Le compte a été créé.');
          this.form.reset({ firstName: '', lastName: '', email: '', phone: '', role: 'HR', password: '' });
          this.showCreate.set(false);
          this.users.reload();
        },
        error: (err: unknown) => {
          const apiError = toApiError(err);
          this.error.set(apiError?.message ?? genericErrorMessage(err));
        },
      });
  }

  protected changeRole(user: User, role: Role): void {
    if (role === user.role) return;
    this.busy.set(user.id);
    this.error.set(null);
    this.http.patch<User>(`${API_BASE}/admin/users/${user.id}/role`, { role }).subscribe({
      next: () => {
        this.busy.set(null);
        this.users.reload();
      },
      error: (err: unknown) => {
        this.busy.set(null);
        this.error.set(genericErrorMessage(err));
        this.users.reload();
      },
    });
  }

  protected toggleEnabled(user: User): void {
    const enabled = !user.enabled;
    if (!enabled && !window.confirm(`Désactiver le compte de ${user.firstName} ${user.lastName} ?`)) return;
    this.busy.set(user.id);
    this.error.set(null);
    this.http.patch<User>(`${API_BASE}/admin/users/${user.id}/enabled`, { enabled }).subscribe({
      next: () => {
        this.busy.set(null);
        this.users.reload();
      },
      error: (err: unknown) => {
        this.busy.set(null);
        this.error.set(genericErrorMessage(err));
      },
    });
  }

  protected goTo(page: number): void {
    this.page.set(page);
    this.users.reload();
  }
}
