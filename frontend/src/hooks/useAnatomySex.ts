import { useEffect, useState } from 'react';
import { getProfile } from '../api/profile';
import type { AnatomySex } from '../components/MuscleSilhouette';

/**
 * Which body the anatomical illustrations should draw for the signed-in user.
 *
 * <p>The pack ships a male and a female sheet, while the profile's `sex` also
 * admits `OTHER` and can be unset — so this maps `FEMALE` to the female figure
 * and everything else, including a profile that fails to load, to the male one.
 * That keeps the first paint (and any offline render) drawing a body instead of
 * a hole, which is the same fallback the training page has always used.
 */
export function useAnatomySex(): AnatomySex {
  const [sex, setSex] = useState<AnatomySex>('male');

  useEffect(() => {
    let active = true;
    getProfile()
      .then((profile) => {
        if (active) setSex(profile.sex === 'FEMALE' ? 'female' : 'male');
      })
      .catch(() => {
        // The male presentation remains the safe fallback when the profile is unavailable.
      });
    return () => {
      active = false;
    };
  }, []);

  return sex;
}
