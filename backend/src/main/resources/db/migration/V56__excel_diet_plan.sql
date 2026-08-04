-- La dieta real, la que un LLM generó en la pestaña «Dieta» del Excel.
--
-- Additive on top of V55 (ADR-003). Siete días transcritos de la hoja
-- «Dieta semanal — recomposición 2200-2400 kcal», celda a celda.
--
-- POR QUÉ ENTRA COMO PLAN Y NO COMO CÓDIGO. Es exactamente lo que V53 se construyó para
-- sostener: un plan que alguien puede editar, activar y archivar. Antes de V53 esto solo
-- habría cabido como más constantes en un .java.
--
-- LAS RACIONES DEL CATÁLOGO SON LAS CANTIDADES DE ESTA HOJA, y no por casualidad: V25 se
-- sembró de esta misma dieta. La avena pesa 60 g, el whey 30, el plátano 120, el queso
-- fresco 250, el pollo 200, el arroz 80, la patata 300, el boniato 250, el salmón 180, la
-- merluza 200, las claras 150, los frutos secos 25. Cada una de esas cifras está en la
-- hoja y en food_catalog.
--
-- Por eso un alimento que la hoja nombra sin número entra como UNA RACIÓN suya (V49) y no
-- como gramos sueltos: «un plátano» es un plátano, y sigue siéndolo si mañana alguien
-- corrige lo que pesa. Solo se escriben gramos cuando la hoja dice un número distinto del
-- de la ración — el pavo, que la hoja pone a 200 g y el catálogo tiene a 150.
--
-- CANTIDADES ARRASTRADAS, Y DE DÓNDE. La hoja da los gramos una vez y repite el plato por
-- su nombre: el lunes dice «Avena 60g + whey 30g + plátano» y el jueves solo «Avena +
-- whey + plátano». Se arrastran, porque es lo que entiende cualquiera que lea la hoja, y
-- queda dicho aquí para que se pueda auditar:
--
--   jueves y sábado desayuno  <- lunes           domingo desayuno    <- miércoles cena
--   viernes comida            <- lunes/martes    sábado comida       <- martes (pavo 200 g)
--   viernes merienda          <- martes          viernes/sábado cena <- martes/lunes
--
-- LAS CIFRAS POR DÍA SON LO QUE EL MODELO DIJO, no una suma comprobada. Entran como
-- target_* porque es lo que son: lo que ese día se pide. La aplicación suma los alimentos
-- por su cuenta en cada lectura y puede decir «pediste 2320 y esto da otra cosa» — que es
-- literalmente la validación que reclama la sección 11 del documento. Si discrepan, no es
-- un error de esta migración: es el dato que el modelo tenía que dar y no dio.
--
-- LO QUE NO SE INVENTA (FOR-134):
--
--   * «Fruta», suelta cinco veces sin decir cuál, va como instrucción de la comida. El
--     catálogo tiene plátano y frutos rojos; la hoja no elige, así que aquí tampoco.
--   * La comida del domingo es «comida libre controlada: proteína + carbo + verdura».
--     Eso es una regla, no una lista, y entra sin alimentos y con instrucciones — el caso
--     exacto que la sección 8 del documento describe.
--   * La cena del domingo ofrece «pescado/huevos». Es una alternativa (sección 9), que no
--     está construida: entra la verdura, que sí es segura, y la elección como texto.
--   * NINGUNA HORA. La hoja no da ni una, y scheduled_time admite nulos.
--
-- LO QUE SE DESCUBRE AL SUMARLA, Y NO SE CORRIGE
--
-- Cargada y sumada contra el catálogo, ninguna de las cifras del modelo cuadra con su propia
-- lista de comida. Todos los días se quedan cortos, entre 379 y 702 kcal:
--
--     día   dicho   real       P dicho  P real      G dicho  G real
--       1    2320   1798           165     172           65      25
--       2    2350   1971           170     174           78      60
--       3    2250   1730           165     183           62      29
--       4    2280   1662           160     139           60      26
--       5    2300   1643           170     170           75      46
--       6    2400   1698           165     170           65      15
--
-- Parte del hueco es comida que a propósito no se cuenta: la pieza de fruta sin nombre de cinco
-- días y la comida libre del domingo. Pero una fruta son cien calorías y el lunes se queda a
-- quinientas, así que la mayor parte no es eso.
--
-- El patrón es nítido: LA PROTEÍNA LA CLAVA y todo lo demás lo sobrestima. Sea lo que fuera lo que
-- hacía el modelo al escribir esos totales, seguía la proteína y estimaba el resto.
--
-- No se corrige. Añadir comida hasta que los números cuadraran sería inventar una dieta que nadie
-- escribió, que es justo lo que las trece migraciones anteriores se han negado a hacer. La
-- aplicación suma los alimentos en cada lectura y dice el hueco, que es exactamente para lo que la
-- sección 11 del documento pedía separar objetivo de calculado. Lo pinta ExcelDietPlanTest.
--
-- LOS TIPOS DE DÍA DE LA HOJA COINCIDEN CON WeeklyTrainingDayPolicy, hasta el desglose de
-- fuerza: lunes/miércoles/sábado carrera, martes empuje, jueves tirón, domingo pierna,
-- viernes descanso. La dieta y el plan de entrenamiento salieron de la misma cabeza.

