import { AbstractControl } from '@angular/forms';

/** Affiche l'erreur d'un champ une fois touché, ou après une tentative d'envoi. */
export function showError(control: AbstractControl, submitted: boolean): boolean {
  return control.invalid && (control.touched || submitted);
}
