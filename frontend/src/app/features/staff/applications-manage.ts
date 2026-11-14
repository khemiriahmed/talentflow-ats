import { DatePipe } from '@angular/common';
import { HttpClient, httpResource } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { genericErrorMessage } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';
import { API_BASE, PAGE_SIZE } from '../../core/config';
import { STAGE_STEP_LABELS, STAGE_OPTIONS } from '../../core/labels';
import { Application, JobOffer, Page, Stage } from '../../core/models';

@Component({
  selector: 'app-applications-manage',
  imports: [DatePipe, FormsModule, RouterLink],
  templateUrl: './applications-manage.html',
  styleUrl: './applications-manage.scss',
})
export class ApplicationsManage {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);

  protected readonly stages = STAGE_OPTIONS;
  protected readonly stageLabels = STAGE_STEP_LABELS;

  protected readonly keyword = signal('');
  protected readonly jobOfferId = signal('');
  protected readonly stage = signal<'' | Stage>('');
  protected readonly tag = signal('');
  protected readonly page = signal(0);
  protected readonly exporting = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly canExport = computed(() => {
    const role = this.auth.user()?.role;
    return role === 'HR' || role === 'ADMIN';
  });

  protected readonly jobs = httpResource<Page<JobOffer>>(() => ({
    url: `${API_BASE}/jobs/manage`,
    params: { size: 100 },
  }));

  protected readonly applications = httpResource<Page<Application>>(() => ({
    url: `${API_BASE}/applications`,
    params: this.params(),
  }));

  protected search(): void {
    this.page.set(0);
    this.applications.reload();
  }

  protected goTo(page: number): void {
    this.page.set(page);
    this.applications.reload();
  }

  protected exportCsv(): void {
    if (this.exporting()) return;
    this.exporting.set(true);
    this.error.set(null);
    this.http
      .get(`${API_BASE}/applications/export`, { params: this.params(), responseType: 'blob' })
      .subscribe({
        next: (blob) => {
          this.exporting.set(false);
          const url = URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = `candidatures-${new Date().toISOString().slice(0, 10)}.csv`;
          link.click();
          URL.revokeObjectURL(url);
        },
        error: (err: unknown) => {
          this.exporting.set(false);
          this.error.set(genericErrorMessage(err));
        },
      });
  }

  private params(): Record<string, string | number> {
    const params: Record<string, string | number> = { page: this.page(), size: PAGE_SIZE };
    if (this.keyword().trim()) params['q'] = this.keyword().trim();
    if (this.jobOfferId()) params['jobOfferId'] = this.jobOfferId();
    if (this.stage()) params['stage'] = this.stage();
    if (this.tag().trim()) params['tag'] = this.tag().trim();
    return params;
  }
}
