import { HttpClient } from '@angular/common/http';
import { Component, inject, input, output, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { applyErrorMessage } from '../../core/api-error';
import { API_BASE, MAX_COVER_LETTER, MAX_CV_BYTES } from '../../core/config';
import { formatFileSize } from '../../core/format';
import { SOURCE_OPTIONS } from '../../core/labels';
import { Application } from '../../core/models';
import { Icon } from '../../shared/icon';

@Component({
  selector: 'app-apply-form',
  imports: [ReactiveFormsModule, RouterLink, Icon],
  templateUrl: './apply-form.html',
  styleUrl: './apply-form.scss',
})
export class ApplyForm {
  readonly jobId = input.required<number>();
  readonly applied = output<void>();

  private readonly http = inject(HttpClient);
  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly sources = SOURCE_OPTIONS;
  protected readonly maxCoverLetter = MAX_COVER_LETTER;
  protected readonly form = this.fb.group({ coverLetter: '', source: '' });
  protected readonly coverLength = toSignal(this.form.controls.coverLetter.valueChanges, {
    initialValue: '',
  });

  protected readonly file = signal<File | null>(null);
  protected readonly fileError = signal<string | null>(null);
  protected readonly dragging = signal(false);
  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly done = signal(false);

  protected readonly formatSize = formatFileSize;

  protected onPick(event: Event): void {
    const input = event.target as HTMLInputElement;
    const picked = input.files?.item(0);
    if (picked) {
      void this.accept(picked);
    }
    input.value = '';
  }

  protected onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(true);
  }

  protected onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(false);
    const dropped = event.dataTransfer?.files.item(0);
    if (dropped) {
      void this.accept(dropped);
    }
  }

  /** Vérifie le fichier avant l'envoi : le serveur refait les mêmes contrôles. */
  protected async accept(candidate: File): Promise<void> {
    this.fileError.set(null);
    if (candidate.size > MAX_CV_BYTES) {
      this.file.set(null);
      this.fileError.set('Ce fichier dépasse 5 Mo. Choisissez un PDF plus léger.');
      return;
    }
    const signature = await candidate.slice(0, 5).text();
    if (signature !== '%PDF-') {
      this.file.set(null);
      this.fileError.set('Ce fichier n’est pas un PDF. Exportez votre CV au format PDF puis réessayez.');
      return;
    }
    this.file.set(candidate);
  }

  protected submit(): void {
    if (this.submitting()) {
      return;
    }
    const cv = this.file();
    if (!cv) {
      this.fileError.set('Ajoutez votre CV au format PDF pour continuer.');
      return;
    }
    this.error.set(null);
    this.submitting.set(true);

    const { coverLetter, source } = this.form.getRawValue();
    const body = new FormData();
    body.append('cv', cv, cv.name);
    if (coverLetter.trim()) body.append('coverLetter', coverLetter.trim());
    if (source) body.append('source', source);

    this.http.post<Application>(`${API_BASE}/jobs/${this.jobId()}/applications`, body).subscribe({
      next: () => {
        this.submitting.set(false);
        this.done.set(true);
        this.applied.emit();
      },
      error: (err: unknown) => {
        this.submitting.set(false);
        this.error.set(applyErrorMessage(err));
      },
    });
  }
}
