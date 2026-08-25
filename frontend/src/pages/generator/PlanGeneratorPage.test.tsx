import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { PlanGeneratorPage } from './PlanGeneratorPage';
import { generatePlanDraft, getEnergyRequirement } from '../../api/planGenerator';

vi.mock('../../api/planGenerator', () => ({
  getEnergyRequirement: vi.fn(),
  generatePlanDraft: vi.fn(),
}));

const energyMock = vi.mocked(getEnergyRequirement);
const generateMock = vi.mocked(generatePlanDraft);

/** El ejemplo del mockup: hombre, 45 años, 75 kg, 182 cm, moderado. */
const REQUIREMENT = {
  basalKcal: 1668,
  activityFactor: 1.55,
  dailyKcal: 2585,
  objectiveFactor: 1,
  planKcal: 2585,
};

function renderFunnel() {
  const user = userEvent.setup();
  render(
    <MemoryRouter>
      <PlanGeneratorPage />
    </MemoryRouter>,
  );
  return user;
}

/** Rellena el paso 1 con el ejemplo del mockup y pasa al 2. */
async function completeStepOne(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText('Edad'), '45');
  await user.type(screen.getByLabelText('Peso'), '75');
  await user.type(screen.getByLabelText('Altura'), '182');
  await waitFor(() => expect(screen.getByRole('button', { name: /Siguiente/ })).toBeEnabled());
  await user.click(screen.getByRole('button', { name: /Siguiente/ }));
}

