import { describe, expect, it } from 'vitest';
import {
  formatDueDate,
  formatMetricValue,
  metricLabel,
  metricUnit,
  statusLabel,
} from './goalsDisplay';

describe('goalsDisplay', () => {
  it.each([
    ['BODY_FAT_PCT', 'Grasa corporal'],
    ['WEIGHT_KG', 'Peso corporal'],
    ['LEAN_MASS_KG', 'Masa magra'],
  ])('labels the %s metric as "%s"', (metric, label) => {
    expect(metricLabel(metric)).toBe(label);
  });

  it('falls back to the raw value for an unrecognized metric', () => {
    expect(metricLabel('SOME_FUTURE_METRIC')).toBe('SOME_FUTURE_METRIC');
  });

  it.each([
    ['BODY_FAT_PCT', '%'],
    ['WEIGHT_KG', 'kg'],
    ['LEAN_MASS_KG', 'kg'],
  ])('units the %s metric as "%s"', (metric, unit) => {
    expect(metricUnit(metric)).toBe(unit);
  });

  it.each([
    ['ACTIVE', 'Activo'],
    ['ACHIEVED', 'Conseguido'],
    ['ARCHIVED', 'Archivado'],
  ])('labels the %s status as "%s"', (status, label) => {
    expect(statusLabel(status)).toBe(label);
  });

  it('falls back to the raw value for an unrecognized status', () => {
    expect(statusLabel('SOMETHING_ELSE')).toBe('SOMETHING_ELSE');
  });

  it('formats a due date in es-ES long form', () => {
    expect(formatDueDate('2026-12-31')).toBe('31 dic 2026');
  });

  it('renders a calm placeholder when there is no due date', () => {
    expect(formatDueDate(null)).toBe('Sin fecha límite');
  });

  it('formats a metric value with its unit', () => {
    expect(formatMetricValue(16.4, 'BODY_FAT_PCT')).toBe('16.4 %');
    expect(formatMetricValue(78, 'WEIGHT_KG')).toBe('78 kg');
  });

  it('never fabricates a value: null renders an em dash, not 0', () => {
    expect(formatMetricValue(null, 'BODY_FAT_PCT')).toBe('—');
  });
});
