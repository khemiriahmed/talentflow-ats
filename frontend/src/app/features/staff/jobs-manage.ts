import { DatePipe } from '@angular/common';
import { HttpClient, httpResource } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { genericErrorMessage } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';
import { API_BASE, PAGE_SIZE } from '../../core/config';
import { formatSalary } from '../../core/format';
import {
  CONTRACT_LABELS,
  CONTRACT_OPTIONS,
  JOB_STATUS_LABELS,
  JOB_STATUS_OPTIONS,
} from '../../core/labels';
import { JobOffer, JobStatus, Page } from '../../core/models';

@Component({
  selector: 'app-jobs-manage',
  imports: [DatePipe, FormsModule, RouterLink],
  templateUrl: './jobs-manage.html',
  styleUrl: './jobs-manage.scss',
})
export class JobsManage {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);

  protected readonly contracts = CONTRACT_OPTIONS;
  protected readonly statuses = JOB_STATUS_OPTIONS;
  protected readonly statusLabels = JOB_STATUS_LABELS;
  protected readonly contractLabels = CONTRACT_LABELS;

  protected readonly keyword = signal('');
  protected readonly location = signal('');
  protected readonly contractType = signal('');
  protected readonly status = signal<'' | JobStatus>('');
  protected readonly page = signal(0);
  protected readonly error = signal<string | null>(null);
  protected readonly busy = signal<number | null>(null);

  protected readonly canManage = computed(() => {
    const role = this.auth.user()?.role;
    return role === 'HR' || role === 'ADMIN';
  });

  protected readonly jobs = httpResource<Page<JobOffer>>(() => ({
    url: `${API_BASE}/jobs/manage`,
    params: this.params(),
  }));

  protected search(): void {
    this.page.set(0);
    this.jobs.reload();
  }

  protected goTo(page: number): void {
    this.page.set(page);
    this.jobs.reload();
  }

  protected salary(job: JobOffer): string {
    return formatSalary(job.salaryMin, job.salaryMax) ?? '—';
  }

  protected statusClass(status: JobStatus): string {
    switch (status) {
      case 'PUBLISHED':
        return 'badge';
      case 'DRAFT':
        return 'badge warn';
      case 'CLOSED':
        return 'badge neutral';
      default:
        return 'badge neutral';
    }
  }

  protected publish(job: JobOffer): void {
    this.changeStatus(job, 'PUBLISHED');
  }

  protected close(job: JobOffer): void {
    this.changeStatus(job, 'CLOSED');
  }

  protected archive(job: JobOffer): void {
    if (!window.confirm('Archiver cette offre ? Cette action est définitive.')) return;
    this.changeStatus(job, 'ARCHIVED');
  }

  protected remove(job: JobOffer): void {
    if (!window.confirm(`Supprimer définitivement l’offre « ${job.title} » ?`)) return;
    this.busy.set(job.id);
    this.error.set(null);
    this.http.delete(`${API_BASE}/jobs/${job.id}`).subscribe({
      next: () => {
        this.busy.set(null);
        this.jobs.reload();
      },
      error: (err: unknown) => {
        this.busy.set(null);
        this.error.set(genericErrorMessage(err));
      },
    });
  }

  private changeStatus(job: JobOffer, status: JobStatus): void {
    this.busy.set(job.id);
    this.error.set(null);
    this.http.patch(`${API_BASE}/jobs/${job.id}/status`, { status }).subscribe({
      next: () => {
        this.busy.set(null);
        this.jobs.reload();
      },
      error: (err: unknown) => {
        this.busy.set(null);
        this.error.set(genericErrorMessage(err));
      },
    });
  }

  private params(): Record<string, string | number> {
    const params: Record<string, string | number> = { page: this.page(), size: PAGE_SIZE };
    if (this.keyword().trim()) params['keyword'] = this.keyword().trim();
    if (this.location().trim()) params['location'] = this.location().trim();
    if (this.contractType()) params['contractType'] = this.contractType();
    if (this.status()) params['status'] = this.status();
    return params;
  }
}
