import { formatFileSize, formatSalary, isDeadlinePassed, isDeadlineSoon } from './format';

describe('format helpers', () => {
  it('formats salary ranges in French', () => {
    expect(formatSalary(null, null)).toBeNull();
    expect(formatSalary(3500, 5200)).toMatch(/^3\s?500 à 5\s?200 €$/);
    expect(formatSalary(40000, 40000)).toMatch(/^40\s?000 €$/);
    expect(formatSalary(2800, null)).toMatch(/^À partir de 2\s?800 €$/);
    expect(formatSalary(null, 3000)).toMatch(/^Jusqu’à 3\s?000 €$/);
  });

  it('formats file sizes', () => {
    expect(formatFileSize(200)).toBe('1 Ko');
    expect(formatFileSize(250_000)).toBe('244 Ko');
    expect(formatFileSize(2.5 * 1024 * 1024)).toBe('2,5 Mo');
  });

  it('detects deadlines that are soon or passed', () => {
    const now = new Date('2026-11-07T10:00:00');
    expect(isDeadlineSoon('2026-11-10', now)).toBe(true);
    expect(isDeadlineSoon('2026-12-10', now)).toBe(false);
    expect(isDeadlineSoon(null, now)).toBe(false);
    expect(isDeadlinePassed('2026-11-06', now)).toBe(true);
    expect(isDeadlinePassed('2026-11-07', now)).toBe(false);
  });
});
