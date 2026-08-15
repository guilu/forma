import { useEffect, useState } from 'react';
import { getMuscleMap, type MuscleWorked } from '../api/training';

/**
 * The worked-muscle map for a session (FOR-136), shared by the two things that
 * read it: the "Enfoque: …" line and the silhouette overlay beside it.
 *
 * <p>Deliberately not cached. An earlier version memoized answers per session
 * id in a module-level map, to save the second request when both consumers ask
 * for the same session — but that is process-wide mutable state: it outlives
 * the component, survives navigation, and is shared by every test in a file, so
 * one test's fixture leaks into the next. The saving was one small `GET` that
 * the browser's own HTTP cache already handles.
 *
 * <p>Failures resolve to an empty map rather than an error state, on purpose:
 * this decorates a card whose real content — title, status, actions — does not
 * depend on it. A run or a rest day legitimately has no muscles either, so
 * "nothing to show" is a normal answer here, not a fault.
 *
 * <p>Passing `undefined` means "no session to ask about" (a run, a rest day)
 * and skips the request entirely.
 */
export function useSessionMuscles(sessionId: string | undefined): readonly MuscleWorked[] {
  const [muscles, setMuscles] = useState<readonly MuscleWorked[]>([]);

  useEffect(() => {
    if (!sessionId) {
      setMuscles([]);
      return;
    }

    let cancelled = false;
    setMuscles([]);
    getMuscleMap(sessionId)
      .then((map) => {
        if (!cancelled) setMuscles(map.muscles);
      })
      .catch(() => {
        if (!cancelled) setMuscles([]);
      });

    return () => {
      cancelled = true;
    };
  }, [sessionId]);

  return muscles;
}
