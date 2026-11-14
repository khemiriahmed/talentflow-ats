import { HttpClient, httpResource } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { genericErrorMessage } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';
import { API_BASE } from '../../core/config';
import { STAGE_STEP_LABELS, STAGE_STEPS } from '../../core/labels';
import { Application, JobOffer, Page, Stage } from '../../core/models';

@Component({
  selector: 'app-pipeline',
  imports: [FormsModule, RouterLink],
  templateUrl: './pipeline.html',
  styleUrl: './pipeline.scss',
})
export class Pipeline {
  private readonly auth = inject(AuthService);
  private readonly http = inject(HttpClient);

  protected readonly stages = STAGE_STEPS;
  protected readonly stageLabels = STAGE_STEP_LABELS;
  protected readonly jobOfferId = signal('');
  protected readonly keyword = signal('');
  protected readonly dragging = signal<number | null>(null);
  protected readonly overStage = signal<Stage | null>(null);
  protected readonly error = signal<string | null>(null);

  protected readonly canMove = computed(() => {
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

  protected readonly grouped = computed(() => {
    const groups = new Map<Stage, Application[]>();
    for (const stage of this.stages) groups.set(stage, []);
    for (const application of this.applications.value()?.content ?? []) {
      if (application.currentStage === 'REJECTED') continue;
      groups.get(application.currentStage)?.push(application);
    }
    return groups;
  });

  protected readonly rejected = computed(
    () => (this.applications.value()?.content ?? []).filter((item) => item.currentStage === 'REJECTED'),
  );

  protected refresh(): void {
    this.applications.reload();
  }

  protected dragStart(event: DragEvent, application: Application): void {
    if (!this.canMove()) {
      event.preventDefault();
      return;
    }
    event.dataTransfer?.setData('text/plain', String(application.id));
    if (event.dataTransfer) event.dataTransfer.effectAllowed = 'move';
    this.dragging.set(application.id);
  }

  protected dragEnd(): void {
    this.dragging.set(null);
    this.overStage.set(null);
  }

  protected allowDrop(event: DragEvent, stage: Stage): void {
    if (!this.canMove()) return;
    event.preventDefault();
    this.overStage.set(stage);
  }

  protected drop(event: DragEvent, stage: Stage): void {
    event.preventDefault();
    this.overStage.set(null);
    this.dragging.set(null);
    if (!this.canMove()) return;
    const raw = event.dataTransfer?.getData('text/plain');
    const id = Number(raw);
    if (!raw || Number.isNaN(id)) return;
    const application = (this.applications.value()?.content ?? []).find((item) => item.id === id);
    if (!application || application.currentStage === stage) return;
    this.move(application, stage);
  }

  private move(application: Application, stage: Stage): void {
    this.error.set(null);
    this.http
      .patch(`${API_BASE}/applications/${application.id}/stage`, {
        stage,
        reason: 'Déplacement dans le pipeline',
      })
      .subscribe({
        next: () => this.applications.reload(),
        error: (err: unknown) => {
          this.error.set(genericErrorMessage(err));
          this.applications.reload();
        },
      });
  }

  private params(): Record<string, string | number> {
    const params: Record<string, string | number> = { page: 0, size: 200 };
    if (this.jobOfferId()) params['jobOfferId'] = this.jobOfferId();
    if (this.keyword().trim()) params['q'] = this.keyword().trim();
    return params;
  }
}
