import { render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';
import { PreproRibbon } from './PreproRibbon';
import { isPreproHost } from './preproHost';

/**
 * `window.location` is not writable in jsdom, so each case installs a stand-in
 * and the teardown puts the real one back. Redefining is the only way to drive
 * a component that reads the host it is running on.
 */
const realLocation = window.location;
function atHost(hostname: string) {
  Object.defineProperty(window, 'location', {
    value: { ...realLocation, hostname },
    writable: true,
    configurable: true,
  });
}
afterEach(() => {
  Object.defineProperty(window, 'location', {
    value: realLocation,
    writable: true,
    configurable: true,
  });
});

describe('isPreproHost', () => {
  it('reconoce los dos dominios de preproducción y sus subdominios', () => {
    expect(isPreproHost('forma.diegobarrioh.dev')).toBe(true);
    expect(isPreproHost('akademia.diegobarrioh.dev')).toBe(true);
    expect(isPreproHost('diegobarrioh.dev')).toBe(true);
    expect(isPreproHost('tokenmeter.backendtothefuture.com')).toBe(true);
    expect(isPreproHost('backendtothefuture.com')).toBe(true);
  });

  it('no reconoce localhost ni un dominio de producción', () => {
    expect(isPreproHost('localhost')).toBe(false);
    expect(isPreproHost('forma.app')).toBe(false);
  });

  /*
   * El sufijo se compara por etiqueta de dominio, no por `endsWith`: un dominio
   * ajeno que TERMINE en el nuestro es de otro y no debe encender el aviso.
   */
  it('no se deja engañar por un dominio que solo termina igual', () => {
    expect(isPreproHost('notdiegobarrioh.dev')).toBe(false);
    expect(isPreproHost('evil-backendtothefuture.com')).toBe(false);
  });
});

describe('PreproRibbon', () => {
  it('no pinta nada fuera de preproducción', () => {
    atHost('forma.app');
    const { container } = render(<PreproRibbon sha="bf2974c" />);
    expect(container).toBeEmptyDOMElement();
  });

  it('anuncia PREPRO y el commit desplegado', () => {
    atHost('forma.diegobarrioh.dev');
    render(<PreproRibbon sha="bf2974c" />);
    expect(screen.getByText('PREPRO')).toBeInTheDocument();
    expect(screen.getByText('bf2974c')).toBeInTheDocument();
  });

  /* Sin sha (build sin git y sin arg) el aviso sigue valiendo; la línea sobra. */
  it('sin sha mantiene el aviso y omite la segunda línea', () => {
    atHost('forma.diegobarrioh.dev');
    render(<PreproRibbon sha="" />);
    expect(screen.getByText('PREPRO')).toBeInTheDocument();
    expect(screen.queryByTestId('prepro-sha')).not.toBeInTheDocument();
  });
});
