import { useState } from 'react';
import { useNotify } from '../../components/NotificationProvider';
import { ApiRequestError } from '../../api/client';
import { rescheduleSession, updateSessionStatus } from '../../api/training';
import type { DayOfWeek, SessionStatus } from '../../api/training';
import { DAY_LABELS } from './trainingLabels';

const MARK_ERROR = 'No se pudo actualizar la sesión. Inténtalo de nuevo.';
const MOVE_ERROR = 'No se pudo mover la sesión. Inténtalo de nuevo.';

interface SessionActionsOptions {
  /** Refetches whatever view is showing the week. Awaited before the pending flag clears. */
  readonly reload: () => Promise<unknown>;
  /** After a successful status change — the detail dialog patches the session it is showing. */
  readonly onMarked?: (sessionId: string, status: SessionStatus) => void;
  /** After a successful move — the session is no longer on the day it was opened from. */
  readonly onMoved?: (sessionId: string, day: DayOfWeek) => void;
}

/**
 * Marking a session done and moving it to another day, with the feedback both
 * of them owe the user.
 *
 * <p>Extracted from `TrainingPage` when the dashboard's training card started
 * opening the same detail dialog: the dialog's two buttons and its day select
 * have to keep working from either screen, and a second copy of this would be
 * two places for the error copy and the toast rule to drift apart.
 *
 * <p>Success feedback is deliberately asymmetric (FOR-63): completing is a
 * moment worth confirming, skipping is not — `ui-guidelines.md`'s "no guilt
 * language" cuts both ways. Every failure goes to the notification region, the
 * same place a success does, rather than to a band each screen draws for itself.
 */
export function useSessionActions({ reload, onMarked, onMoved }: SessionActionsOptions) {
  const notify = useNotify();
  const [pendingId, setPendingId] = useState<string | undefined>(undefined);

  async function mark(sessionId: string, status: SessionStatus) {
    setPendingId(sessionId);
    try {
      await updateSessionStatus(sessionId, status);
      await reload();
      onMarked?.(sessionId, status);
      if (status === 'COMPLETED') {
        notify.success('Entrenamiento marcado como completado.');
      }
    } catch (error) {
      notify.error(error instanceof ApiRequestError ? error.message : MARK_ERROR);
    } finally {
      setPendingId(undefined);
    }
  }

  /**
   * Moves a session to another day of this week. The week is refetched rather
   * than patched in place: the move changes which day every other session
   * shares it with, so the calendar has to redraw anyway.
   */
  async function move(sessionId: string, day: DayOfWeek) {
    setPendingId(sessionId);
    try {
      await rescheduleSession(sessionId, day);
      await reload();
      onMoved?.(sessionId, day);
      notify.success(`Sesión movida a ${DAY_LABELS[day].toLocaleLowerCase('es-ES')}.`);
    } catch (error) {
      notify.error(error instanceof ApiRequestError ? error.message : MOVE_ERROR);
    } finally {
      setPendingId(undefined);
    }
  }

  return { pendingId, mark, move };
}
