import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Brand } from './Brand';

describe('Brand', () => {
  it('renders the brand mark as a decorative image pointing at the shared logo asset', () => {
    const { container } = render(<Brand />);

    // The mark is served from the app-wide asset (frontend/public/logo.svg,
    // copied from docs/logo.svg) rather than inlined, since the source SVG
    // bakes in fixed gradient ids that would collide if multiple <Brand>
    // instances (Topbar + Sidebar) inlined it on the same page.
    const mark = container.querySelector('img');
    expect(mark).toHaveAttribute('src', '/logo.svg');
  });

  it('keeps the mark decorative so "FORMA" is announced once, not doubled', () => {
    render(<Brand />);

    // The wordmark is the sole accessible name for the lockup: the mark
    // image must carry an empty alt (removing it from the a11y tree), not
    // alt="FORMA", or screen readers would announce "FORMA" twice.
    expect(screen.queryByRole('img')).not.toBeInTheDocument();
    expect(screen.getByText('FORMA')).toBeInTheDocument();
  });

  it('omits the wordmark when showWordmark is false but keeps the mark', () => {
    const { container } = render(<Brand showWordmark={false} />);

    expect(screen.queryByText('FORMA')).not.toBeInTheDocument();
    expect(container.querySelector('img')).toHaveAttribute('src', '/logo.svg');
  });
});
