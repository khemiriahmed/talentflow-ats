import { DatePipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { API_BASE } from '../../core/config';
import { AuditLog, Page } from '../../core/models';

@Component({
  selector: 'app-audit-log',
  imports: [DatePipe, FormsModule],
  templateUrl: './audit-log.html',
  styleUrl: './audit-log.scss',
})
export class AuditLogPage {
  protected readonly action = signal('');
  protected readonly entityType = signal('');
  protected readonly actorId = signal('');
  protected readonly page = signal(0);

  protected readonly logs = httpResource<Page<AuditLog>>(() => ({
    url: `${API_BASE}/admin/audit`,
    params: this.params(),
  }));

  protected readonly entityTypes = [
    { value: '', label: 'Tous les objets' },
    { value: 'User', label: 'Utilisateur' },
    { value: 'JobOffer', label: 'Offre' },
    { value: 'Application', label: 'Candidature' },
    { value: 'Interview', label: 'Entretien' },
  ];

  protected search(): void {
    this.page.set(0);
    this.logs.reload();
  }

  protected goTo(page: number): void {
    this.page.set(page);
    this.logs.reload();
  }

  protected humanize(action: string): string {
    return action
      .toLowerCase()
      .split('_')
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  }

  private params(): Record<string, string | number> {
    const params: Record<string, string | number> = { page: this.page(), size: 30 };
    if (this.action().trim()) params['action'] = this.action().trim();
    if (this.entityType()) params['entityType'] = this.entityType();
    if (this.actorId().trim()) params['actorId'] = this.actorId().trim();
    return params;
  }
}
