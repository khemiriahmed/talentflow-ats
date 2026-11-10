import { Component, computed, input } from '@angular/core';
import { STAGE_MESSAGES, STAGE_STEPS, STAGE_STEP_LABELS } from '../core/labels';
import { Stage } from '../core/models';
import { Icon } from './icon';

interface Step {
  stage: Stage;
  label: string;
  state: 'done' | 'current' | 'todo';
}

/** Parcours d'une candidature : les étapes franchies, l'étape en cours, puis celles à venir. */
@Component({
  selector: 'app-stage-track',
  imports: [Icon],
  templateUrl: './stage-track.html',
  styleUrl: './stage-track.scss',
})
export class StageTrack {
  readonly stage = input.required<Stage>();

  protected readonly rejected = computed(() => this.stage() === 'REJECTED');
  protected readonly message = computed(() => STAGE_MESSAGES[this.stage()]);
  protected readonly position = computed(() => STAGE_STEPS.indexOf(this.stage()));

  protected readonly steps = computed<Step[]>(() => {
    const position = this.position();
    return STAGE_STEPS.map((stage, index) => ({
      stage,
      label: STAGE_STEP_LABELS[stage],
      state: index < position ? 'done' : index === position ? 'current' : 'todo',
    }));
  });

  protected readonly summary = computed(
    () => `Étape ${this.position() + 1} sur ${STAGE_STEPS.length} : ${STAGE_STEP_LABELS[this.stage()]}`,
  );
}
