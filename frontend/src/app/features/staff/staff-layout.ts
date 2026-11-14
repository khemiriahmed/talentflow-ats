import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-staff-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './staff-layout.html',
  styleUrl: './staff-layout.scss',
})
export class StaffLayout {
  protected readonly auth = inject(AuthService);
  protected readonly canSeeDashboard = () => {
    const role = this.auth.user()?.role;
    return role === 'HR' || role === 'ADMIN';
  };
}
