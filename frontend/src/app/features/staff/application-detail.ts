import { DatePipe } from '@angular/common';
import { HttpClient, httpResource } from '@angular/common/http';
import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { genericErrorMessage, toApiError } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';
import { API_BASE } from '../../core/config';
import {
  INTERVIEW_MODE_LABELS,
  INTERVIEW_STATUS_LABELS,
  RECOMMENDATION_LABELS,
  RECOMMENDATION_OPTIONS,
  STAGE_STEP_LABELS,
  STAGE_OPTIONS,
} from '../../core/labels';
import {
  Application,
  Feedback,
  Interview,
  InterviewPayload,
  Note,
  StaffMember,
  Stage,
  StageHistoryEntry,
} from '../../core/models';

@Component({
  selector: 'app-application-detail',
  imports: [DatePipe, ReactiveFormsModule, RouterLink],
  templateUrl: './application-detail.html',
  styleUrl: './application-detail.scss',
})
export class ApplicationDetail {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly fb = inject(NonNullableFormBuilder);

  readonly id = input.required<string>();

  protected readonly stageLabels = STAGE_STEP_LABELS;
  protected readonly stageOptions = STAGE_OPTIONS;
  protected readonly modeLabels = INTERVIEW_MODE_LABELS;
  protected readonly interviewStatusLabels = INTERVIEW_STATUS_LABELS;
  protected readonly recommendationLabels = RECOMMENDATION_LABELS;
  protected readonly recommendationOptions = RECOMMENDATION_OPTIONS;

  protected readonly error = signal<string | null>(null);
  protected readonly actionError = signal<string | null>(null);
  protected readonly saved = signal<string | null>(null);
  protected readonly openingCv = signal(false);
  protected readonly feedbackOpen = signal<number | null>(null);
  protected readonly feedbackByInterview = signal<Record<number, Feedback | null>>({});
  protected readonly activeFeedbackInterview = signal<number | null>(null);

  protected readonly application = httpResource<Application>(() => `${API_BASE}/applications/${this.id()}`);
  protected readonly history = httpResource<StageHistoryEntry[]>(() =>
    `${API_BASE}/applications/${this.id()}/history`,
  );
  protected readonly notes = httpResource<Note[]>(() => `${API_BASE}/applications/${this.id()}/notes`);
  protected readonly interviews = httpResource<Interview[]>(() =>
    `${API_BASE}/applications/${this.id()}/interviews`,
  );
  protected readonly staff = httpResource<StaffMember[]>(() => `${API_BASE}/staff`);

  protected readonly canManage = computed(() => {
    const role = this.auth.user()?.role;
    return role === 'HR' || role === 'ADMIN';
  });
  protected readonly canEvaluate = computed(() => {
    const role = this.auth.user()?.role;
    return role === 'HR' || role === 'ADMIN' || role === 'MANAGER';
  });

  protected readonly stageForm = this.fb.group({
    stage: ['SCREENING' as Stage, Validators.required],
    reason: ['', Validators.maxLength(2000)],
  });
  protected readonly tagsForm = this.fb.group({
    tags: ['', Validators.maxLength(500)],
  });
  protected readonly noteForm = this.fb.group({
    content: ['', [Validators.required, Validators.maxLength(5000)]],
  });
  protected readonly interviewForm = this.fb.group({
    startAt: ['', Validators.required],
    durationMin: [60, [Validators.required, Validators.min(15), Validators.max(480)]],
    mode: ['ONLINE' as 'ONLINE' | 'ON_SITE', Validators.required],
    locationOrLink: ['', Validators.maxLength(500)],
    participantIds: this.fb.control<number[]>([], { validators: [Validators.required] }),
  });
  protected readonly feedbackForm = this.fb.group({
    rating: [4, [Validators.required, Validators.min(1), Validators.max(5)]],
    strengths: ['', Validators.maxLength(3000)],
    weaknesses: ['', Validators.maxLength(3000)],
    recommendation: ['YES' as 'STRONG_YES' | 'YES' | 'NO' | 'STRONG_NO', Validators.required],
  });

  constructor() {
    effect(() => {
      const application = this.application.value();
      if (!application) return;
      this.stageForm.patchValue({ stage: application.currentStage }, { emitEvent: false });
      this.tagsForm.patchValue({ tags: (application.tags ?? []).join(', ') }, { emitEvent: false });
    });
  }

  protected reloadAll(): void {
    this.application.reload();
    this.history.reload();
    this.notes.reload();
    this.interviews.reload();
  }

