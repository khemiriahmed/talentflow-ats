import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { API_BASE } from './config';
import { Page } from './models';

export interface AppNotification {
  id: number;
  type: string;
  message: string;
  referenceType: string | null;
  referenceId: number | null;
  read: boolean;
  createdAt: string;
}

/** Compteur de la cloche, partagé par le bandeau et la page de notifications. */
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly http = inject(HttpClient);
  readonly unreadCount = signal(0);

  refreshUnread(): void {
    this.http
      .get<{ count: number }>(`${API_BASE}/notifications/unread-count`)
      .subscribe({ next: ({ count }) => this.unreadCount.set(count), error: () => undefined });
  }

  list(unreadOnly = false, page = 0, size = 30): Observable<Page<AppNotification>> {
    return this.http.get<Page<AppNotification>>(`${API_BASE}/notifications`, {
      params: { unreadOnly, page, size },
    });
  }

  markRead(id: number): Observable<AppNotification> {
    return this.http
      .patch<AppNotification>(`${API_BASE}/notifications/${id}/read`, {})
      .pipe(tap(() => this.refreshUnread()));
  }

  markAllRead(): Observable<unknown> {
    return this.http
      .post(`${API_BASE}/notifications/read-all`, {})
      .pipe(tap(() => this.unreadCount.set(0)));
  }
}
