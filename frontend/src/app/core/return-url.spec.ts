import { safeReturnUrl } from './return-url';

describe('safeReturnUrl', () => {
  it('keeps internal paths', () => {
    expect(safeReturnUrl('/offres/12?page=2', '/offres')).toBe('/offres/12?page=2');
  });

  it.each(['https://evil.example', '//evil.example', '/\\evil.example', 'offres', '', null, undefined])(
    'falls back for %s',
    (value) => {
      expect(safeReturnUrl(value as string | null | undefined, '/offres')).toBe('/offres');
    },
  );
});
