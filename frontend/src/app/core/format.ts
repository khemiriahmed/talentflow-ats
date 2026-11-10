import { CURRENCY } from './config';

const number = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 });

export function formatSalary(min: number | null, max: number | null): string | null {
  if (min == null && max == null) {
    return null;
  }
  if (min != null && max != null) {
    return min === max
      ? `${number.format(min)} ${CURRENCY}`
      : `${number.format(min)} à ${number.format(max)} ${CURRENCY}`;
  }
  return min != null ? `À partir de ${number.format(min)} ${CURRENCY}` : `Jusqu’à ${number.format(max!)} ${CURRENCY}`;
}

export function formatFileSize(bytes: number): string {
  if (bytes < 1024 * 1024) {
    return `${Math.max(1, Math.round(bytes / 1024))} Ko`;
  }
  return `${(bytes / (1024 * 1024)).toFixed(1).replace('.', ',')} Mo`;
}

/** Date limite dans les 7 prochains jours (ou dépassée). */
export function isDeadlineSoon(deadline: string | null, now = new Date()): boolean {
  if (!deadline) {
    return false;
  }
  const days = (new Date(deadline + 'T23:59:59').getTime() - now.getTime()) / 86_400_000;
  return days <= 7;
}

export function isDeadlinePassed(deadline: string | null, now = new Date()): boolean {
  return !!deadline && new Date(deadline + 'T23:59:59').getTime() < now.getTime();
}
