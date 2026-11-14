import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from './models';

export function toApiError(err: unknown): ApiError | null {
  if (err instanceof HttpErrorResponse && err.error && typeof err.error === 'object') {
    return err.error as ApiError;
  }
  return null;
}

const NETWORK = 'Impossible de joindre le serveur. Vérifiez votre connexion et réessayez.';
const GENERIC = 'Une erreur est survenue. Réessayez dans quelques instants.';

function statusOf(err: unknown): number {
  return err instanceof HttpErrorResponse ? err.status : -1;
}

export function loginErrorMessage(err: unknown): string {
  switch (statusOf(err)) {
    case 401:
      return 'Adresse e-mail ou mot de passe incorrect. Vérifiez votre saisie et réessayez.';
    case 403:
      return 'Ce compte est désactivé ou son adresse e-mail n’est pas encore vérifiée. Consultez votre boîte de réception ou contactez un administrateur.';
    case 0:
      return NETWORK;
    default:
      return GENERIC;
  }
}

export function registerErrorMessage(err: unknown): string {
  switch (statusOf(err)) {
    case 409:
      return 'Un compte existe déjà avec cette adresse e-mail. Connectez-vous ou utilisez une autre adresse.';
    case 400:
      return 'Certaines informations ne sont pas valides. Vérifiez le formulaire et réessayez.';
    case 0:
      return NETWORK;
    default:
      return GENERIC;
  }
}

export function applyErrorMessage(err: unknown): string {
  switch (statusOf(err)) {
    case 409:
      return 'Vous avez déjà postulé à cette offre. Retrouvez votre candidature dans « Mes candidatures ».';
    case 413:
      return 'Le fichier dépasse 5 Mo. Choisissez un PDF plus léger.';
    case 400:
      return 'Votre candidature n’a pas pu être envoyée. Vérifiez que le CV est un PDF de 5 Mo maximum et que l’offre est encore ouverte.';
    case 403:
      return 'Seuls les comptes candidats peuvent postuler à une offre.';
    case 404:
      return 'Cette offre n’est plus disponible.';
    case 0:
      return NETWORK;
    default:
      return GENERIC;
  }
}

export function genericErrorMessage(err: unknown): string {
  return statusOf(err) === 0 ? NETWORK : GENERIC;
}
