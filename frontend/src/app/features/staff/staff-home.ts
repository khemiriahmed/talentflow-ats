import { httpResource } from '@angular/common/http';
import { Component, computed, inject } from '@angular/core';
import { AuthService } from '../../core/auth.service';
import { API_BASE } from '../../core/config';
import { ROLE_LABELS } from '../../core/labels';
import { DashboardStats } from '../../core/models';

@Component({
  selector: 'app-staff-home',
  templateUrl: './staff-home.html',
  styleUrl: './staff-home.scss',
})
export class StaffHome {
  private readonly auth = inject(AuthService);

  protected readonly user = this.auth.user;
  protected readonly roleLabel = computed(() => {
    const role = this.user()?.role;
    return role ? ROLE_LABELS[role] : '';
  });

  private readonly canSeeStats = computed(() => {
    const role = this.user()?.role;
    return role === 'HR' || role === 'ADMIN';
  });

  protected readonly stats = httpResource<DashboardStats>(() =>
    this.canSeeStats() ? `${API_BASE}/dashboard/stats` : undefined,
  );
  protected readonly unread = httpResource<{ count: number }>(() => `${API_BASE}/notifications/unread-count`);
}
