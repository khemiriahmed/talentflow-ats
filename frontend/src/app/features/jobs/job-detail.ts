import { DatePipe } from '@angular/common';
import { HttpErrorResponse, httpResource } from '@angular/common/http';
import { Component, computed, effect, inject, input } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';
import { API_BASE } from '../../core/config';
import { AuthService } from '../../core/auth.service';
import { formatSalary, isDeadlinePassed } from '../../core/format';
import { CONTRACT_LABELS } from '../../core/labels';
import { PublicJob } from '../../core/models';
import { Icon } from '../../shared/icon';
import { ApplyForm } from '../applications/apply-form';

@Component({
  selector: 'app-job-detail',
  imports: [RouterLink, DatePipe, Icon, ApplyForm],
  templateUrl: './job-detail.html',
  styleUrl: './job-detail.scss',
})
export class JobDetail {
  readonly id = input.required<string>();

  protected readonly auth = inject(AuthService);
  private readonly title = inject(Title);

  protected readonly job = httpResource<PublicJob>(() => `${API_BASE}/jobs/${this.id()}`);

  protected readonly notFound = computed(() => {
    const error = this.job.error();
    return error instanceof HttpErrorResponse && error.status === 404;
  });
  protected readonly closed = computed(() => isDeadlinePassed(this.job.value()?.deadline ?? null));
  protected readonly returnUrl = computed(() => `/offres/${this.id()}`);

  constructor() {
    effect(() => {
      const job = this.job.value();
      if (job) {
        this.title.setTitle(`${job.title} - TalentFlow`);
      }
    });
  }

  protected contractLabel(job: PublicJob): string {
    return CONTRACT_LABELS[job.contractType];
  }

  protected salary(job: PublicJob): string | null {
    return formatSalary(job.salaryMin, job.salaryMax);
  }
}
