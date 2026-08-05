-- La cuenta heredada tenía dueño, y no era el marcador.
--
-- Additive on top of V56 (ADR-003). No crea nada: reasigna.
--
-- QUÉ PASÓ. V26 creó una fila de usuario para los datos que ya existían antes de que hubiera login
-- —el PLACEHOLDER_USER_ID 00000000-…, con la contraseña '!' y is_active FALSE— y las migraciones
-- V27 a V34 colgaron de ella todo el histórico. Después alguien se registró de verdad, y
-- UserService.register mintió un UUID nuevo. El círculo nunca se cerró: el perfil, las mediciones,
-- los objetivos, los registros, la lista de compra, las integraciones y los dos planes de
-- alimentación se quedaron en el marcador, y la cuenta real nació vacía.
--
-- El síntoma que se ve: la pantalla de nutrición está en blanco. /nutrition/days/{type} lee el plan
-- activo DE QUIEN LLAMA, y quien llama no tenía ninguno.
--
-- CÓMO SE IDENTIFICA A QUIÉN ADOPTAR. La única cuenta que no es el marcador. Sin correos escritos
-- aquí: no hacen falta y no pintan nada en un repositorio. Si hay cero cuentas reales, o hay más de
-- una, esta migración NO TOCA NADA — no puede saber de quién es qué, y adivinarlo sería peor que no
-- hacerlo. Cada UPDATE lleva esa condición dentro; ninguno depende de que otro se haya ejecutado.
--
-- QUÉ GANA CUANDO CHOCAN. Lo de la cuenta real. Lo que alguien haya escrito después de registrarse
-- es más reciente y más suyo, así que solo se mueven las filas que no chocan con una que ya exista.
-- Nada se sobrescribe. Las que no pueden moverse se quedan donde están: borrarlas sería destruir
-- datos para dejar bonito un marcador que nadie va a mirar.
--
-- DIEZ TABLAS PUEDEN CHOCAR y siete no. Las que llevan user_id en su clave —perfil, logros,
-- integraciones, estado de sesiones, productos de compra, insights— necesitan el NOT EXISTS. Las que
-- tienen id propio —mediciones, objetivos, registros de comida y agua, fotos, listas, seguimiento
-- semanal— se mueven enteras. nutrition_plan no lleva user_id en la clave pero sí en un índice único
-- con active_marker, así que se trata como las primeras.

-- ---------------------------------------------------------------------------------------------
-- Tablas sin choque posible: id propio, nada que comparar.
-- ---------------------------------------------------------------------------------------------

UPDATE body_measurements SET user_id = (SELECT id FROM users WHERE id <> '00000000-0000-0000-0000-000000000000')
 WHERE user_id = '00000000-0000-0000-0000-000000000000'
   AND (SELECT COUNT(*) FROM users WHERE id <> '00000000-0000-0000-0000-000000000000') = 1;

UPDATE goal SET user_id = (SELECT id FROM users WHERE id <> '00000000-0000-0000-0000-000000000000')
 WHERE user_id = '00000000-0000-0000-0000-000000000000'
   AND (SELECT COUNT(*) FROM users WHERE id <> '00000000-0000-0000-0000-000000000000') = 1;

UPDATE meal_log_entry SET user_id = (SELECT id FROM users WHERE id <> '00000000-0000-0000-0000-000000000000')
 WHERE user_id = '00000000-0000-0000-0000-000000000000'
   AND (SELECT COUNT(*) FROM users WHERE id <> '00000000-0000-0000-0000-000000000000') = 1;

UPDATE water_intake_entry SET user_id = (SELECT id FROM users WHERE id <> '00000000-0000-0000-0000-000000000000')
 WHERE user_id = '00000000-0000-0000-0000-000000000000'
   AND (SELECT COUNT(*) FROM users WHERE id <> '00000000-0000-0000-0000-000000000000') = 1;

UPDATE progress_photo SET user_id = (SELECT id FROM users WHERE id <> '00000000-0000-0000-0000-000000000000')
 WHERE user_id = '00000000-0000-0000-0000-000000000000'
   AND (SELECT COUNT(*) FROM users WHERE id <> '00000000-0000-0000-0000-000000000000') = 1;

UPDATE shopping_lists SET user_id = (SELECT id FROM users WHERE id <> '00000000-0000-0000-0000-000000000000')
 WHERE user_id = '00000000-0000-0000-0000-000000000000'
   AND (SELECT COUNT(*) FROM users WHERE id <> '00000000-0000-0000-0000-000000000000') = 1;

UPDATE weekly_tracking_record SET user_id = (SELECT id FROM users WHERE id <> '00000000-0000-0000-0000-000000000000')
 WHERE user_id = '00000000-0000-0000-0000-000000000000'
   AND (SELECT COUNT(*) FROM users WHERE id <> '00000000-0000-0000-0000-000000000000') = 1;

-- ---------------------------------------------------------------------------------------------
-- Tablas donde la cuenta real puede tener ya su fila: se mueve solo lo que no choca.
-- ---------------------------------------------------------------------------------------------