describe('PlanGeneratorPage — el embudo público', () => {
  beforeEach(() => {
    energyMock.mockReset();
    energyMock.mockResolvedValue(REQUIREMENT);
    generateMock.mockReset();
    generateMock.mockResolvedValue({ email: 'diego@ejemplo.com', planKcal: 2585, mealsPerDay: 5 });
  });

  it('empieza en el primer paso', () => {
    renderFunnel();

    expect(screen.getByRole('heading', { name: '¿Quién eres?', level: 2 })).toBeInTheDocument();
  });

  /**
   * El cálculo lo hace el servidor.
   *
   * <p>Mifflin-St Jeor tiene que estar en el backend para generar el plan de verdad; escrita
   * también aquí sería libre de separarse, y el número que convence a alguien dejaría de ser
   * el número con el que se construye su plan.
   *
   * <p>Esto lo guardaba antes el desglose («GEB 1668 kcal», «GET 2585 kcal»), que FOR-190
   * cambió por una sola cifra. Fijar la que queda comprobaría menos que antes, así que en su
   * lugar se comprueba la regla directamente: la pantalla pinta lo que responde el servidor.
   * El segundo caso usa una cifra que NINGUNA fórmula daría para esos datos — si algún día
   * alguien vuelve a calcular en React, sale 2585 y el test cae.
   */
  it('pide el requerimiento al servidor y lo pinta', async () => {
    const user = renderFunnel();

    await user.type(screen.getByLabelText('Edad'), '45');
    await user.type(screen.getByLabelText('Peso'), '75');
    await user.type(screen.getByLabelText('Altura'), '182');

    // El formateador español no agrupa cuatro cifras: 2585, no 2.585.
    expect(await screen.findByText('2585')).toBeInTheDocument();
    await waitFor(() =>
      expect(energyMock).toHaveBeenCalledWith(
        expect.objectContaining({ sex: 'MALE', ageYears: 45, weightKg: 75, heightCm: 182 }),
      ),
    );
  });

  it('pinta la cifra del servidor y no una calculada aquí', async () => {
    energyMock.mockResolvedValue({ ...REQUIREMENT, dailyKcal: 9999 });
    const user = renderFunnel();

    await user.type(screen.getByLabelText('Edad'), '45');
    await user.type(screen.getByLabelText('Peso'), '75');
    await user.type(screen.getByLabelText('Altura'), '182');

    expect(await screen.findByText('9999')).toBeInTheDocument();
    expect(screen.queryByText('2585')).not.toBeInTheDocument();
  });

  /**
   * Las dos formas de dar una medida escriben el mismo dato.
   *
   * <p>El deslizador es lo que hace rápido el paso en el móvil, pero 74,5 kg con el pulgar
   * es lotería: el campo numérico tiene que seguir ahí, y los dos tienen que mover el mismo
   * valor o la pantalla mandaría al servidor algo distinto de lo que se ve.
   */
  it('deja dar el peso tecleándolo o arrastrando', async () => {
    const user = renderFunnel();

    await user.type(screen.getByLabelText('Peso'), '75');
    expect(screen.getByLabelText('Peso (deslizador)')).toHaveValue('75');

    fireEvent.change(screen.getByLabelText('Altura (deslizador)'), { target: { value: '182' } });
    expect(screen.getByLabelText('Altura')).toHaveValue(182);
  });

  /** Nada que calcular sin datos: no se llama al servidor por una pantalla vacía. */
  it('no pide nada mientras faltan datos', async () => {
    const user = renderFunnel();

    await user.type(screen.getByLabelText('Edad'), '45');

    await waitFor(() => expect(energyMock).not.toHaveBeenCalled());
    expect(screen.getByRole('button', { name: /Siguiente/ })).toBeDisabled();
  });

  /** El nivel de actividad se pregunta una vez, en el paso donde se ve multiplicar. */
  it('pregunta el nivel de actividad solo en el primer paso', async () => {
    const user = renderFunnel();

    expect(screen.getByRole('radio', { name: /Moderado/ })).toBeInTheDocument();

    await completeStepOne(user);

    expect(screen.queryByRole('radio', { name: /Sedentario/ })).not.toBeInTheDocument();
  });

  /**
   * Las restricciones con candado; las patologías, ni eso.
   *
   * <p>Las restricciones alimentarias existen y hoy nada sabe convertirlas en un menú
   * distinto: se enseña que llegarán y no se piden. Las patologías no se enseñan siquiera
   * — anunciarlas prometía una capacidad clínica que no hay, e invitaba a pensar en datos
   * de salud dentro de un embudo público que no los recoge.
   */
  it('enseña las restricciones con candado y no menciona patologías', async () => {
    const user = renderFunnel();
    await completeStepOne(user);

    expect(screen.getByText(/Restricciones alimentarias/)).toBeInTheDocument();
    expect(screen.queryByText(/patologías/i)).not.toBeInTheDocument();
    expect(screen.queryByRole('checkbox', { name: /Hipertensión/ })).not.toBeInTheDocument();
    expect(screen.queryByRole('radio', { name: /Hipertensión/ })).not.toBeInTheDocument();
  });

  it('no deja avanzar sin objetivo', async () => {
    const user = renderFunnel();
    await completeStepOne(user);

    expect(screen.getByRole('button', { name: /Siguiente/ })).toBeDisabled();

    await user.click(screen.getByRole('radio', { name: /Pérdida de peso/ }));

    expect(screen.getByRole('button', { name: /Siguiente/ })).toBeEnabled();
  });

  /** Vegetariana y vegana no se ofrecen: el catálogo no da para cumplirlas. */
  it('ofrece solo los estilos que el catálogo aguanta', async () => {
    const user = renderFunnel();
    await completeStepOne(user);
    await user.click(screen.getByRole('radio', { name: /Mantenimiento/ }));
    await user.click(screen.getByRole('button', { name: /Siguiente/ }));

    expect(screen.getByRole('radio', { name: /Estándar español/ })).toBeInTheDocument();
    expect(screen.getByRole('radio', { name: /Mediterránea/ })).toBeInTheDocument();
    expect(screen.queryByRole('radio', { name: /Vegetariana/ })).not.toBeInTheDocument();
    expect(screen.getByText(/Dietas vegetariana y vegana/)).toBeInTheDocument();
  });

  it('se puede volver atrás sin perder lo escrito', async () => {
    const user = renderFunnel();
    await completeStepOne(user);

    await user.click(screen.getByRole('button', { name: /Anterior/ }));

    expect(screen.getByLabelText('Edad')).toHaveValue(45);
  });

  /** El consentimiento empieza sin marcar, y sin él no se envía nada. */
  it('exige aceptar el aviso de privacidad', async () => {
    const user = renderFunnel();
    await completeStepOne(user);
    await user.click(screen.getByRole('radio', { name: /Mantenimiento/ }));
    await user.click(screen.getByRole('button', { name: /Siguiente/ }));
    await user.click(screen.getByRole('button', { name: /Siguiente/ }));

    await user.type(screen.getByLabelText('Nombre'), 'Diego');
    await user.type(screen.getByLabelText('Email'), 'diego@ejemplo.com');

    expect(screen.getByRole('button', { name: /Generar mi plan/ })).toBeDisabled();

    await user.click(screen.getByRole('checkbox', { name: /acepto el aviso de privacidad/ }));

    expect(screen.getByRole('button', { name: /Generar mi plan/ })).toBeEnabled();
  });

  /**
   * Leer el aviso de privacidad no puede costar el embudo.
   *
   * <p>Costaba. Era un `Link` a `/privacidad`, y navegar desmontaba `PlanGeneratorPage` con
   * todo su estado dentro: quien lo leía volvía al paso 1 en blanco, y le tocaba responder
   * las cuatro pantallas otra vez con el correo ya escrito. En un modal el paso 4 no se va a
   * ninguna parte.
   *
   * <p>Se comprueba el correo y no el paso, porque volver al paso 1 con los datos puestos
   * sería igual de malo: lo que hay que defender es que no se pierde NADA.
   */
  it('deja leer el aviso de privacidad sin perder lo escrito', async () => {
    const user = renderFunnel();
    await completeStepOne(user);
    await user.click(screen.getByRole('radio', { name: /Mantenimiento/ }));
    await user.click(screen.getByRole('button', { name: /Siguiente/ }));
    await user.click(screen.getByRole('button', { name: /Siguiente/ }));

    await user.type(screen.getByLabelText('Nombre'), 'Diego');
    await user.type(screen.getByLabelText('Email'), 'diego@ejemplo.com');

    await user.click(screen.getByRole('button', { name: 'aviso de privacidad' }));

    const dialog = screen.getByRole('dialog');
    expect(
      within(dialog).getByRole('heading', { name: /Aviso de privacidad/ }),
    ).toBeInTheDocument();
    // El texto de verdad, no un enlace a él: la casilla de al lado dice «he leído».
    expect(within(dialog).getByText(/Quién es responsable/)).toBeInTheDocument();

    await user.click(within(dialog).getByRole('button', { name: /Cerrar/ }));

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(screen.getByLabelText('Email')).toHaveValue('diego@ejemplo.com');
    expect(screen.getByLabelText('Nombre')).toHaveValue('Diego');
    expect(screen.getByRole('heading', { name: /¿Dónde te lo enviamos\?/ })).toBeInTheDocument();
  });

  /** El origen es opcional y por defecto nadie dice nada; el embudo no debe mandar un valor. */
  it('manda el origen solo cuando se elige uno', async () => {
    const user = renderFunnel();
    await completeStepOne(user);
    await user.click(screen.getByRole('radio', { name: /Mantenimiento/ }));
    await user.click(screen.getByRole('button', { name: /Siguiente/ }));
    await user.click(screen.getByRole('button', { name: /Siguiente/ }));

    const origen = screen.getByLabelText(/Cómo nos encontraste/);
    expect(origen).toHaveValue('');

    await user.selectOptions(origen, 'Redes sociales');

    expect(origen).toHaveValue('Redes sociales');
  });

  it('manda el embudo entero y enseña la pantalla final', async () => {
    const user = renderFunnel();
    await completeStepOne(user);
    await user.click(screen.getByRole('radio', { name: /Pérdida de peso/ }));
    await user.click(screen.getByRole('button', { name: /Siguiente/ }));
    await user.click(screen.getByRole('button', { name: /Siguiente/ }));
    await user.type(screen.getByLabelText('Nombre'), 'Diego');
    await user.type(screen.getByLabelText('Email'), 'diego@ejemplo.com');
    await user.click(screen.getByRole('checkbox', { name: /acepto el aviso de privacidad/ }));
    await user.click(screen.getByRole('button', { name: /Generar mi plan/ }));

    await waitFor(() =>
      expect(generateMock).toHaveBeenCalledWith(
        expect.objectContaining({
          sex: 'MALE',
          ageYears: 45,
          objective: 'WEIGHT_LOSS',
          daysPerWeek: 5,
          mealsPerDay: 5,
          email: 'diego@ejemplo.com',
          acceptsPrivacyPolicy: true,
        }),
      ),
    );
    expect(await screen.findByRole('heading', { name: /Tenemos tus datos/ })).toBeInTheDocument();
  });

  /**
   * La pantalla final no promete lo que no existe.
   *
   * <p>Hoy no se genera plan, no se manda correo y no se guarda nada. Un botón de descarga que
   * no descarga es la forma más rápida de que alguien deje de creerse el resto.
   */
  it('no ofrece descargar un PDF que todavía no existe', async () => {
    const user = renderFunnel();
    await completeStepOne(user);
    await user.click(screen.getByRole('radio', { name: /Mantenimiento/ }));
    await user.click(screen.getByRole('button', { name: /Siguiente/ }));
    await user.click(screen.getByRole('button', { name: /Siguiente/ }));
    await user.type(screen.getByLabelText('Nombre'), 'Diego');
    await user.type(screen.getByLabelText('Email'), 'diego@ejemplo.com');
    await user.click(screen.getByRole('checkbox', { name: /acepto el aviso de privacidad/ }));
    await user.click(screen.getByRole('button', { name: /Generar mi plan/ }));

    await screen.findByRole('heading', { name: /Tenemos tus datos/ });
    expect(screen.queryByRole('button', { name: /Descargar/ })).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Crear mi cuenta/ })).toBeInTheDocument();
  });

  it('avisa cuando el servidor rechaza el embudo', async () => {
    generateMock.mockRejectedValue(new Error('nope'));
    const user = renderFunnel();
    await completeStepOne(user);
    await user.click(screen.getByRole('radio', { name: /Mantenimiento/ }));
    await user.click(screen.getByRole('button', { name: /Siguiente/ }));
    await user.click(screen.getByRole('button', { name: /Siguiente/ }));
    await user.type(screen.getByLabelText('Nombre'), 'Diego');
    await user.type(screen.getByLabelText('Email'), 'diego@ejemplo.com');
    await user.click(screen.getByRole('checkbox', { name: /acepto el aviso de privacidad/ }));
    await user.click(screen.getByRole('button', { name: /Generar mi plan/ }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/No se pudo generar/);
  });

  /** Las cuatro opciones del paso 2, y su descripción, tal como las pide el mockup. */
  it('ofrece los cuatro objetivos', async () => {
    const user = renderFunnel();
    await completeStepOne(user);

    const group = screen.getByRole('group', { name: 'Objetivo principal' });
    for (const label of [/Pérdida de peso/, /Ganancia muscular/, /Mantenimiento/, /Comer sano/]) {
      expect(within(group).getByRole('radio', { name: label })).toBeInTheDocument();
    }
  });
});
