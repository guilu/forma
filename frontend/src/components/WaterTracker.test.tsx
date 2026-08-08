import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { WaterTracker } from './WaterTracker';
import {
  getHydration,
  logWaterIntake,
  removeWaterGlass,
  type HydrationProgress,
} from '../api/nutrition';

vi.mock('../api/nutrition', () => ({
  getHydration: vi.fn(),
  logWaterIntake: vi.fn(),
  removeWaterGlass: vi.fn(),
}));

const hydrationMock = vi.mocked(getHydration);
const logMock = vi.mocked(logWaterIntake);
const removeMock = vi.mocked(removeWaterGlass);

const DATE = '2026-08-04';

/** 2.1 of 2.5 litres — the figures the placeholder used to invent, now arriving from the API. */
const drunk: HydrationProgress = {
  date: DATE,
  totalMl: 2100,
  goalMl: 2500,
  progress: 0.84,
};

describe('WaterTracker', () => {
  beforeEach(() => {
    hydrationMock.mockReset();
    hydrationMock.mockResolvedValue(drunk);
    logMock.mockReset();
    logMock.mockResolvedValue({ id: 'w1', date: DATE, volumeMl: 250 });
    removeMock.mockReset();
    removeMock.mockResolvedValue({ ...drunk, totalMl: 1850, progress: 0.74 });
  });

  it('shows what was drunk against the goal, with an accessible summary', async () => {
    render(<WaterTracker date={DATE} />);

    expect(await screen.findByText('84%')).toBeInTheDocument();
    expect(
      screen.getByRole('img', { name: /Hidratación: 2,1 de 2,5 litros \(84%\)/ }),
    ).toBeInTheDocument();
  });

  /** Millilitres on the wire, litres on the screen — the conversion happens once, here. */
  it('reads the day it is given', async () => {
    render(<WaterTracker date={DATE} />);

    await waitFor(() => expect(hydrationMock).toHaveBeenCalledWith(DATE));
  });

  it('logs a volume and reads the day back', async () => {
    const user = userEvent.setup();
    render(<WaterTracker date={DATE} />);
    await screen.findByText('84%');

    await user.click(screen.getByRole('button', { name: '+ Vaso (250 ml)' }));

    await waitFor(() => expect(logMock).toHaveBeenCalledWith(DATE, 250));
    await waitFor(() => expect(hydrationMock).toHaveBeenCalledTimes(2));
  });

  /** The label carries the millilitres, because "a glass" means something different everywhere. */
  it('says how much each shortcut logs', async () => {
    render(<WaterTracker date={DATE} />);
    await screen.findByText('84%');

    expect(screen.getByRole('button', { name: '+ Vaso (250 ml)' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '− Vaso (250 ml)' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Botella/ })).not.toBeInTheDocument();
  });

  it('removes a glass through persistence and renders the returned total', async () => {
    const user = userEvent.setup();
    render(<WaterTracker date={DATE} />);
    await screen.findByText('84%');

    await user.click(screen.getByRole('button', { name: '− Vaso (250 ml)' }));

    await waitFor(() => expect(removeMock).toHaveBeenCalledWith(DATE));
    expect(await screen.findByText('74%')).toBeInTheDocument();
  });

  /**
   * Past the goal, the meter stops at full.
   *
   * <p>The API reports progress uncapped on purpose — 1.2 is twenty per cent over — and a meter
   * cannot draw six fifths of five segments.
   */
  it('does not overfill the meter past the goal', async () => {
    hydrationMock.mockResolvedValue({ date: DATE, totalMl: 3000, goalMl: 2500, progress: 1.2 });
    render(<WaterTracker date={DATE} />);

    expect(await screen.findByText('120%')).toBeInTheDocument();
    expect(screen.getByRole('img', { name: /3,0 de 2,5 litros/ })).toBeInTheDocument();
  });

  /** No goal is a state the API documents: no denominator, rather than a denominator of zero. */
  it('shows what was drunk when there is no goal at all', async () => {
    hydrationMock.mockResolvedValue({ date: DATE, totalMl: 900, goalMl: null, progress: null });
    render(<WaterTracker date={DATE} />);

    expect(await screen.findByText(/0,9/)).toBeInTheDocument();
    expect(screen.queryByText(/%/)).not.toBeInTheDocument();
    expect(screen.getByRole('img', { name: /sin objetivo fijado/ })).toBeInTheDocument();
  });

  /** A failed request says so. It never falls back to a number nobody measured. */
  it('says so when the day cannot be read', async () => {
    hydrationMock.mockRejectedValue(new Error('nope'));
    render(<WaterTracker date={DATE} />);

    expect(await screen.findByText(/No se pudo cargar el agua/)).toBeInTheDocument();
  });
});