-- El perfil: altura, baseline y objetivos que V20 transcribió de la hoja Perfil. Solo llega si la
-- cuenta real no tiene perfil propio; si se registró y rellenó el onboarding, el suyo manda.
UPDATE user_profile p SET user_id = (SELECT id FROM users WHERE id <> '00000000-0000-0000-0000-000000000000')
 WHERE p.user_id = '00000000-0000-0000-0000-000000000000'
   AND (SELECT COUNT(*) FROM users WHERE id <> '00000000-0000-0000-0000-000000000000') = 1
   AND NOT EXISTS (SELECT 1 FROM user_profile o
                    WHERE o.user_id <> '00000000-0000-0000-0000-000000000000');

UPDATE earned_achievement e SET user_id = (SELECT id FROM users WHERE id <> '00000000-0000-0000-0000-000000000000')
 WHERE e.user_id = '00000000-0000-0000-0000-000000000000'
   AND (SELECT COUNT(*) FROM users WHERE id <> '00000000-0000-0000-0000-000000000000') = 1
   AND NOT EXISTS (SELECT 1 FROM earned_achievement o
                    WHERE o.user_id <> '00000000-0000-0000-0000-000000000000'
                      AND o.achievement_id = e.achievement_id);

UPDATE training_session_status t SET user_id = (SELECT id FROM users WHERE id <> '00000000-0000-0000-0000-000000000000')
 WHERE t.user_id = '00000000-0000-0000-0000-000000000000'
   AND (SELECT COUNT(*) FROM users WHERE id <> '00000000-0000-0000-0000-000000000000') = 1
   AND NOT EXISTS (SELECT 1 FROM training_session_status o
                    WHERE o.user_id <> '00000000-0000-0000-0000-000000000000'
                      AND o.session_id = t.session_id);

UPDATE shopping_products s SET user_id = (SELECT id FROM users WHERE id <> '00000000-0000-0000-0000-000000000000')
 WHERE s.user_id = '00000000-0000-0000-0000-000000000000'
   AND (SELECT COUNT(*) FROM users WHERE id <> '00000000-0000-0000-0000-000000000000') = 1
   AND NOT EXISTS (SELECT 1 FROM shopping_products o
                    WHERE o.user_id <> '00000000-0000-0000-0000-000000000000'
                      AND o.store_product_id = s.store_product_id);

-- Las tres tablas de integración van juntas conceptualmente —conexión, token y marcador de medidas—
-- pero cada una tiene su propia clave, así que cada una comprueba la suya.
UPDATE integration_connection i SET user_id = (SELECT id FROM users WHERE id <> '00000000-0000-0000-0000-000000000000')
 WHERE i.user_id = '00000000-0000-0000-0000-000000000000'
   AND (SELECT COUNT(*) FROM users WHERE id <> '00000000-0000-0000-0000-000000000000') = 1
   AND NOT EXISTS (SELECT 1 FROM integration_connection o
                    WHERE o.user_id <> '00000000-0000-0000-0000-000000000000'
                      AND o.provider = i.provider);

UPDATE integration_token i SET user_id = (SELECT id FROM users WHERE id <> '00000000-0000-0000-0000-000000000000')
 WHERE i.user_id = '00000000-0000-0000-0000-000000000000'
   AND (SELECT COUNT(*) FROM users WHERE id <> '00000000-0000-0000-0000-000000000000') = 1
   AND NOT EXISTS (SELECT 1 FROM integration_token o
                    WHERE o.user_id <> '00000000-0000-0000-0000-000000000000'
                      AND o.provider = i.provider);

UPDATE integration_measure_marker i SET user_id = (SELECT id FROM users WHERE id <> '00000000-0000-0000-0000-000000000000')
 WHERE i.user_id = '00000000-0000-0000-0000-000000000000'
   AND (SELECT COUNT(*) FROM users WHERE id <> '00000000-0000-0000-0000-000000000000') = 1
   AND NOT EXISTS (SELECT 1 FROM integration_measure_marker o
                    WHERE o.user_id <> '00000000-0000-0000-0000-000000000000'
                      AND o.provider = i.provider AND o.grpid = i.grpid);

-- Los estados de OAuth a medias no se mueven: son de un solo uso y caducan. Arrastrar el intento de
-- conexión de otra sesión no ayuda a nadie y podría confundir un callback en vuelo.
DELETE FROM integration_oauth_state
 WHERE user_id = '00000000-0000-0000-0000-000000000000'
   AND (SELECT COUNT(*) FROM users WHERE id <> '00000000-0000-0000-0000-000000000000') = 1;

-- ---------------------------------------------------------------------------------------------
-- Los planes. El índice único (user_id, active_marker) admite un solo activo por cuenta, así que
-- el plan sembrado solo se mueve como activo si la cuenta real no sigue ya otro; si lo sigue, llega
-- igualmente pero como COMPLETED, porque un plan que existe vale más que un plan que se descarta.
-- ---------------------------------------------------------------------------------------------

