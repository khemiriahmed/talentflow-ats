/**
 * N'accepte qu'un chemin interne (évite les redirections ouvertes vers un site externe).
 */
export function safeReturnUrl(url: string | null | undefined, fallback: string): string {
  if (!url || !url.startsWith('/') || url.startsWith('//') || url.includes('\\')) {
    return fallback;
  }
  return url;
}
