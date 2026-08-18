import { afterEach, describe, expect, it, vi } from 'vitest';
import { act, renderHook, waitFor } from '@testing-library/react';
import { usePlannedMealToggle } from './usePlannedMealToggle';
import { logPlannedMealAsPlanned, unmarkPlannedMeal, type NutritionMeal } from '../api/nutrition';
import { NotificationProvider } from '../components/NotificationProvider';

vi.mock('../api/nutrition', () => ({
  logPlannedMealAsPlanned: vi.fn(),
  unmarkPlannedMeal: vi.fn(),
}));

const logMock = vi.mocked(logPlannedMealAsPlanned);
const unmarkMock = vi.mocked(unmarkPlannedMeal);

const MEAL = { id: 'm1', mealType: 'BREAKFAST', name: 'Desayuno' } as unknown as NutritionMeal;

function setup(reload: () => Promise<unknown>) {
  return renderHook(() => usePlannedMealToggle('2026-08-18', reload), {
    wrapper: NotificationProvider,
  });
}

describe('usePlannedMealToggle', () => {
  afterEach(() => vi.clearAllMocks());

  it('logs an unmarked meal and refreshes the day it belongs to', async () => {
    logMock.mockResolvedValue({} as never);
    const reload = vi.fn().mockResolvedValue(undefined);
    const { result } = setup(reload);

    act(() => result.current.toggle(MEAL, false));

    expect(logMock).toHaveBeenCalledWith('2026-08-18', MEAL);
    await waitFor(() => expect(reload).toHaveBeenCalled());
  });

  it('unmarks a meal that was already eaten', async () => {
    unmarkMock.mockResolvedValue(undefined);
    const reload = vi.fn().mockResolvedValue(undefined);
    const { result } = setup(reload);

    act(() => result.current.toggle(MEAL, true));

    expect(unmarkMock).toHaveBeenCalledWith('2026-08-18', 'm1');
    await waitFor(() => expect(reload).toHaveBeenCalled());
  });

  it('reports only the meal whose request is in flight, and clears it after', async () => {
    let settle: () => void = () => {};
    logMock.mockReturnValue(new Promise<never>((resolve) => (settle = resolve as () => void)));
    const reload = vi.fn().mockResolvedValue(undefined);
    const { result } = setup(reload);

    act(() => result.current.toggle(MEAL, false));
    // Per meal, not a single page-wide flag: one row waiting must not disable
    // the other four.
    expect(result.current.marking.has('m1')).toBe(true);
    expect(result.current.marking.has('m2')).toBe(false);

    act(() => settle());
    await waitFor(() => expect(result.current.marking.has('m1')).toBe(false));
  });

  it('stops marking and does not refresh when the request fails', async () => {
    logMock.mockRejectedValue(new Error('offline'));
    const reload = vi.fn().mockResolvedValue(undefined);
    const { result } = setup(reload);

    act(() => result.current.toggle(MEAL, false));

    await waitFor(() => expect(result.current.marking.has('m1')).toBe(false));
    // A failed write must not leave the UI showing the meal as done.
    expect(reload).not.toHaveBeenCalled();
  });
});
