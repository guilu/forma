/**
 * The product photo a store gave us, at the size the screen needs (FOR-195).
 *
 * <p>Mercadona serves its images through imgix with the crop in the query string
 * — `?fit=crop&h=300&w=300`. Rewriting those parameters rather than scaling in
 * CSS is what keeps a 24 px avatar from downloading a 300 px photo, once per row.
 */
const SIZED = /([?&])(h|w)=\d+/g;

export function thumbnailUrl(url: string | undefined, size: number): string | undefined {
  if (!url) {
    return undefined;
  }
  // Only http(s) reaches an <img>: the url arrives from a third party, and
  // `javascript:` in a src is a script, not a picture.
  if (!/^https?:\/\//i.test(url)) {
    return undefined;
  }
  if (SIZED.test(url)) {
    SIZED.lastIndex = 0;
    return url.replace(
      SIZED,
      (_match, separator: string, parameter: string) => `${separator}${parameter}=${size}`,
    );
  }
  const separator = url.includes('?') ? '&' : '?';
  return `${url}${separator}fit=crop&h=${size}&w=${size}`;
}
