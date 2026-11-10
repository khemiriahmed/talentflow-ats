import { DatePipe } from '@angular/common';
import { HttpClient, httpResource } from '@angular/common/http';
import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { RouterLink } from '@angular/router';
import { genericErrorMessage } from '../../core/api-error';
import { API_BASE } from '../../core/config';
import { Application, Page } from '../../core/models';
import { StageTrack } from '../../shared/stage-track';

@Component({
  selector: 'app-my-applications',
  imports: [DatePipe, RouterLink, StageTrack],
  templateUrl: './my-applications.html',
  styleUrl: './my-applications.scss',
})
export class MyApplications {
  private readonly http = inject(HttpClient);

  protected readonly applications = httpResource<Page<Application>>(() => ({
    url: `${API_BASE}/applications/mine`,
    params: { size: 50 },
  }));

  protected readonly pending = signal<Application | null>(null);
  protected readonly withdrawing = signal(false);
  protected readonly withdrawError = signal<string | null>(null);

  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('confirmDialog');

  protected askWithdraw(application: Application): void {
    this.pending.set(application);
    this.withdrawError.set(null);
    this.dialog().nativeElement.showModal();
  }

  protected cancelWithdraw(): void {
    this.dialog().nativeElement.close();
    this.pending.set(null);
  }

  protected confirmWithdraw(): void {
    const application = this.pending();
    if (!application || this.withdrawing()) {
      return;
    }
    this.withdrawing.set(true);
    this.http.delete(`${API_BASE}/applications/${application.id}`).subscribe({
      next: () => {
        this.withdrawing.set(false);
        this.cancelWithdraw();
        this.applications.reload();
      },
      error: (err: unknown) => {
        this.withdrawing.set(false);
        this.withdrawError.set(genericErrorMessage(err));
      },
    });
  }
}
