import { DecimalPipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { Component, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { API_BASE } from '../../core/config';
import { STAGE_STEP_LABELS } from '../../core/labels';
import { DashboardStats, Granularity, JobOffer, Page } from '../../core/models';

function isoDate(date: Date): string {
  return date.toISOString().slice(0, 10);
}

@Component({
  selector: 'app-dashboard',
  imports: [DecimalPipe, FormsModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard {
  protected readonly stageLabels = STAGE_STEP_LABELS;
  private readonly today = new Date();
  private readonly defaultFrom = new Date(this.today.getTime() - 89 * 86_400_000);

  protected from = signal(isoDate(this.defaultFrom));
  protected to = signal(isoDate(this.today));
  protected jobOfferId = signal('');
  protected granularity = signal<'' | Granularity>('');

  protected readonly jobs = httpResource<Page<JobOffer>>(() => ({
    url: `${API_BASE}/jobs/manage`,
    params: { size: 100 },
  }));

  protected readonly stats = httpResource<DashboardStats>(() => ({
    url: `${API_BASE}/dashboard/stats`,
    params: this.params(),
  }));

  protected readonly maxTime = computed(() => {
    const points = this.stats.value()?.applicationsOverTime ?? [];
    return Math.max(1, ...points.map((point) => point.count));
  });

  protected readonly maxSource = computed(() => {
    const sources = this.stats.value()?.bySource ?? [];
    return Math.max(1, ...sources.map((source) => source.count));
  });

  protected readonly maxJob = computed(() => {
    const jobs = this.stats.value()?.topJobs ?? [];
    return Math.max(1, ...jobs.map((job) => job.count));
  });

  protected barWidth(value: number, max: number): string {
    return `${Math.max(2, Math.round((value / max) * 100))}%`;
  }

  protected reset(): void {
    this.from.set(isoDate(new Date(this.today.getTime() - 89 * 86_400_000)));
    this.to.set(isoDate(this.today));
    this.jobOfferId.set('');
    this.granularity.set('');
  }

  protected readonly granularities = [
    { value: '', label: 'Automatique' },
    { value: 'DAY', label: 'Par jour' },
    { value: 'WEEK', label: 'Par semaine' },
    { value: 'MONTH', label: 'Par mois' },
  ] as const;

  private params(): Record<string, string> {
    const params: Record<string, string> = {
      from: new Date(this.from() + 'T00:00:00').toISOString(),
      to: new Date(new Date(this.to() + 'T00:00:00').getTime() + 86_400_000).toISOString(),
    };
    if (this.jobOfferId()) params['jobOfferId'] = this.jobOfferId();
    if (this.granularity()) params['granularity'] = this.granularity();
    return params;
  }
}
