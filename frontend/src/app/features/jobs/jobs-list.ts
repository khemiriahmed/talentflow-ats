import { DatePipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { Component, DestroyRef, OnInit, computed, inject, input, linkedSignal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { debounceTime } from 'rxjs';
import { API_BASE, PAGE_SIZE } from '../../core/config';
import { formatSalary, isDeadlineSoon } from '../../core/format';
import { CONTRACT_LABELS, CONTRACT_OPTIONS } from '../../core/labels';
import { ContractType, Page, PublicJob } from '../../core/models';
import { Icon } from '../../shared/icon';

@Component({
  selector: 'app-jobs-list',
  imports: [ReactiveFormsModule, RouterLink, DatePipe, Icon],
  templateUrl: './jobs-list.html',
  styleUrl: './jobs-list.scss',
})
export class JobsList implements OnInit {
  private readonly router = inject(Router);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  // Les filtres vivent dans l'URL (?keyword=...&location=...) : une recherche se partage par lien.
  readonly keyword = input<string | undefined>();
  readonly location = input<string | undefined>();
  readonly contractType = input<string | undefined>();
  readonly page = input<string | undefined>();

  protected readonly contracts = CONTRACT_OPTIONS;
  protected readonly form = this.fb.group({ keyword: '', location: '', contractType: '' });

  protected readonly jobs = httpResource<Page<PublicJob>>(() => ({
    url: `${API_BASE}/jobs`,
    params: this.params(),
  }));

  /** Garde les résultats précédents à l'écran pendant qu'une nouvelle recherche charge. */
  protected readonly shown = linkedSignal<Page<PublicJob> | undefined, Page<PublicJob> | undefined>({
    source: () => this.jobs.value(),
    computation: (current, previous) => current ?? previous?.value,
  });

  protected readonly hasFilters = computed(
    () => !!(this.keyword()?.trim() || this.location()?.trim() || this.contractType()),
  );

  ngOnInit(): void {
    this.form.patchValue(
      {
        keyword: this.keyword() ?? '',
        location: this.location() ?? '',
        contractType: this.contractType() ?? '',
      },
      { emitEvent: false },
    );
    this.form.valueChanges
      .pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.search());
  }

  protected search(): void {
    const { keyword, location, contractType } = this.form.getRawValue();
    void this.router.navigate([], {
      queryParams: {
        keyword: keyword.trim() || null,
        location: location.trim() || null,
        contractType: contractType || null,
        page: null,
      },
      queryParamsHandling: 'merge',
    });
  }

  protected clearFilters(): void {
    this.form.reset({ keyword: '', location: '', contractType: '' }, { emitEvent: false });
    this.search();
  }

  protected goTo(pageIndex: number): void {
    void this.router.navigate([], {
      queryParams: { page: pageIndex > 0 ? pageIndex : null },
      queryParamsHandling: 'merge',
    });
  }

  protected contractLabel(type: ContractType): string {
    return CONTRACT_LABELS[type];
  }

  protected salary(job: PublicJob): string | null {
    return formatSalary(job.salaryMin, job.salaryMax);
  }

  protected soon(job: PublicJob): boolean {
    return isDeadlineSoon(job.deadline);
  }

  private params(): Record<string, string | number> {
    const params: Record<string, string | number> = {
      page: Number(this.page() ?? 0) || 0,
      size: PAGE_SIZE,
    };
    const keyword = this.keyword()?.trim();
    const location = this.location()?.trim();
    const contractType = this.contractType();
    if (keyword) params['keyword'] = keyword;
    if (location) params['location'] = location;
    if (contractType) params['contractType'] = contractType;
    return params;
  }
}
