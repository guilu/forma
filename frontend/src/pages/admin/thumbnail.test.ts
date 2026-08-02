import { describe, expect, it } from 'vitest';
import { thumbnailUrl } from './thumbnail';

describe('thumbnailUrl', () => {
  /**
   * The shop hands us a 300px crop; the list needs 24. Rewriting their own imgix
   * parameters is what keeps a 24px avatar from downloading a 300px photo.
   */
  it('resizes an imgix url in place', () => {
    expect(
      thumbnailUrl('https://prod-mercadona.imgix.net/images/abc.jpg?fit=crop&h=300&w=300', 24),
    ).toBe('https://prod-mercadona.imgix.net/images/abc.jpg?fit=crop&h=24&w=24');
  });

  /**
   * A CDN that advertises no sizing is left alone: asking Amazon for `fit=crop` would be asking for
   * a resize nobody promised, and the URL would carry parameters that mean nothing. The image is
   * sized in CSS either way.
   */
  it('leaves a url with no sizing of its own untouched', () => {
    expect(thumbnailUrl('https://m.media-amazon.com/images/I/31kt192oAzL._AC_.jpg', 24)).toBe(
      'https://m.media-amazon.com/images/I/31kt192oAzL._AC_.jpg',
    );
  });

  it('has nothing to resize when there is no image', () => {
    expect(thumbnailUrl(undefined, 24)).toBeUndefined();
  });

  /** Never hand an <img> something that is not a url — a broken src is a broken layout. */
  it('ignores anything that is not an http url', () => {
    expect(thumbnailUrl('javascript:alert(1)', 24)).toBeUndefined();
  });
});
