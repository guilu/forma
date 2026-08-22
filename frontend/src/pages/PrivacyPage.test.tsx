import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { PrivacyPage } from './PrivacyPage';

/**
 * El aviso de privacidad.
 *
 * <p>No se comprueba la redacción —eso lo revisa alguien de legal—, sino las dos cosas que un
 * cambio descuidado rompe sin que se note: que estén las secciones que el artículo 13 del RGPD
 * exige, y que nadie publique la página con un marcador dentro.
 */
describe('PrivacyPage — el aviso de privacidad', () => {
  function renderPage() {
    render(
      <MemoryRouter>
        <PrivacyPage />
      </MemoryRouter>,
    );
  }

  /**
   * Lo que el modelo del que se partió NO tenía.
   *
   * <p>Era un aviso mexicano: identidad, datos, finalidades, cookies, transferencias y «derechos
   * ARCO». Bajo el RGPD faltan la base jurídica de cada tratamiento, el plazo de conservación, el
   * derecho a retirar el consentimiento y el derecho a reclamar ante la AEPD. Se fijan aquí porque
   * son justo las que se caen al reescribir el documento «para acortarlo».
   */
  it.each([
    ['la base jurídica de cada tratamiento', /Base legal \(art\. 6 RGPD\)/],
    ['el plazo de conservación', /Cuánto tiempo los guardamos/i],
    ['el derecho a retirar el consentimiento', /Retirar tu consentimiento/i],
    ['la autoridad de control', /Agencia Española de Protección de Datos/i],
  ])('declara %s', (_role, matcher) => {
    renderPage();

    expect(screen.getByText(matcher)).toBeInTheDocument();
  });

  it('enumera los seis derechos del RGPD, no los cuatro de ARCO', () => {
    renderPage();

    for (const right of [
      /^Acceso:/,
      /^Rectificación:/,
      /^Supresión:/,
      /^Limitación:/,
      /^Portabilidad:/,
      /^Oposición:/,
    ]) {
      expect(screen.getByText(right)).toBeInTheDocument();
    }
  });

  /**
   * La versión que se enseña tiene que ser la que se guarda.
   *
   * <p>Al aceptar, el servidor anota `PlanLeadService.PRIVACY_POLICY_VERSION`. Si esta página dice
   * otra fecha, la prueba del consentimiento apunta a un texto que nadie vio, que es peor que no
   * guardarla: parece una prueba y no lo es.
   */
  it('enseña la misma versión que el servidor guarda al aceptar', () => {
    renderPage();

    expect(screen.getByText(/Última actualización: 2026-08-22/)).toBeInTheDocument();
  });

  /**
   * Los marcadores se VEN.
   *
   * <p>La razón social, el NIF, el domicilio, el correo y el proveedor de alojamiento no están en el
   * repositorio, y AGENTS.md prohíbe inventarlos. La protección contra publicar el documento a
   * medias no es que alguien se acuerde: es que un hueco sin rellenar se lea en la página, en el
   * sitio donde iría el dato, y no escondido en un comentario del código.
   *
   * <p>Cuando se rellenen, este test se borra con ellos. Está escrito para que borrarlo sea el
   * paso obvio: falla en cuanto el primero deja de estar, y el mensaje dice por qué.
   */
  it('enseña en la propia página los datos que faltan por rellenar', () => {
    renderPage();

    const pending = screen.getAllByText(/\[COMPLETAR/);

    expect(
      pending.length,
      'Si ya no hay marcadores, este test sobra: bórralo junto con el último [COMPLETAR …].',
    ).toBeGreaterThan(0);
    for (const marker of pending) {
      expect(marker).toBeVisible();
    }
  });
});