-- «Plan base» (V54) deja de ser el que se sigue. Va PRIMERO: el índice único admite un solo
-- marcador por usuario, así que insertar el nuevo antes de retirar el viejo lo rechazaría.
-- Ese es el mismo orden que sigue changeStatus en el repositorio, por el mismo motivo.
UPDATE nutrition_plan
   SET status = 'COMPLETED', active_marker = NULL, updated_at = CURRENT_TIMESTAMP
 WHERE user_id = '00000000-0000-0000-0000-000000000000'
   AND name = 'Plan base'
   AND active_marker IS NOT NULL;

-- El plan. Status y marcador van juntos: el CHECK de V53 los ata.
INSERT INTO nutrition_plan (id, user_id, name, description, objective, status, active_marker,
                            target_kcal_min, target_kcal_max, generated_by, generation_metadata)
VALUES ('bbbbbbbb-0000-4000-8000-000000000001', '00000000-0000-0000-0000-000000000000',
        'Dieta semanal — recomposición',
        'Transcrita de la pestaña «Dieta» de docs/fitness_os.xlsm.',
        'COMPOSICION', 'ACTIVE', '1', 2200, 2400, 'AI',
        '{"source":"docs/fitness_os.xlsm#Dieta","transcribedBy":"human","claimedTotals":"per-day target_*"}');

