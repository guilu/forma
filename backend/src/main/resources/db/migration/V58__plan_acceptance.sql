-- =================================================================================================
-- V58 — El plan se ofrece, no se impone.
--
-- Hasta aquí la aplicación decidía si enseñar el plan mirando `user_profile.first_run_completed`,
-- un campo que responde a otra pregunta: «¿rellenó el onboarding?». Quien se registraba heredaba
-- de V57 el plan sembrado pero no el perfil que iba con él, así que entraba con un plan activo y
-- una bandera en falso — y veía la pantalla de nutrición contradecirse consigo misma: la tarjeta
-- del registro diario leía el plan por `/consumption`, sin puerta, mientras `/days/{type}` se lo
-- negaba por la bandera.
--
-- Aquí se separan las dos preguntas. El plan sembrado pasa a DRAFT: existe, está escrito y no está
-- en marcha. La primera vez que se entra, la aplicación lo ofrece; al aceptar, se activa y se
-- anota en `plan_acceptance` que esta cuenta dijo que sí.
-- =================================================================================================

CREATE TABLE plan_acceptance (
    -- La clave primaria es la cuenta: se acepta una vez, no una vez por plan. Cambiar de plan más
    -- adelante es otra cosa y tendrá su propia historia.
    user_id     UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    accepted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- -------------------------------------------------------------------------------------------------
-- El plan sembrado de V56 deja de estar en marcha y pasa a estar a la espera.
--
-- Acotado por ID a propósito. Un UPDATE por criterio amplio —«todos los planes activos», «todos los
-- sembrados»— le apagaría el plan a cualquier cuenta real que ya lo esté siguiendo, que es
-- exactamente el daño que esta migración viene a reparar en la dirección contraria.
--
-- Estado y marcador van juntos: el CHECK de V53 los ata, y DRAFT exige `active_marker` a NULL.
-- DRAFT y no COMPLETED porque el plan no se ha hecho: está sin empezar.
-- -------------------------------------------------------------------------------------------------
UPDATE nutrition_plan
   SET status = 'DRAFT', active_marker = NULL, updated_at = CURRENT_TIMESTAMP
 WHERE id = 'bbbbbbbb-0000-4000-8000-000000000001'
   AND active_marker IS NOT NULL;
