import { Component, effect, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';
import { NotificationService } from './core/notification.service';
import { Icon } from './shared/icon';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Icon],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly auth = inject(AuthService);
  protected readonly notifications = inject(NotificationService);

  constructor() {
    effect(() => {
      if (this.auth.user()) {
        this.notifications.refreshUnread();
      } else {
        this.notifications.unreadCount.set(0);
      }
    });
  }

  protected logout(): void {
    this.auth.logout();
  }
}