  protected openCv(): void {
    if (this.openingCv()) return;
    this.openingCv.set(true);
    this.actionError.set(null);
    this.http.get(`${API_BASE}/applications/${this.id()}/cv`, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        this.openingCv.set(false);
        const url = URL.createObjectURL(blob);
        const opened = window.open(url, '_blank');
        if (!opened) URL.revokeObjectURL(url);
        else setTimeout(() => URL.revokeObjectURL(url), 60_000);
      },
      error: (err: unknown) => {
        this.openingCv.set(false);
        this.actionError.set('Le CV n’a pas pu être ouvert.');
      },
    });
  }

  protected changeStage(): void {
    if (this.stageForm.invalid) return;
    const raw = this.stageForm.getRawValue();
    this.actionError.set(null);
    this.saved.set(null);
    this.http
      .patch(`${API_BASE}/applications/${this.id()}/stage`, {
        stage: raw.stage,
        reason: raw.reason.trim() || null,
      })
      .subscribe({
        next: () => {
          this.saved.set('L’étape de la candidature a été mise à jour.');
          this.stageForm.patchValue({ reason: '' });
          this.reloadAll();
        },
        error: (err: unknown) => this.actionError.set(genericErrorMessage(err)),
      });
  }

  protected saveTags(): void {
    const tags = this.tagsForm
      .getRawValue()
      .tags.split(',')
      .map((tag) => tag.trim())
      .filter((tag) => tag.length > 0);
    if (tags.length > 10) {
      this.actionError.set('Vous pouvez saisir au maximum 10 étiquettes.');
      return;
    }
    this.actionError.set(null);
    this.saved.set(null);
    this.http.put(`${API_BASE}/applications/${this.id()}/tags`, { tags }).subscribe({
      next: () => {
        this.saved.set('Les étiquettes ont été enregistrées.');
        this.application.reload();
      },
      error: (err: unknown) => this.actionError.set(genericErrorMessage(err)),
    });
  }

  protected addNote(): void {
    if (this.noteForm.invalid) return;
    const content = this.noteForm.getRawValue().content.trim();
    if (!content) return;
    this.actionError.set(null);
    this.saved.set(null);
    this.http.post(`${API_BASE}/applications/${this.id()}/notes`, { content }).subscribe({
      next: () => {
        this.noteForm.reset({ content: '' });
        this.notes.reload();
      },
      error: (err: unknown) => this.actionError.set(genericErrorMessage(err)),
    });
  }

  protected deleteNote(note: Note): void {
    if (!window.confirm('Supprimer cette note ?')) return;
    this.http.delete(`${API_BASE}/applications/${this.id()}/notes/${note.id}`).subscribe({
      next: () => this.notes.reload(),
      error: (err: unknown) => this.actionError.set(genericErrorMessage(err)),
    });
  }

  protected scheduleInterview(): void {
    if (this.interviewForm.invalid) return;
    const raw = this.interviewForm.getRawValue();
    if (!raw.participantIds || raw.participantIds.length === 0) {
      this.actionError.set('Sélectionnez au moins un participant.');
      return;
    }
    const start = new Date(raw.startAt);
    if (Number.isNaN(start.getTime()) || start.getTime() <= Date.now()) {
      this.actionError.set('Choisissez une date et une heure futures.');
      return;
    }
    const payload: InterviewPayload = {
      startAt: start.toISOString(),
      durationMin: Number(raw.durationMin),
      mode: raw.mode,
      locationOrLink: raw.locationOrLink.trim() || null,
      participantIds: raw.participantIds.map(Number),
    };
    this.actionError.set(null);
    this.saved.set(null);
    this.http.post(`${API_BASE}/applications/${this.id()}/interviews`, payload).subscribe({
      next: () => {
        this.saved.set('L’entretien a été planifié.');
        this.interviewForm.reset({
          startAt: '',
          durationMin: 60,
          mode: 'ONLINE',
          locationOrLink: '',
          participantIds: [],
        });
        this.interviews.reload();
      },
      error: (err: unknown) => this.actionError.set(genericErrorMessage(err)),
    });
  }

  protected cancelInterview(interview: Interview): void {
    if (!window.confirm('Annuler cet entretien ? Les participants seront prévenus.')) return;
    this.http.patch(`${API_BASE}/interviews/${interview.id}/cancel`, {}).subscribe({
      next: () => this.interviews.reload(),
      error: (err: unknown) => this.actionError.set(genericErrorMessage(err)),
    });
  }

  protected openFeedback(interview: Interview): void {
    if (this.feedbackOpen() === interview.id) {
      this.feedbackOpen.set(null);
      return;
    }
    this.feedbackOpen.set(interview.id);
    this.activeFeedbackInterview.set(interview.id);
    this.feedbackForm.reset({
      rating: 4,
      strengths: '',
      weaknesses: '',
      recommendation: 'YES',
    });
    this.http.get<Feedback>(`${API_BASE}/interviews/${interview.id}/feedback`).subscribe({
      next: (feedback) => {
        this.setFeedback(interview.id, feedback);
        if (this.activeFeedbackInterview() !== interview.id) return;
        this.feedbackForm.patchValue({
          rating: feedback.rating,
          strengths: feedback.strengths ?? '',
          weaknesses: feedback.weaknesses ?? '',
          recommendation: feedback.recommendation,
        });
      },
      error: () => {
        this.setFeedback(interview.id, null);
      },
    });
  }

  protected submitFeedback(interview: Interview): void {
    if (this.feedbackForm.invalid) return;
    const raw = this.feedbackForm.getRawValue();
    this.actionError.set(null);
    this.saved.set(null);
    this.http
      .post<Feedback>(`${API_BASE}/interviews/${interview.id}/feedback`, {
        rating: Number(raw.rating),
        strengths: raw.strengths.trim() || null,
        weaknesses: raw.weaknesses.trim() || null,
        recommendation: raw.recommendation,
      })
      .subscribe({
        next: (feedback) => {
          this.setFeedback(interview.id, feedback);
          this.saved.set('Votre avis a été enregistré.');
        },
        error: (err: unknown) => {
          const apiError = toApiError(err);
          this.actionError.set(apiError?.message ?? genericErrorMessage(err));
        },
      });
  }

  private setFeedback(interviewId: number, feedback: Feedback | null): void {
    this.feedbackByInterview.update((map) => {
      const next: Record<number, Feedback | null> = { ...map };
      next[interviewId] = feedback;
      return next;
    });
  }

  protected toggleParticipant(id: number, checked: boolean): void {
    const current = this.interviewForm.controls.participantIds.value ?? [];
    this.interviewForm.controls.participantIds.setValue(
      checked ? [...current, id] : current.filter((participantId) => participantId !== id),
    );
  }

  protected participantChecked(id: number): boolean {
    return (this.interviewForm.controls.participantIds.value ?? []).includes(id);
  }
}
