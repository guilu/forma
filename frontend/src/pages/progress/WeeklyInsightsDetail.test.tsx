import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { WeeklyInsightsDetail } from './WeeklyInsightsDetail';
import { type WeeklyInsights } from '../../api/insights';

/**
 * WeeklyInsightsDetail tests (FOR-124). This component is the extracted,
 * reusable "full insights" rendering shared by the current-week
 * `InsightsSection` (FOR-56) and the new `InsightsHistorySection` (FOR-124)
 * for a selected historical period — same main/secondary/signals/disclaimer
 * markup either way, plus FOR-110's week-over-week deltas on related
 * signals when present.
 */
const baseInsights: WeeklyInsights = {
  checkIn: {
    weekStartDate: '2026-07-06',
    latestWeightKg: 70.2,
    latestBodyFatPercentage: 18.4,
    latestLeanMassKg: 55.1,
    plannedRunningSessions: 3,
    completedRunningSessions: 3,
    plannedStrengthSessions: 3,
    completedStrengthSessions: 2,
  },
  main: {
    category: 'BODY',
    severity: 'ACTION',
    message: 'El peso baja rápido; considera aumentar un poco las calorías para frenar la pérdida.',
    reason:
      'El peso baja 1.5 kg en 7 días (~-2.1% por semana), por encima del 1% semanal recomendado.',
    relatedMetric: 'weeklyWeightChangeKg',
    createdAt: '2026-07-10T08:00:00Z',
  },
  secondary: [
    {
      category: 'TRAINING',
      severity: 'INFO',
      message: 'Semana muy constante; mantén este ritmo.',
      reason: 'Se completaron 5 de 6 sesiones planificadas.',
      createdAt: '2026-07-10T08:00:00Z',
    },
  ],
  generatedAt: '2026-07-10T08:00:00Z',
};

describe('WeeklyInsightsDetail', () => {
  it('renders the main recommendation, related signals and disclaimer', () => {
    render(<WeeklyInsightsDetail insights={{ ...baseInsights, deltas: {} }} />);

    expect(screen.getByText(baseInsights.main.message)).toBeInTheDocument();
    expect(screen.getByText(baseInsights.main.reason)).toBeInTheDocument();
    expect(screen.getByText('70.2 kg')).toBeInTheDocument();
    expect(
      screen.getByText(/no sustituyen el diagnóstico ni el consejo de un profesional sanitario/i),
    ).toBeInTheDocument();
  });

  it('renders secondary recommendations when present', () => {
    render(<WeeklyInsightsDetail insights={{ ...baseInsights, deltas: {} }} />);

    expect(screen.getByText('Semana muy constante; mantén este ritmo.')).toBeInTheDocument();
  });

  it('shows only the absolute value when the backend provides no delta (no prior period)', () => {
    render(<WeeklyInsightsDetail insights={{ ...baseInsights, deltas: {} }} />);

    expect(screen.getByText('70.2 kg')).toBeInTheDocument();
    expect(screen.queryByText(/vs\. semana anterior/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/undefined/i)).not.toBeInTheDocument();
  });

  it('shows a negative weight delta alongside the absolute value, unambiguously signed', () => {
    render(
      <WeeklyInsightsDetail insights={{ ...baseInsights, deltas: { weightDeltaKg: -0.4 } }} />,
    );

    expect(screen.getByText('70.2 kg')).toBeInTheDocument();
    expect(screen.getByText(/−0\.4 kg vs\. semana anterior/)).toBeInTheDocument();
  });

  it('shows a positive body fat delta with an explicit plus sign', () => {
    render(
      <WeeklyInsightsDetail
        insights={{ ...baseInsights, deltas: { bodyFatPercentageDelta: 1.2 } }}
      />,
    );

    expect(screen.getByText('18.4 %')).toBeInTheDocument();
    expect(screen.getByText(/\+1\.2 % vs\. semana anterior/)).toBeInTheDocument();
  });

  it('shows a lean mass delta of exactly zero as an explicit, unambiguous value', () => {
    render(<WeeklyInsightsDetail insights={{ ...baseInsights, deltas: { leanMassDeltaKg: 0 } }} />);

    expect(screen.getByText('55.1 kg')).toBeInTheDocument();
    expect(screen.getByText(/0\.0 kg vs\. semana anterior/)).toBeInTheDocument();
  });

  it('shows an aggregate training-completion delta line when the backend provides one', () => {
    render(
      <WeeklyInsightsDetail
        insights={{ ...baseInsights, deltas: { trainingCompletionDelta: 2 } }}
      />,
    );

    expect(screen.getByText('Entrenamientos completados')).toBeInTheDocument();
    expect(screen.getByText(/\+2 sesiones vs\. semana anterior/)).toBeInTheDocument();
  });

  it('omits the training-completion delta line entirely when the backend provides none', () => {
    render(<WeeklyInsightsDetail insights={{ ...baseInsights, deltas: {} }} />);

    expect(screen.queryByText('Entrenamientos completados')).not.toBeInTheDocument();
  });

  it('formats a single-session training delta with singular wording', () => {
    render(
      <WeeklyInsightsDetail
        insights={{ ...baseInsights, deltas: { trainingCompletionDelta: -1 } }}
      />,
    );

    expect(screen.getByText(/−1 sesión vs\. semana anterior/)).toBeInTheDocument();
  });
});
