-- =================================================================================================
-- V61 — El embudo deja de tirar a la basura a quien lo termina.
--
-- Hasta aquí `POST /api/v1/public/plan-generator` validaba las cuatro pantallas y respondía que sí.
-- Nada más. Alguien rellenaba el embudo entero, daba su correo, veía una pantalla de éxito, y no
-- sobrevivía ni un rastro suyo a la petición. El propio controlador lo decía en mayúsculas y lo
-- llamaba «lo primero que hay que arreglar, antes que el plan».
--
-- Esta tabla es ese arreglo. Guarda lo que hace falta para construirle el plan cuando el generador
-- exista, y la prueba de que consintió.
--
-- POR QUÉ TABLA PROPIA Y NO `users`
--
-- Un lead no es una cuenta. No tiene contraseña, no puede entrar, y la mayoría nunca se registrará.
-- Meterlo en `users` obligaría a inventar una fila de usuario sin credenciales que el resto de la
-- aplicación tendría que aprender a esquivar en cada consulta — y `users.email` es UNIQUE, así que
-- pedir dos planes desde el mismo correo, o pedir uno y registrarse después, chocaría contra el
-- índice. Cuando alguien se registre, será una fila de `users` con su propio correo; enlazar las
-- dos cosas es otra historia y necesita su decisión.
--
-- QUÉ NO ESTÁ AQUÍ
--
-- Ni patologías, ni alergias, ni restricciones alimentarias. El embudo no las pide —van con candado
-- en las pantallas 2 y 3— y `PlanDraftRequest` no tiene dónde recibirlas. Son datos de salud,
-- categoría especial del artículo 9 del RGPD, y recogerlos en un formulario público para no usarlos
-- todavía sería lo peor de las dos opciones.
--
-- Sexo, edad, peso y altura SÍ están: son la petición misma. Sin ellos la fila no sirve para
-- construir nada, que es lo único para lo que existe.
-- =================================================================================================

CREATE TABLE plan_lead (
    id                      UUID PRIMARY KEY,

    -- Lo que identifica a una persona. `email` NO es único: la misma persona puede pedir un plan
    -- dos veces, y la segunda petición es un hecho distinto de la primera, con otras respuestas y
    -- otra fecha. Deduplicar aquí perdería la petición más reciente o la más antigua, y ninguna de
    -- las dos es basura.
    full_name               VARCHAR(120)             NOT NULL,
    email                   VARCHAR(320)             NOT NULL,
    country                 VARCHAR(64),
    heard_about_us          VARCHAR(64),

    -- La petición: con esto y la fórmula se construye el plan que se prometió.
    sex                     VARCHAR(16)              NOT NULL,
    age_years               INTEGER                  NOT NULL,
    weight_kg               NUMERIC(5, 1)            NOT NULL,
    height_cm               NUMERIC(5, 1)            NOT NULL,
    activity_level          VARCHAR(32)              NOT NULL,
    objective               VARCHAR(32)              NOT NULL,
    days_per_week           INTEGER                  NOT NULL,
    meals_per_day           INTEGER                  NOT NULL,
    eating_style            VARCHAR(64)              NOT NULL,

    -- El requerimiento que se le enseñó en pantalla. Se guarda calculado y no se recalcula al leer:
    -- es lo que esa persona vio y aceptó. Si la fórmula cambia mañana, el plan que reciba tiene que
    -- poder compararse con la cifra que le convenció, no con la que daría hoy.
    plan_kcal               INTEGER                  NOT NULL,

    -- LA PRUEBA DEL CONSENTIMIENTO.
    --
    -- No basta con guardar un booleano: el artículo 7.1 del RGPD pide poder demostrar que se
    -- consintió, y una demostración sin fecha ni contenido no demuestra nada. Por eso van los tres
    -- juntos: qué se aceptó, cuándo, y qué texto estaba en vigor en ese momento.
    --
    -- `accepts_privacy_policy` no lleva DEFAULT y lleva CHECK: la aplicación ya rechaza un `false`
    -- con `@AssertTrue`, y esto es la misma regla escrita donde no se puede saltar por otra vía.
    -- Un consentimiento que puede estar a falso no es un consentimiento.
    accepts_privacy_policy  BOOLEAN                  NOT NULL,
    privacy_policy_version  VARCHAR(32)              NOT NULL,

    -- Pregunta aparte, y guardada aparte. Aceptar que te manden lo que has pedido y aceptar que te
    -- escriban después son dos consentimientos distintos, y el segundo se puede retirar sin tocar
    -- el primero.
    wants_marketing         BOOLEAN                  NOT NULL,

    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT plan_lead_privacy_accepted CHECK (accepts_privacy_policy)
);

-- El borrado por retención recorre la tabla por fecha una vez al día (ver `PlanLeadRetentionJob`),
-- y atender un derecho de supresión la recorre por correo. Son las dos únicas formas en que se lee.
CREATE INDEX idx_plan_lead_created_at ON plan_lead (created_at);
CREATE INDEX idx_plan_lead_email ON plan_lead (email);
