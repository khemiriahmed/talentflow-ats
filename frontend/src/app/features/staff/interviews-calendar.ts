import { DatePipe } from '@angular/common';
import { HttpClient, httpResource } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { genericErrorMessage } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';
import { API_BASE } from '../../core/config';
import { INTERVIEW_MODE_LABELS, INTERVIEW_STATUS_LABELS } from '../../core/labels';
import { Interview } from '../../core/models';

@Component({
  selector: 'app-interviews-calendar',
  imports: [DatePipe, FormsModule, RouterLink],
  templateUrl: './interviews-calendar.html',
  styleUrl: './interviews-calendar.scss',
})
export class InterviewsCalendar {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);

  protected readonly modeLabels = INTERVIEW_MODE_LABELS;
  protected readonly statusLabels = INTERVIEW_STATUS_LABELS;
  protected readonly rangeDays = signal(14);
  protected readonly anchor = signal(this.startOfToday());
  protected readonly error = signal<string | null>(null);

  protected readonly canManage = computed(() => {
    const role = this.auth.user()?.role;
    return role === 'HR' || role === 'ADMIN';
  });

  protected readonly rangeLabel = computed(() => {
    const from = this.anchor();
    const to = new Date(from.getTime() + (this.rangeDays() - 1) * 86_400_000);
    return `${from.toLocaleDateString('fr-FR', { day: 'numeric', month: 'long' })} – ${to.toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' })}`;
  });

  protected readonly interviews = httpResource<Interview[]>(() => ({
    url: `${API_BASE}/interviews`,
    params: {
      from: this.anchor().toISOString(),
      to: new Date(this.anchor().getTime() + this.rangeDays() * 86_400_000).toISOString(),
    },
  }));

  protected readonly days = computed(() => {
    const groups = new Map<string, Interview[]>();
    for (const interview of this.interviews.value() ?? []) {
      const date = new Date(interview.startAt);
      const key = date.toISOString().slice(0, 10);
      const list = groups.get(key) ?? [];
      list.push(interview);
      groups.set(key, list);
    }
    return [...groups.entries()]
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([date, list]) => ({
        date,
        interviews: list.sort((a, b) => a.startAt.localeCompare(b.startAt)),
      }));
  });

  protected shift(direction: number): void {
    this.anchor.update((date) => new Date(date.getTime() + direction * this.rangeDays() * 86_400_000));
  }

  protected goToday(): void {
    this.anchor.set(this.startOfToday());
  }

  protected changeRange(days: number): void {
    this.rangeDays.set(days);
  }

  protected cancel(interview: Interview): void {
    if (!window.confirm('Annuler cet entretien ?')) return;
    this.error.set(null);
    this.http.patch(`${API_BASE}/interviews/${interview.id}/cancel`, {}).subscribe({
      next: () => this.interviews.reload(),
      error: (err: unknown) => this.error.set(genericErrorMessage(err)),
    });
  }

  private startOfToday(): Date {
    const now = new Date();
    return new Date(now.getFullYear(), now.getMonth(), now.getDate());
  }
}
