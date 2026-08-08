import { describe, expect, it } from 'vitest';
import { localIsoDate } from './localIsoDate';

describe('localIsoDate', () => {
  it('uses calendar-local fields instead of the UTC date near midnight', () => {
    const nearMidnight = {
      getFullYear: () => 2026,
      getMonth: () => 7,
      getDate: () => 8,
      toISOString: () => '2026-08-07T22:30:00.000Z',
    } as Date;

    expect(localIsoDate(nearMidnight)).toBe('2026-08-08');
  });
});
