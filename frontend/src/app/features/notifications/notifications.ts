import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { genericErrorMessage } from '../../core/api-error';
import { NotificationService } from '../../core/notification.service';
import { Notification, Page } from '../../core/models';

@Component({
  selector: 'app-notifications',
  imports: [DatePipe, RouterLink],
  templateUrl: './notifications.html',
  styleUrl: './notifications.scss',
})
export class Notifications {
  private readonly service = inject(NotificationService);

  protected readonly items = signal<Notification[]>([]);
  protected readonly page = signal<Page<Notification> | null>(null);
  protected readonly unreadOnly = signal(false);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.service.list(this.unreadOnly(), 0, 50).subscribe({
      next: (result) => {
        this.items.set(result.content as Notification[]);
        this.page.set(result as Page<Notification>);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.loading.set(false);
        this.error.set(genericErrorMessage(err));
      },
    });
  }

  protected toggleUnread(): void {
    this.unreadOnly.set(!this.unreadOnly());
    this.load();
  }

  protected markRead(notification: Notification): void {
    if (notification.read) return;
    this.service.markRead(notification.id).subscribe({
      next: () => this.load(),
      error: () => undefined,
    });
  }

  protected markAllRead(): void {
    this.service.markAllRead().subscribe({
      next: () => this.load(),
      error: () => undefined,
    });
  }

  protected linkFor(notification: Notification): string | null {
    if (notification.referenceType === 'APPLICATION' && notification.referenceId) {
      return `/equipe/candidatures/${notification.referenceId}`;
    }
    if (notification.referenceType === 'INTERVIEW') {
      return '/equipe/entretiens';
    }
    return null;
  }
}
