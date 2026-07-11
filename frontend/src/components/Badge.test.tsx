import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Badge } from './Badge';

describe('Badge', () => {
  it('renders its label', () => {
    render(<Badge tone="accent">Saludable</Badge>);

    expect(screen.getByText('Saludable')).toBeInTheDocument();
  });

  it('defaults to the neutral tone', () => {
    render(<Badge>Sin datos</Badge>);

    expect(screen.getByText('Sin datos')).toHaveAttribute('data-tone', 'neutral');
  });

  it.each(['accent', 'warning', 'danger', 'neutral'] as const)(
    'applies the %s tone as a data attribute for styling',
    (tone) => {
      render(<Badge tone={tone}>Estado</Badge>);

      expect(screen.getByText('Estado')).toHaveAttribute('data-tone', tone);
    },
  );
});