UPDATE nutrition_plan SET status = 'COMPLETED', active_marker = NULL
 WHERE user_id = '00000000-0000-0000-0000-000000000000'
   AND active_marker IS NOT NULL
   AND (SELECT COUNT(*) FROM users WHERE id <> '00000000-0000-0000-0000-000000000000') = 1
   AND EXISTS (SELECT 1 FROM nutrition_plan o
                WHERE o.user_id <> '00000000-0000-0000-0000-000000000000'
                  AND o.active_marker IS NOT NULL);

UPDATE nutrition_plan SET user_id = (SELECT id FROM users WHERE id <> '00000000-0000-0000-0000-000000000000')
 WHERE user_id = '00000000-0000-0000-0000-000000000000'
   AND (SELECT COUNT(*) FROM users WHERE id <> '00000000-0000-0000-0000-000000000000') = 1;

-- ---------------------------------------------------------------------------------------------
-- Los insights, que son el caso raro.
--
-- insight_history_recommendation tiene una FK COMPUESTA (user_id, week_start_date) contra su padre.
-- Un UPDATE del padre deja huérfanos a los hijos; uno de los hijos apunta a un padre que aún no
-- existe. No hay orden que funcione, así que los hijos salen a una tabla temporal, se mueve el
-- padre y vuelven a entrar bajo el dueño nuevo. Una FK compuesta sobre la columna que se está
-- cambiando es exactamente el caso que un UPDATE no puede resolver solo.
-- ---------------------------------------------------------------------------------------------

-- Cada paso lleva la misma condición que los demás. Sin ella el DELETE de abajo se ejecutaba
-- siempre —también en una instalación sin nadie registrado, donde no hay a quién adoptar— y las
-- recomendaciones no volvían nunca: se perdían. Lo cazó el test de V34, no la lectura.
CREATE TABLE tmp_v57_recommendations AS
SELECT * FROM insight_history_recommendation
 WHERE user_id = '00000000-0000-0000-0000-000000000000'
   AND (SELECT COUNT(*) FROM users WHERE id <> '00000000-0000-0000-0000-000000000000') = 1;

DELETE FROM insight_history_recommendation
 WHERE user_id = '00000000-0000-0000-0000-000000000000'
   AND (SELECT COUNT(*) FROM users WHERE id <> '00000000-0000-0000-0000-000000000000') = 1;

UPDATE insight_history h SET user_id = (SELECT id FROM users WHERE id <> '00000000-0000-0000-0000-000000000000')
 WHERE h.user_id = '00000000-0000-0000-0000-000000000000'
   AND (SELECT COUNT(*) FROM users WHERE id <> '00000000-0000-0000-0000-000000000000') = 1
   AND NOT EXISTS (SELECT 1 FROM insight_history o
                    WHERE o.user_id <> '00000000-0000-0000-0000-000000000000'
                      AND o.week_start_date = h.week_start_date);

-- Las de las semanas que SÍ se movieron, bajo su padre nuevo. La condición NOT EXISTS distingue un
-- padre que se movió de uno que la cuenta real ya tenía: sin ella, las hijas de una semana que chocó
-- se colarían bajo el padre ajeno.
INSERT INTO insight_history_recommendation
  (user_id, week_start_date, sort_order, is_main, category, severity, message, reason,
   related_metric, created_at)
SELECT h.user_id, t.week_start_date, t.sort_order, t.is_main, t.category, t.severity, t.message,
       t.reason, t.related_metric, t.created_at
  FROM tmp_v57_recommendations t
  JOIN insight_history h ON h.week_start_date = t.week_start_date
 WHERE h.user_id <> '00000000-0000-0000-0000-000000000000'
   AND NOT EXISTS (SELECT 1 FROM insight_history p
                    WHERE p.user_id = '00000000-0000-0000-0000-000000000000'
                      AND p.week_start_date = t.week_start_date);

-- Y las de las semanas que no pudieron moverse vuelven donde estaban. Sacarlas para mover el padre
-- y no devolverlas sería destruirlas por un motivo puramente mecánico.
INSERT INTO insight_history_recommendation
  (user_id, week_start_date, sort_order, is_main, category, severity, message, reason,
   related_metric, created_at)
SELECT '00000000-0000-0000-0000-000000000000', t.week_start_date, t.sort_order, t.is_main,
       t.category, t.severity, t.message, t.reason, t.related_metric, t.created_at
  FROM tmp_v57_recommendations t
  JOIN insight_history h ON h.week_start_date = t.week_start_date
                        AND h.user_id = '00000000-0000-0000-0000-000000000000';

DROP TABLE tmp_v57_recommendations;

-- El marcador se queda como fila de usuario: V26 lo dejó inactivo y con una contraseña imposible, y
-- borrarlo rompería las claves ajenas de cualquier fila que no se haya podido mover. Lo que cambia
-- es que ya no es dueño de nada que importe.
