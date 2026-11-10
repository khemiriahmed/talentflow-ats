import { ContractType, Role, Stage } from './models';

export const CONTRACT_LABELS: Record<ContractType, string> = {
  CDI: 'CDI',
  CDD: 'CDD',
  FREELANCE: 'Freelance',
  INTERNSHIP: 'Stage',
  APPRENTICESHIP: 'Alternance',
};

export const CONTRACT_OPTIONS = (Object.keys(CONTRACT_LABELS) as ContractType[]).map((value) => ({
  value,
  label: CONTRACT_LABELS[value],
}));

export const ROLE_LABELS: Record<Role, string> = {
  CANDIDATE: 'Candidat',
  HR: 'Ressources humaines',
  MANAGER: 'Manager',
  ADMIN: 'Administrateur',
};

/** Étapes du parcours, dans l'ordre. REJECTED est une issue, pas une étape. */
export const STAGE_STEPS: Stage[] = [
  'RECEIVED',
  'SCREENING',
  'HR_INTERVIEW',
  'TECH_INTERVIEW',
  'OFFER',
  'HIRED',
];

export const STAGE_STEP_LABELS: Record<Stage, string> = {
  RECEIVED: 'Reçue',
  SCREENING: 'Présélection',
  HR_INTERVIEW: 'Entretien RH',
  TECH_INTERVIEW: 'Entretien technique',
  OFFER: 'Proposition',
  HIRED: 'Embauché(e)',
  REJECTED: 'Non retenue',
};

export const STAGE_MESSAGES: Record<Stage, string> = {
  RECEIVED: 'Votre candidature a bien été reçue. L’équipe recrutement l’étudie.',
  SCREENING: 'Votre profil est en cours de présélection.',
  HR_INTERVIEW: 'Vous êtes à l’étape de l’entretien avec l’équipe RH.',
  TECH_INTERVIEW: 'Vous êtes à l’étape de l’entretien technique.',
  OFFER: 'Une proposition d’embauche est en préparation.',
  HIRED: 'Félicitations, vous êtes embauché(e).',
  REJECTED: 'Votre candidature n’a pas été retenue pour ce poste.',
};

/** Valeurs envoyées au backend dans le champ "source" (utilisées par le dashboard RH). */
export const SOURCE_OPTIONS = [
  { value: 'linkedin', label: 'LinkedIn' },
  { value: 'site', label: 'Site de l’entreprise' },
  { value: 'cooptation', label: 'Cooptation' },
  { value: 'autre', label: 'Autre' },
];
