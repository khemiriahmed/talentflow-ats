import {
  ContractType,
  InterviewMode,
  InterviewStatus,
  JobStatus,
  Recommendation,
  Role,
  Stage,
} from './models';

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

export const JOB_STATUS_LABELS: Record<JobStatus, string> = {
  DRAFT: 'Brouillon',
  PUBLISHED: 'Publiée',
  CLOSED: 'Clôturée',
  ARCHIVED: 'Archivée',
};

export const JOB_STATUS_OPTIONS = (Object.keys(JOB_STATUS_LABELS) as JobStatus[]).map((value) => ({
  value,
  label: JOB_STATUS_LABELS[value],
}));

export const INTERVIEW_MODE_LABELS: Record<InterviewMode, string> = {
  ONLINE: 'En ligne',
  ON_SITE: 'Sur site',
};

export const INTERVIEW_STATUS_LABELS: Record<InterviewStatus, string> = {
  SCHEDULED: 'Planifié',
  DONE: 'Réalisé',
  CANCELLED: 'Annulé',
};

export const RECOMMENDATION_LABELS: Record<Recommendation, string> = {
  STRONG_YES: 'Avis très favorable',
  YES: 'Favorable',
  NO: 'Défavorable',
  STRONG_NO: 'Avis très défavorable',
};

export const RECOMMENDATION_OPTIONS = (
  Object.keys(RECOMMENDATION_LABELS) as Recommendation[]
).map((value) => ({ value, label: RECOMMENDATION_LABELS[value] }));

export const STAGE_OPTIONS = (Object.keys(STAGE_STEP_LABELS) as Stage[]).map((value) => ({
  value,
  label: STAGE_STEP_LABELS[value],
}));

/** Valeurs envoyées au backend dans le champ "source" (utilisées par le dashboard RH). */
export const SOURCE_OPTIONS = [
  { value: 'linkedin', label: 'LinkedIn' },
  { value: 'site', label: 'Site de l’entreprise' },
  { value: 'cooptation', label: 'Cooptation' },
  { value: 'autre', label: 'Autre' },
];
