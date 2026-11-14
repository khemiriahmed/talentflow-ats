import { HttpClient, httpResource } from '@angular/common/http';
import { Component, effect, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { genericErrorMessage, toApiError } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';
import { API_BASE } from '../../core/config';
import { CONTRACT_OPTIONS } from '../../core/labels';
import { JobOffer, JobOfferPayload, StaffMember } from '../../core/models';

@Component({
  selector: 'app-job-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './job-form.html',
  styleUrl: './job-form.scss',
})
export class JobForm {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(NonNullableFormBuilder);

  readonly id = input<string | undefined>();

  protected readonly contracts = CONTRACT_OPTIONS;
  protected readonly submitted = signal(false);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly fieldErrors = signal<Record<string, string>>({});

  protected readonly job = httpResource<JobOffer>(() =>
    this.id() ? `${API_BASE}/jobs/manage/${this.id()}` : undefined,
  );

  protected readonly staff = httpResource<StaffMember[]>(() => `${API_BASE}/staff`);

  protected readonly canAssignManager = (() => {
    const role = this.auth.user()?.role;
    return role === 'HR' || role === 'ADMIN';
  })();

  protected readonly form = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    description: ['', [Validators.required, Validators.maxLength(10000)]],
    department: ['', Validators.maxLength(100)],
    location: ['', Validators.maxLength(100)],
    contractType: ['CDI', Validators.required],
    salaryMin: [''],
    salaryMax: [''],
    deadline: [''],
    managerId: [''],
  });

  constructor() {
    effect(() => {
      const job = this.job.value();
      if (!job) return;
      this.form.patchValue(
        {
          title: job.title,
          description: job.description,
          department: job.department ?? '',
          location: job.location ?? '',
          contractType: job.contractType,
          salaryMin: job.salaryMin != null ? String(job.salaryMin) : '',
          salaryMax: job.salaryMax != null ? String(job.salaryMax) : '',
          deadline: job.deadline ?? '',
          managerId: job.managerId != null ? String(job.managerId) : '',
        },
        { emitEvent: false },
      );
    });
  }

  protected fieldError(name: string): string | null {
    return this.fieldErrors()[name] ?? null;
  }

  protected submit(): void {
    this.submitted.set(true);
    this.error.set(null);
    this.fieldErrors.set({});
    if (this.form.invalid || this.saving()) {
      return;
    }
    const raw = this.form.getRawValue();
    const salaryMin = raw.salaryMin.trim() ? Number(raw.salaryMin) : null;
    const salaryMax = raw.salaryMax.trim() ? Number(raw.salaryMax) : null;
    if (salaryMin !== null && salaryMax !== null && salaryMin > salaryMax) {
      this.error.set('Le salaire minimum ne peut pas dépasser le salaire maximum.');
      return;
    }
    if ((salaryMin !== null && Number.isNaN(salaryMin)) || (salaryMax !== null && Number.isNaN(salaryMax))) {
      this.error.set('Les montants doivent être des nombres.');
      return;
    }
    const payload: JobOfferPayload = {
      title: raw.title.trim(),
      description: raw.description.trim(),
      department: raw.department.trim() || null,
      location: raw.location.trim() || null,
      contractType: raw.contractType as JobOfferPayload['contractType'],
      salaryMin,
      salaryMax,
      deadline: raw.deadline || null,
      managerId: raw.managerId ? Number(raw.managerId) : null,
    };
    this.saving.set(true);
    const request = this.id()
      ? this.http.put<JobOffer>(`${API_BASE}/jobs/${this.id()}`, payload)
      : this.http.post<JobOffer>(`${API_BASE}/jobs`, payload);
    request.subscribe({
      next: () => {
        this.saving.set(false);
        void this.router.navigate(['/equipe/offres']);
      },
      error: (err: unknown) => {
        this.saving.set(false);
        const apiError = toApiError(err);
        this.fieldErrors.set(apiError?.fieldErrors ?? {});
        this.error.set(apiError?.message ?? genericErrorMessage(err));
      },
    });
  }
}