-- Día 1 — Running 4-5 km
INSERT INTO nutrition_plan_day (id, nutrition_plan_id, week_number, day_number, day_type, target_kcal, target_protein_g, target_carbs_g, target_fat_g, notes)
VALUES ('65f683e8-19d8-5f79-a784-94563530f086', 'bbbbbbbb-0000-4000-8000-000000000001', 1, 1, 'RUNNING', 2320, 165.0, 270.0, 65.0, 'Running 4-5 km');
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, instructions, optional)
VALUES ('92c8f7a5-e8d6-55c3-8310-469ceaea661c', '65f683e8-19d8-5f79-a784-94563530f086', 'BREAKFAST', 'Desayuno', 0, NULL, FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('aba8e186-7d4c-5fe9-9960-b12fa6a42753', '92c8f7a5-e8d6-55c3-8310-469ceaea661c', 'oats', NULL, 60.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('3280a9aa-3c38-5794-8cd1-3574ca803aaa', '92c8f7a5-e8d6-55c3-8310-469ceaea661c', 'whey-protein', NULL, 30.0, 1);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('ca487e3c-d759-5766-91b4-d629e27ef0d6', '92c8f7a5-e8d6-55c3-8310-469ceaea661c', 'banana', 'banana', 1.0, 2);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, instructions, optional)
VALUES ('0a7854f9-5605-50fc-85c8-41742922b1f5', '65f683e8-19d8-5f79-a784-94563530f086', 'MID_MORNING', 'Media mañana', 1, NULL, FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('d3fdfe79-7d11-58f0-bb16-d2c909a0cb1a', '0a7854f9-5605-50fc-85c8-41742922b1f5', 'fresh-cheese', NULL, 250.0, 0);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, instructions, optional)
VALUES ('39ad6f6d-11ae-5a94-b33b-3059b9eff1f9', '65f683e8-19d8-5f79-a784-94563530f086', 'LUNCH', 'Comida', 2, NULL, FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('8ddf22f7-ab72-5758-9741-ad340267fb28', '39ad6f6d-11ae-5a94-b33b-3059b9eff1f9', 'chicken', NULL, 200.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('f615d817-4dca-56bd-9349-bd3627674fc6', '39ad6f6d-11ae-5a94-b33b-3059b9eff1f9', 'rice', NULL, 80.0, 1);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('1ee44d9f-1158-55b8-b8ef-8f6b06a5debf', '39ad6f6d-11ae-5a94-b33b-3059b9eff1f9', 'vegetables', 'vegetables', 1.0, 2);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('6342808e-eb44-5508-be08-8b82159858e9', '39ad6f6d-11ae-5a94-b33b-3059b9eff1f9', 'olive-oil', 'olive-oil', 1.0, 3);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, instructions, optional)
VALUES ('71bf86df-c759-576b-9c18-61cc05dcbcc8', '65f683e8-19d8-5f79-a784-94563530f086', 'SNACK', 'Merienda', 3, 'Con una pieza de fruta. El Excel no dice cuál.', FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('dd978406-796f-594b-a244-65cda9da06f4', '71bf86df-c759-576b-9c18-61cc05dcbcc8', 'yogurt', 'yogurt', 1.0, 0);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, instructions, optional)
VALUES ('1632d475-98e0-5079-8743-e043fec1e257', '65f683e8-19d8-5f79-a784-94563530f086', 'DINNER', 'Cena', 4, NULL, FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('d2846442-d4b4-5cdd-b155-f86ce6c27214', '1632d475-98e0-5079-8743-e043fec1e257', 'fish', NULL, 200.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('30552bf4-611c-549d-9515-58ab7c285129', '1632d475-98e0-5079-8743-e043fec1e257', 'potato', NULL, 300.0, 1);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('508716ae-173b-585c-86d4-0dc0dbfbbeb0', '1632d475-98e0-5079-8743-e043fec1e257', 'salad', 'salad', 1.0, 2);

-- Día 2 — Fuerza empuje
INSERT INTO nutrition_plan_day (id, nutrition_plan_id, week_number, day_number, day_type, target_kcal, target_protein_g, target_carbs_g, target_fat_g, notes)
VALUES ('1067a305-f70a-57a6-97f4-1dc6f225d44c', 'bbbbbbbb-0000-4000-8000-000000000001', 1, 2, 'STRENGTH', 2350, 170.0, 235.0, 78.0, 'Fuerza empuje');
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, instructions, optional)
VALUES ('4aa20446-7e06-5d15-a4e2-4e8de434f999', '1067a305-f70a-57a6-97f4-1dc6f225d44c', 'BREAKFAST', 'Desayuno', 0, NULL, FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('eb48a57d-1f8a-5bcc-98a0-5993de493ccf', '4aa20446-7e06-5d15-a4e2-4e8de434f999', 'eggs', 'eggs', 1.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('e93e6ede-da06-5539-afca-f59ddccf8717', '4aa20446-7e06-5d15-a4e2-4e8de434f999', 'egg-whites', NULL, 150.0, 1);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('8c199a3e-ba98-5d71-bf75-760f935a50e3', '4aa20446-7e06-5d15-a4e2-4e8de434f999', 'whole-wheat-bread', 'whole-wheat-bread', 1.0, 2);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, instructions, optional)
VALUES ('bb10e5bf-31e6-5cc0-826f-9946adea1516', '1067a305-f70a-57a6-97f4-1dc6f225d44c', 'MID_MORNING', 'Media mañana', 1, 'Con una pieza de fruta. El Excel no dice cuál.', FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('e20361a2-03de-5bec-8a8f-3c0c17ab8894', 'bb10e5bf-31e6-5cc0-826f-9946adea1516', 'almonds-walnuts', NULL, 25.0, 0);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, instructions, optional)
VALUES ('6d9da2db-5c37-54e4-b9e9-d58d0d752ad3', '1067a305-f70a-57a6-97f4-1dc6f225d44c', 'LUNCH', 'Comida', 2, NULL, FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('5b5e158c-e0e0-594d-b9a5-d3c951f53385', '6d9da2db-5c37-54e4-b9e9-d58d0d752ad3', 'turkey', NULL, 200.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('a9070c75-b6d5-5410-8506-02a658e1f514', '6d9da2db-5c37-54e4-b9e9-d58d0d752ad3', 'sweet-potato', NULL, 250.0, 1);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('68152ba2-9b9a-5b83-9844-4a71c6a5cd2b', '6d9da2db-5c37-54e4-b9e9-d58d0d752ad3', 'vegetables', 'vegetables', 1.0, 2);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, instructions, optional)
VALUES ('72152c38-c87c-5baf-951e-8f4c154c2aae', '1067a305-f70a-57a6-97f4-1dc6f225d44c', 'SNACK', 'Merienda', 3, NULL, FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('b9d6390d-9ff0-50b4-a808-a14393c8f895', '72152c38-c87c-5baf-951e-8f4c154c2aae', 'whey-protein', NULL, 30.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('5c87fcfa-1160-5d90-8ec0-d2f9d312473f', '72152c38-c87c-5baf-951e-8f4c154c2aae', 'skim-milk', 'skim-milk', 1.0, 1);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, instructions, optional)
VALUES ('922758f2-5836-5e39-9fb7-76ac16e8af28', '1067a305-f70a-57a6-97f4-1dc6f225d44c', 'DINNER', 'Cena', 4, NULL, FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('a4034bc4-ecbb-5f9b-bda3-060369308836', '922758f2-5836-5e39-9fb7-76ac16e8af28', 'salmon', NULL, 180.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('87952242-148d-5e8f-8c2f-fde739b7d31c', '922758f2-5836-5e39-9fb7-76ac16e8af28', 'salad', 'salad', 1.0, 1);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('a7cad5ae-3fc9-5f9b-b273-1207c30184b0', '922758f2-5836-5e39-9fb7-76ac16e8af28', 'potato', 'potato', 1.0, 2);

-- Día 3 — Running suave
INSERT INTO nutrition_plan_day (id, nutrition_plan_id, week_number, day_number, day_type, target_kcal, target_protein_g, target_carbs_g, target_fat_g, notes)
VALUES ('6e04e1e2-98a2-5a25-aad1-de028f2b8f21', 'bbbbbbbb-0000-4000-8000-000000000001', 1, 3, 'RUNNING', 2250, 165.0, 250.0, 62.0, 'Running suave');
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, instructions, optional)
VALUES ('520474b3-021e-5627-9cb7-b3c02cc3bc01', '6e04e1e2-98a2-5a25-aad1-de028f2b8f21', 'BREAKFAST', 'Desayuno', 0, NULL, FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('25479c4a-6935-5f11-b8f1-57090814b6dc', '520474b3-021e-5627-9cb7-b3c02cc3bc01', 'oats', NULL, 60.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('ffc2266a-889f-587c-8fd5-0c213ec33f64', '520474b3-021e-5627-9cb7-b3c02cc3bc01', 'yogurt', 'yogurt', 1.0, 1);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('872a362e-c50e-593a-a0c7-a5206681b3ca', '520474b3-021e-5627-9cb7-b3c02cc3bc01', 'berries', 'berries', 1.0, 2);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, instructions, optional)
VALUES ('a72e4531-b0ee-568f-a791-118679b87c04', '6e04e1e2-98a2-5a25-aad1-de028f2b8f21', 'MID_MORNING', 'Media mañana', 1, NULL, FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('0f065ca7-35c2-52b0-8dc5-f53585cab385', 'a72e4531-b0ee-568f-a791-118679b87c04', 'tuna', 'tuna', 1.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('fa2104d5-24e4-5d58-a7c9-a6893363e0c8', 'a72e4531-b0ee-568f-a791-118679b87c04', 'whole-wheat-bread', 'whole-wheat-bread', 1.0, 1);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, instructions, optional)
VALUES ('514ab643-383c-55c8-adaa-610dea64ce17', '6e04e1e2-98a2-5a25-aad1-de028f2b8f21', 'LUNCH', 'Comida', 2, NULL, FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('ccfb25f1-bb12-567e-b61e-86d2ca104146', '514ab643-383c-55c8-adaa-610dea64ce17', 'chicken', NULL, 200.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('9a49c4be-94d1-5bf2-9079-60178bba103d', '514ab643-383c-55c8-adaa-610dea64ce17', 'whole-wheat-pasta', NULL, 80.0, 1);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('ea741434-91a5-5a78-8ece-027d7abc0c00', '514ab643-383c-55c8-adaa-610dea64ce17', 'vegetables', 'vegetables', 1.0, 2);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, instructions, optional)
VALUES ('d9583e67-3933-5cf8-bb7b-799f2a6ce889', '6e04e1e2-98a2-5a25-aad1-de028f2b8f21', 'SNACK', 'Merienda', 3, 'Con una pieza de fruta. El Excel no dice cuál.', FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('b4f35fa3-67ac-5fc4-a93e-c8b436728148', 'd9583e67-3933-5cf8-bb7b-799f2a6ce889', 'fresh-cheese', NULL, 250.0, 0);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, instructions, optional)
VALUES ('6a94c66b-308b-54ce-b9be-a40debdac760', '6e04e1e2-98a2-5a25-aad1-de028f2b8f21', 'DINNER', 'Cena', 4, NULL, FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('53234d8f-250e-5b45-ad12-6fbcb49e36dd', '6a94c66b-308b-54ce-b9be-a40debdac760', 'eggs', 'eggs', 1.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('35f8e006-99de-577e-8bda-256293a660a8', '6a94c66b-308b-54ce-b9be-a40debdac760', 'egg-whites', 'egg-whites', 1.0, 1);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('2bb92f0e-ee1d-5e21-a999-5ce2fba9ae15', '6a94c66b-308b-54ce-b9be-a40debdac760', 'salad', 'salad', 1.0, 2);

-- Día 4 — Fuerza tirón
INSERT INTO nutrition_plan_day (id, nutrition_plan_id, week_number, day_number, day_type, target_kcal, target_protein_g, target_carbs_g, target_fat_g, notes)
VALUES ('5fc504bc-0180-5de9-aa0d-9397c4cebf2c', 'bbbbbbbb-0000-4000-8000-000000000001', 1, 4, 'STRENGTH', 2280, 160.0, 275.0, 60.0, 'Fuerza tirón');
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, instructions, optional)
VALUES ('c02e8525-73ef-508d-9306-a9b5b7bc8e70', '5fc504bc-0180-5de9-aa0d-9397c4cebf2c', 'BREAKFAST', 'Desayuno', 0, NULL, FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('6329169d-66f6-5837-965a-07e80711fd91', 'c02e8525-73ef-508d-9306-a9b5b7bc8e70', 'oats', NULL, 60.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('7686c2d0-9c02-5ad4-9431-550dc0b3bada', 'c02e8525-73ef-508d-9306-a9b5b7bc8e70', 'whey-protein', NULL, 30.0, 1);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('0f2f5d7f-c405-533c-b760-d55433201dd8', 'c02e8525-73ef-508d-9306-a9b5b7bc8e70', 'banana', 'banana', 1.0, 2);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, instructions, optional)
VALUES ('47938ea7-a30d-5261-8a15-27e0fcdb58ca', '5fc504bc-0180-5de9-aa0d-9397c4cebf2c', 'MID_MORNING', 'Media mañana', 1, NULL, FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('2fa51237-0e2d-55e8-a422-6394750a3f3c', '47938ea7-a30d-5261-8a15-27e0fcdb58ca', 'yogurt', 'yogurt', 1.0, 0);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, instructions, optional)
VALUES ('679d1ff0-07a0-565f-ab2d-978ed0b7f8b4', '5fc504bc-0180-5de9-aa0d-9397c4cebf2c', 'LUNCH', 'Comida', 2, NULL, FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('952c06c7-b371-562e-85ac-352d4df7e690', '679d1ff0-07a0-565f-ab2d-978ed0b7f8b4', 'rice', NULL, 80.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('e911bea5-a8a4-5e2d-9268-f0c11ed00c64', '679d1ff0-07a0-565f-ab2d-978ed0b7f8b4', 'tuna', 'tuna', 1.0, 1);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('97464c02-f6d9-5495-bd09-175559721d50', '679d1ff0-07a0-565f-ab2d-978ed0b7f8b4', 'salad', 'salad', 1.0, 2);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, instructions, optional)
VALUES ('67d951c8-9620-5955-a4a3-e64264f8c8b7', '5fc504bc-0180-5de9-aa0d-9397c4cebf2c', 'SNACK', 'Merienda', 3, 'Con una pieza de fruta. El Excel no dice cuál.', FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('19ccb97c-4df2-552c-bd74-30861570b2d0', '67d951c8-9620-5955-a4a3-e64264f8c8b7', 'almonds-walnuts', NULL, 25.0, 0);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, instructions, optional)
VALUES ('7849851a-56b9-522b-92b6-5ea746b47359', '5fc504bc-0180-5de9-aa0d-9397c4cebf2c', 'DINNER', 'Cena', 4, NULL, FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('7865825e-e9f9-5e03-856e-949bd53301a2', '7849851a-56b9-522b-92b6-5ea746b47359', 'fish', NULL, 200.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('1150a073-afa5-5c93-8226-fa0b80f3ad7d', '7849851a-56b9-522b-92b6-5ea746b47359', 'vegetables', 'vegetables', 1.0, 1);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('a17d59dc-b525-537f-91af-288f2b578c1c', '7849851a-56b9-522b-92b6-5ea746b47359', 'potato', 'potato', 1.0, 2);

-- Día 5 — Descanso/paseo
INSERT INTO nutrition_plan_day (id, nutrition_plan_id, week_number, day_number, day_type, target_kcal, target_protein_g, target_carbs_g, target_fat_g, notes)
VALUES ('577e6ab2-eafb-53be-b325-89c8a0a76da9', 'bbbbbbbb-0000-4000-8000-000000000001', 1, 5, 'REST', 2300, 170.0, 230.0, 75.0, 'Descanso/paseo');
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, instructions, optional)
VALUES ('7e1bb41c-0212-5f7b-bd7a-77f752989716', '577e6ab2-eafb-53be-b325-89c8a0a76da9', 'BREAKFAST', 'Desayuno', 0, 'Con una pieza de fruta. El Excel no dice cuál.', FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('93710a54-f3b8-5c6e-8272-27e82c432cbd', '7e1bb41c-0212-5f7b-bd7a-77f752989716', 'eggs', 'eggs', 1.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('ec528456-bce9-5b94-a61d-ac9fee1a3ad8', '7e1bb41c-0212-5f7b-bd7a-77f752989716', 'whole-wheat-bread', 'whole-wheat-bread', 1.0, 1);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, instructions, optional)
VALUES ('6439f912-b229-5d13-a011-3077b8ba0511', '577e6ab2-eafb-53be-b325-89c8a0a76da9', 'MID_MORNING', 'Media mañana', 1, NULL, FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('0c9156ff-a97f-541e-878f-e2b5267a7a55', '6439f912-b229-5d13-a011-3077b8ba0511', 'fresh-cheese', NULL, 250.0, 0);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, instructions, optional)
VALUES ('3b3ae6f0-bc74-5c62-8f4a-c397dde2d6db', '577e6ab2-eafb-53be-b325-89c8a0a76da9', 'LUNCH', 'Comida', 2, NULL, FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('e08a44df-115f-5116-bdd5-e82cbacb4c09', '3b3ae6f0-bc74-5c62-8f4a-c397dde2d6db', 'chicken', NULL, 200.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('aeebbfef-9959-5f52-ac8a-82c53563c828', '3b3ae6f0-bc74-5c62-8f4a-c397dde2d6db', 'sweet-potato', NULL, 250.0, 1);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('e940fea3-9456-553a-a6cf-bb11355ed14e', '3b3ae6f0-bc74-5c62-8f4a-c397dde2d6db', 'vegetables', 'vegetables', 1.0, 2);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, instructions, optional)
VALUES ('a5f9a3b0-8fb2-5381-b0ba-62e937dd4b21', '577e6ab2-eafb-53be-b325-89c8a0a76da9', 'SNACK', 'Merienda', 3, NULL, FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('1ca93ff8-8c01-56ea-a8e1-987752fab7f8', 'a5f9a3b0-8fb2-5381-b0ba-62e937dd4b21', 'whey-protein', NULL, 30.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('03e94182-3738-5ea1-80ee-d5611be4bfc2', 'a5f9a3b0-8fb2-5381-b0ba-62e937dd4b21', 'skim-milk', 'skim-milk', 1.0, 1);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, instructions, optional)
VALUES ('5ed6c7c0-6837-5831-b6e4-d106b7d2fae3', '577e6ab2-eafb-53be-b325-89c8a0a76da9', 'DINNER', 'Cena', 4, NULL, FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('90fd1f5b-72cf-5c30-88f8-a96155eb31a1', '5ed6c7c0-6837-5831-b6e4-d106b7d2fae3', 'salmon', NULL, 180.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('4a802606-c773-50c3-a40d-f13dccf33c72', '5ed6c7c0-6837-5831-b6e4-d106b7d2fae3', 'salad', 'salad', 1.0, 1);

-- Día 6 — Running largo
INSERT INTO nutrition_plan_day (id, nutrition_plan_id, week_number, day_number, day_type, target_kcal, target_protein_g, target_carbs_g, target_fat_g, notes)
VALUES ('484765a5-bfa9-500e-b3e2-f4d7b46ef208', 'bbbbbbbb-0000-4000-8000-000000000001', 1, 6, 'RUNNING', 2400, 165.0, 290.0, 65.0, 'Running largo');
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, instructions, optional)
VALUES ('53e7bfbb-cd88-59b1-b257-4ee08f4b505a', '484765a5-bfa9-500e-b3e2-f4d7b46ef208', 'BREAKFAST', 'Desayuno', 0, NULL, FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('eb9afdb0-ffb7-5689-aafd-98b034f5c2fe', '53e7bfbb-cd88-59b1-b257-4ee08f4b505a', 'oats', NULL, 60.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('123efa45-cdde-5748-99fd-fd959e450f3e', '53e7bfbb-cd88-59b1-b257-4ee08f4b505a', 'whey-protein', NULL, 30.0, 1);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('dfb17f95-b605-5e7f-b8cf-3c2fe6ed6dfd', '53e7bfbb-cd88-59b1-b257-4ee08f4b505a', 'banana', 'banana', 1.0, 2);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, instructions, optional)
VALUES ('277230e9-14d1-56fb-9e91-042122eddf33', '484765a5-bfa9-500e-b3e2-f4d7b46ef208', 'MID_MORNING', 'Media mañana', 1, NULL, FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('a108c0e2-ffb7-5fa8-b869-9daac9e4a544', '277230e9-14d1-56fb-9e91-042122eddf33', 'yogurt', 'yogurt', 1.0, 0);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, instructions, optional)
VALUES ('ae633aef-dc3b-51d8-ab80-8427c552ff9c', '484765a5-bfa9-500e-b3e2-f4d7b46ef208', 'LUNCH', 'Comida', 2, NULL, FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('c24ddb64-dcfd-5f46-b9f4-061d9c635980', 'ae633aef-dc3b-51d8-ab80-8427c552ff9c', 'turkey', NULL, 200.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('897988bb-8f03-54be-a4dc-09c8a9846199', 'ae633aef-dc3b-51d8-ab80-8427c552ff9c', 'rice', NULL, 80.0, 1);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('b9faa06b-4adf-5f66-a94e-917ef7bdc99e', 'ae633aef-dc3b-51d8-ab80-8427c552ff9c', 'vegetables', 'vegetables', 1.0, 2);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, instructions, optional)
VALUES ('dd99e6e4-a6db-50bc-98fd-82be1ef87b2b', '484765a5-bfa9-500e-b3e2-f4d7b46ef208', 'SNACK', 'Merienda', 3, 'Con una pieza de fruta. El Excel no dice cuál.', FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('ebc62d7f-007a-5cdc-8b77-1006eada6e89', 'dd99e6e4-a6db-50bc-98fd-82be1ef87b2b', 'fresh-cheese', NULL, 250.0, 0);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, instructions, optional)
VALUES ('f050655f-53b2-5cba-ae1a-3e032305b4d9', '484765a5-bfa9-500e-b3e2-f4d7b46ef208', 'DINNER', 'Cena', 4, NULL, FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('e7449b89-88cb-5461-b0c2-fe95dcddd358', 'f050655f-53b2-5cba-ae1a-3e032305b4d9', 'fish', NULL, 200.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('2ae32850-944d-5d9c-89be-6020888bc1ed', 'f050655f-53b2-5cba-ae1a-3e032305b4d9', 'potato', 'potato', 1.0, 1);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('9875b628-cafb-51bb-966b-b3206a128a77', 'f050655f-53b2-5cba-ae1a-3e032305b4d9', 'salad', 'salad', 1.0, 2);

-- Día 7 — Fuerza pierna/core
INSERT INTO nutrition_plan_day (id, nutrition_plan_id, week_number, day_number, day_type, target_kcal, target_protein_g, target_carbs_g, target_fat_g, notes)
VALUES ('ece36e18-bca1-5a63-9720-f5d563b52212', 'bbbbbbbb-0000-4000-8000-000000000001', 1, 7, 'STRENGTH', 2200, 150.0, 230.0, 70.0, 'Fuerza pierna/core');
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, instructions, optional)
VALUES ('e0c4ad72-26b8-5f2b-b7a2-9f0bdc30cbd6', 'ece36e18-bca1-5a63-9720-f5d563b52212', 'BREAKFAST', 'Desayuno', 0, NULL, FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('dc8249b4-d94a-5733-ae2c-bdc442ad1f0f', 'e0c4ad72-26b8-5f2b-b7a2-9f0bdc30cbd6', 'eggs', 'eggs', 1.0, 0);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('b5a275b4-57fc-58d5-937f-e15b7a018007', 'e0c4ad72-26b8-5f2b-b7a2-9f0bdc30cbd6', 'egg-whites', 'egg-whites', 1.0, 1);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('11cedca5-e495-556e-be81-76a85d61b767', 'e0c4ad72-26b8-5f2b-b7a2-9f0bdc30cbd6', 'whole-wheat-bread', 'whole-wheat-bread', 1.0, 2);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, instructions, optional)
VALUES ('8033b1ae-96df-50cc-820c-d6e7a383e95d', 'ece36e18-bca1-5a63-9720-f5d563b52212', 'MID_MORNING', 'Media mañana', 1, 'Con una pieza de fruta. El Excel no dice cuál.', FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('880191ba-995b-516b-aba0-ce08d35ec30d', '8033b1ae-96df-50cc-820c-d6e7a383e95d', 'almonds-walnuts', NULL, 25.0, 0);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, instructions, optional)
VALUES ('1ef9ecaa-e61c-57f6-bd7c-16ac33608cc5', 'ece36e18-bca1-5a63-9720-f5d563b52212', 'LUNCH', 'Comida', 2, 'Comida libre controlada: una proteína, un carbohidrato y una verdura.', FALSE);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, instructions, optional)
VALUES ('1902fec9-6de0-508d-b7ec-9c2e4acbeced', 'ece36e18-bca1-5a63-9720-f5d563b52212', 'SNACK', 'Merienda', 3, NULL, TRUE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('34d1247f-3900-5c84-92f5-835ddcfac38c', '1902fec9-6de0-508d-b7ec-9c2e4acbeced', 'whey-protein', NULL, 30.0, 0);
INSERT INTO nutrition_plan_meal (id, nutrition_plan_day_id, meal_type, name, sort_order, instructions, optional)
VALUES ('7ce437db-f3c7-596e-915f-fbe818030aa1', 'ece36e18-bca1-5a63-9720-f5d563b52212', 'DINNER', 'Cena', 4, 'Pescado o huevos, a elegir. El Excel ofrece las dos y no elige.', FALSE);
INSERT INTO nutrition_plan_meal_item (id, nutrition_plan_meal_id, food_id, serving_id, amount, sort_order)
VALUES ('da670da4-93f9-5f40-93ea-dff3bece188f', '7ce437db-f3c7-596e-915f-fbe818030aa1', 'vegetables', 'vegetables', 1.0, 0);
