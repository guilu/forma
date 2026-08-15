import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MuscleSilhouette } from './MuscleSilhouette';

/**
 * The silhouette is a stack: one bitmap body, plus one masked layer per worked
 * muscle. These pin the parts that are easy to break silently — that the right
 * body is shown, that only the muscles belonging to *this* view are drawn, and
 * that the whole thing stays decorative unless the caller names it.
 */
describe('MuscleSilhouette', () => {
  it('draws one masked layer per muscle that belongs to the view', () => {
    const { container } = render(
      <MuscleSilhouette
        sex="male"
        view="front"
        muscles={{ PECTORAL: 'primary', ABS: 'secondary' }}
      />,
    );

    expect(container.querySelectorAll('[data-muscle]')).toHaveLength(2);
    expect(container.querySelector('[data-muscle="PECTORAL"]')).toHaveAttribute(
      'data-role',
      'primary',
    );
    expect(container.querySelector('[data-muscle="ABS"]')).toHaveAttribute(
      'data-role',
      'secondary',
    );
  });

  it('skips muscles the requested view cannot draw', () => {
    // LATS only exists on the back sheet; asking for it on the front would
    // otherwise request a mask file that is not there.
    const { container } = render(
      <MuscleSilhouette
        sex="male"
        view="front"
        muscles={{ PECTORAL: 'primary', LATS: 'primary' }}
      />,
    );

    expect(container.querySelectorAll('[data-muscle]')).toHaveLength(1);
    expect(container.querySelector('[data-muscle="LATS"]')).toBeNull();
  });

  it('picks the silhouette for the sex and view it was given', () => {
    const { container: front } = render(<MuscleSilhouette sex="female" view="front" />);
    const { container: back } = render(<MuscleSilhouette sex="male" view="back" />);

    expect(front.querySelector('img')).toHaveAttribute('data-silhouette', 'female/front');
    expect(back.querySelector('img')).toHaveAttribute('data-silhouette', 'male/back');
  });

  it('uses the running and rest bodies, which carry no muscles at all', () => {
    const { container: run } = render(<MuscleSilhouette sex="male" variant="running" />);
    const { container: rest } = render(<MuscleSilhouette sex="female" variant="rest" />);

    expect(run.querySelector('img')).toHaveAttribute('data-silhouette', 'male/run');
    expect(rest.querySelector('img')).toHaveAttribute('data-silhouette', 'female/rest');
  });

  it('never draws muscles on a running or rest body, even if asked', () => {
    // A run has no muscle map, but the caller should not have to remember that.
    const { container } = render(
      <MuscleSilhouette sex="male" variant="running" muscles={{ QUADRICEPS: 'primary' }} />,
    );

    expect(container.querySelectorAll('[data-muscle]')).toHaveLength(0);
  });

  it('is decorative unless it is given a label', () => {
    const { container } = render(<MuscleSilhouette sex="male" view="front" />);

    expect(container.querySelector('img')).toHaveAttribute('aria-hidden', 'true');
    expect(screen.queryByRole('img')).not.toBeInTheDocument();
  });

  it('becomes a named image when the caller labels it', () => {
    render(<MuscleSilhouette sex="male" view="front" label="Músculos trabajados hoy" />);

    expect(screen.getByRole('img', { name: 'Músculos trabajados hoy' })).toBeInTheDocument();
  });
});
