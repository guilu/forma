# FORMA — Contrato con el agente de planes

Este documento define los dos mensajes que FORMA y el agente de IA se intercambian: la **petición**
que sale de FORMA y el **plan** que vuelve. El agente no existe todavía y no lo construimos
nosotros, así que esto es lo que tendrá que cumplir quien lo haga.

La decisión de diseño que hay detrás está en
[ADR-015](adr/ADR-015-plan-request-and-ai-generation.md). El formato del plan en sí ya estaba
descrito en [FORMA_Formato_Plan_JSON.md](FORMA_Formato_Plan_JSON.md), y **no se redefine aquí**: es
el mismo, a propósito. Este documento añade lo único que le falta — quién pide, con qué datos, qué
envoltorio lo trae de vuelta y qué se comprueba antes de guardarlo.

---

## Las dos direcciones

```text
FORMA  ──  petición de plan  ──▶  agente
FORMA  ◀──  plan + qué no supo resolver  ──  agente
```

Una petición, una respuesta. El agente no llama a FORMA por su cuenta y no consulta ninguna API
nuestra: todo lo que necesita viaja en la petición. Eso no es una simplificación, es lo que evita
tener que darle una credencial de entrada a un sistema que hoy solo sabe autenticar a personas con
sesión (ADR-012).

---

## `contractVersion`, y por qué existe

Los dos mensajes empiezan igual:

```json
{ "contractVersion": "1" }
```

**FORMA no versiona ningún otro DTO, y es la primera vez que hace falta.** El resto de contratos de
esta aplicación tienen los dos extremos en este mismo repositorio y se despliegan juntos; para esos,
la versión en la ruta (`/api/v1`, ADR-005) basta y sobra. El agente es el primer consumidor que no
desplegamos nosotros. Los dos lados se van a mover por separado, y algún día van a discrepar: un
mensaje que no sabe decir qué contrato habla no falla ese día, que sería lo bueno — falla peor, se
malinterpreta.

Va en el cuerpo y no en la ruta porque lo que evoluciona es la **forma de dos mensajes**, no una API
entera.

`catalogVersion` es otra cosa y por eso es otro campo: dice qué alimentos había, no qué forma tenía
el mensaje. Añadir un alimento al catálogo no es cambiar el contrato, y mezclarlos haría que lo
pareciera.

---

# La petición (FORMA → agente)

## Los campos

| Campo | Tipo | Obligatorio | Unidad / vocabulario |
|---|---|---|---|
| `contractVersion` | string | sí | `"1"` |
| `planRequestId` | uuid | sí | la fila de `plan_request`; vuelve en la respuesta |
| `catalogVersion` | string | sí | fecha del catálogo que viaja abajo |
| `person.sex` | enum | sí | `MALE` · `FEMALE` · `OTHER` |
| `person.ageYears` | entero | sí | años |
| `person.weightKg` | decimal | sí | kg, una decimal |
| `person.heightCm` | decimal | sí | cm, una decimal |
| `person.activityLevel` | enum | sí | `SEDENTARY` · `LIGHT` · `MODERATE` · `ACTIVE` · `VERY_ACTIVE` |
| `goal.mainGoal` | enum | sí | `COMPOSICION` · `RENDIMIENTO` · `HABITO` — el objetivo de vida |
| `goal.planObjective` | enum | sí | `WEIGHT_LOSS` · `MUSCLE_GAIN` · `MAINTENANCE` · `HEALTHY_EATING` — el de este plan |
| `targets.planKcal` | entero | sí | kcal/día. **Ya calculado. No lo recalcules** |
| `targets.proteinG` | decimal | no | g/día |
| `targets.carbsG` | decimal | no | g/día |
| `targets.fatG` | decimal | no | g/día |
| `training.daysPerWeek` | entero | sí | 0–7 |
| `training.weekdays` | lista de enum | no | `MONDAY`…`SUNDAY`. Lista vacía = no lo dijo |
| `training.equipment` | lista de enum | no | `BODYWEIGHT` · `DUMBBELLS` · `BARBELL` · `BANDS` · `MACHINES` · `CARDIO_MACHINE` |
| `preferences.mealsPerDay` | entero | sí | 3–6 |
| `preferences.dietPattern` | enum | sí | `OMNIVORE` · `VEGETARIAN` · `VEGAN` · `GLUTEN_FREE` · `UNSPECIFIED` |
| `preferences.cuisineStyle` | enum | sí | `ESPANOLA` · `MEDITERRANEA` · `UNSPECIFIED` |
| `plan.weeks` | entero | sí | semanas de **este bloque**, no del programa entero. Hoy siempre `4` |
| `plan.startDate` | fecha | no | ISO, el inicio de este bloque. De ella salen las fechas de cada día |
| `catalog.foods[]` | lista | sí | el catálogo entero. Ver abajo |

**Dos objetivos y no uno, a propósito.** `mainGoal` es lo que esa persona persigue en general;
`planObjective` es lo que se le pide a este plan concreto, y es el único de los dos que lleva un
número detrás (el factor que multiplica el gasto diario). No son lo mismo y colapsarlos perdería
uno de los dos. El razonamiento largo está en la decisión 5 del ADR-015.

**`planObjective` viene siempre de una pregunta explícita, nunca de `mainGoal`.** La primera versión
de esta decisión (ADR-015, punto abierto 1) proponía deducirlo — quien elegía «composición»
heredaba `WEIGHT_LOSS` con un 20 % de déficit por defecto. El dueño de producto lo rechazó: elegir
«composición» puede significar perder grasa o ganar músculo igual de honestamente, y adivinar las
calorías de alguien es justo donde adivinar no es aceptable. El asistente ahora pregunta la
dirección explícitamente — perder grasa, ganar músculo o mantener — y esa respuesta, nunca
`mainGoal`, es la que decide `planObjective`. Una consecuencia concreta para quien lea este
contrato: **nunca vas a recibir `planObjective: "HEALTHY_EATING"` procedente de una petición**, esa
pregunta de tres respuestas no tiene una que signifique «solo comer mejor» — quien quiere eso pide
`MAINTENANCE`, que le da el mismo factor (1,0). `HEALTHY_EATING` sigue existiendo como valor válido
del vocabulario, pero fuera de este contrato: solo llega a través de `POST /api/v1/nutrition/plans`
cuando una persona crea un plan a mano.

**Dos ejes de dieta y no uno.** `dietPattern` es una regla de exclusión y `cuisineStyle` es una
cocina. Una dieta vegana mediterránea no es una contradicción, y con un solo campo no se puede
decir.

**`planKcal` viene calculado y es innegociable.** Sale de Mifflin-St Jeor por el factor de actividad
por el factor de objetivo (`EnergyRequirement`), se calcula una vez cuando se crea la petición y se
congela. Es la cifra que esa persona vio en pantalla; si mañana cambiamos la fórmula, su plan tiene
que poder compararse con el número que le convenció, no con el que daría hoy. Recalcúlalo por tu
cuenta y estarás construyendo un plan para otra cifra.

**`plan.weeks` es el bloque, no el programa.** FORMA genera un programa de doce semanas en tres
bloques de cuatro, con revisión cada dos semanas, y no genera las doce de golpe: cada bloque se pide,
se sigue y se ajusta a como le fue a la persona en el bloque anterior, antes de pedir el siguiente —
así es como funciona un seguimiento nutricional real. Un programa, para este contrato, **son tres
peticiones**, cada una con su propio `planRequestId`, su propio `plan.weeks` (siempre `4`) y su propio
`plan.startDate` (el inicio de ese bloque, no del programa). No vas a recibir una petición que pida
doce semanas de una vez, y si alguna vez la recibes es un fallo de quien la generó, no una petición
válida que este contrato contemple. (Decisión completa: ADR-015, decisión 14.)

## Aquí no hay datos de salud

Ni alergias, ni patologías, ni lesiones. No están, y no por olvido: son categoría especial del
artículo 9 del RGPD, y es la misma postura que tomó `V61__plan_lead.sql` cuando el embudo público
dejó de pedirlas.

La consecuencia práctica para quien escriba el agente es que **no vas a recibir restricciones
médicas y no debes pedirlas**. Si el plan necesita esa información para ser seguro, el plan no debe
generarse: eso es una conversación con un profesional, no un campo de un JSON.

## El catálogo viaja dentro

`catalog.foods[]` es exactamente lo que devuelve
`GET /api/v1/nutrition/plans/import/catalog` (`ImportCatalogResponse`), incrustado en la petición:

```json
{ "id": "oats", "name": "Copos de avena",
  "per100g": { "kcal": 370, "proteinG": 13.0, "carbsG": 60.0, "fatG": 7.0 },
  "preparation": null,
  "servings": [ { "id": "oats", "name": null, "grams": 60.0, "isDefault": true } ] }
```

Viaja dentro en vez de que lo pidas tú porque ese endpoint exige sesión, y darte una credencial para
que lo consultes sería abrir una puerta de entrada a FORMA para ahorrarte una llamada.

`preparation` dice si esos macros describen el alimento `CRUDO`, `COCINADO` o `TAL_CUAL`. Un `null`
significa que nadie lo ha decidido, que no es lo mismo que «da igual».

## Ejemplo completo

```json
{
  "contractVersion": "1",
  "planRequestId": "8f1c2b64-0e5a-4c7d-9a10-3f2b6d4e7c11",
  "catalogVersion": "2026-09-18",
  "person": {
    "sex": "MALE",
    "ageYears": 38,
    "weightKg": 73.6,
    "heightCm": 180.0,
    "activityLevel": "MODERATE"
  },
  "goal": { "mainGoal": "COMPOSICION", "planObjective": "WEIGHT_LOSS" },
  "targets": { "planKcal": 2078, "proteinG": 160.0, "carbsG": 260.0, "fatG": 70.0 },
  "training": {
    "daysPerWeek": 5,
    "weekdays": ["MONDAY", "TUESDAY", "THURSDAY", "FRIDAY", "SATURDAY"],
    "equipment": ["DUMBBELLS", "BARBELL", "CARDIO_MACHINE"]
  },
  "preferences": {
    "mealsPerDay": 5,
    "dietPattern": "OMNIVORE",
    "cuisineStyle": "ESPANOLA"
  },
  "plan": { "weeks": 4, "startDate": "2026-09-21" },
  "catalog": { "foods": [ "… el catálogo entero …" ] }
}
```

Los 2078 kcal de arriba salen de: `10·73,6 + 6,25·180 − 5·38 + 5 = 1676` basales, `× 1,55` de
actividad = `2598` diarias, `× 0,80` por objetivo = `2078`. Están puestos para que se pueda
comprobar la cuenta, no como cifra bonita.

---

# El plan (agente → FORMA)

## El envoltorio

```json
{
  "contractVersion": "1",
  "planRequestId": "8f1c2b64-0e5a-4c7d-9a10-3f2b6d4e7c11",
  "catalogVersion": "2026-09-18",
  "plan": { "… el plan, en el formato de siempre …" },
  "unmatched": [],
  "agent": {
    "model": "el-modelo-que-sea",
    "generatedAt": "2026-09-18T11:04:22Z"
  }
}
```

`plan` es, campo por campo, lo que ya describe
[FORMA_Formato_Plan_JSON.md](FORMA_Formato_Plan_JSON.md) y lo que ya acepta
`POST /api/v1/nutrition/plans` (`NutritionPlanRequest`). **No hay un segundo formato**, y eso es
deliberado: es la misma razón que da `PlanImportRequest` para reutilizarlo en las importaciones —
dos formatos para la misma información significan que cada campo que se añade a uno hay que
acordarse de añadirlo al otro.

Lo que sí es nuevo aquí es `unmatched`, y es el corazón de este contrato.

## Cómo se nombra un alimento

Esta es la parte difícil. FORMA identifica alimentos por `foodId`, recetas por `recipeId` y
raciones por `servingId`, y son identificadores nuestros que un agente externo no tiene manera de
adivinar. La respuesta del contrato es directa:

> **Un plan solo puede nombrar identificadores del catálogo que viajó en la petición.** No hay forma
> de crear un alimento desde aquí, y es a propósito: los macros de un alimento son un dato que
> alguien mide, no que un modelo estime.

Una línea nombra **un alimento o una receta, nunca las dos cosas ni ninguna**, y la cantidad se dice
de una de estas tres maneras:

```json
{ "foodId": "oats",   "amount": 60 }                          ← 60 gramos
{ "foodId": "banana", "servingId": "banana", "amount": 1 }    ← 1 ración de esa ración
{ "recipeId": "guiso-arroz", "amount": 1 }                    ← 1 ración del plato
```

Prefiere la ración cuando la haya. Una ración pertenece a su alimento: contar «rebanadas» de aceite
de oliva se rechaza.

### Cuando el alimento que quieres no está

Es el caso que hay que resolver bien, porque va a pasar todas las semanas. El catálogo es pequeño y
tu plan va a querer lentejas antes o después. Hay tres salidas y solo dos son aceptables:

1. **Inventarte un `foodId`.** Se rechaza. `NutritionPlanService#problemsIn` comprueba cada línea
   contra el catálogo y responde `No existe el alimento: lentejas`. El fichero entero no escribe
   nada — pero te devuelve **todos** los fallos de una vez, no el primero, precisamente porque quien
   escribe estos ficheros es un modelo y una respuesta con un solo fallo convierte cinco erratas en
   cinco intentos.
2. **Sustituirlo en silencio por lo más parecido que sí esté.** ❌ **Prohibido.** Un plan que dice
   arroz donde tú querías lentejas es un plan distinto del que diseñaste, y nadie se entera. Si
   sustituyes, dilo en `unmatched`.
3. **Decirlo, y seguir.** ✅ Es la salida buena, y el formato ya la tiene. Lo que no puedas concretar
   va en `instructions` de la comida, y la comida se queda con los alimentos que sí son seguros:

   ```json
   { "mealType": "LUNCH", "name": "Comida",
     "instructions": "Acompañar con una legumbre cocida (lentejas o garbanzos), unos 150 g.",
     "items": [ { "foodId": "vegetables", "servingId": "vegetables", "amount": 1 } ] }
   ```

   No es un apaño: es lo que hizo `V56__excel_diet_plan.sql` cinco veces con la palabra «Fruta»
   cuando la hoja original no decía cuál.

### `unmatched`: que el hueco no se pierda en la prosa

Una instrucción en castellano la lee una persona. Lo que hace falta además es una lista que pueda
leer la aplicación, para que el hueco del catálogo se convierta en trabajo pendiente en vez de
perderse:

```json
"unmatched": [
  {
    "wanted": "Lentejas cocidas",
    "where": "days[0].meals[2]",
    "resolution": "INSTRUCTION",
    "usedInstead": null,
    "note": "No hay ninguna legumbre en el catálogo."
  },
  {
    "wanted": "Yogur griego",
    "where": "days[3].meals[3]",
    "resolution": "SUBSTITUTED",
    "usedInstead": "yogurt",
    "note": "Sustituido por yogur proteína; los macros no son los mismos."
  }
]
```

| Campo | Qué es |
|---|---|
| `wanted` | lo que querías, en lenguaje natural |
| `where` | la ruta exacta dentro de `plan`, igual que la usan los errores |
| `resolution` | `INSTRUCTION` (lo dejaste dicho y no listado) · `SUBSTITUTED` (usaste otro) · `OMITTED` (lo quitaste) |
| `usedInstead` | el `foodId` que usaste, o `null` |
| `note` | por qué |

`unmatched` vacío es una respuesta normal y buena. Lo que no es aceptable es un `unmatched` vacío y
un plan que sustituyó por su cuenta.

## Lo que FORMA comprueba antes de guardar nada

Dos comprobaciones distintas, con dos respuestas distintas.

**1. Que el plan se pueda escribir (estructura).** Que cada `foodId` y cada `recipeId` existan, que
cada `servingId` sea de su alimento, que ninguna línea sea las dos cosas ni ninguna. Si algo de esto
falla, **no se escribe nada** y la petición queda en `FAILED` con la lista completa de problemas.

**2. Que las cifras que declaras sean las que suma tu propia comida (sección 11).** FORMA resuelve
cada línea a gramos y suma los macros contra el catálogo — no guarda totales, los calcula en cada
lectura — y compara esa suma con los `targets` que tú escribiste, día a día:

```text
Calorías:      ±5 %
Proteína:      ±5 g
Carbohidratos: ±10 g
Grasas:        ±5 g
```

**Si no cuadra, el plan NO se rechaza.** Entra como borrador, se anota la desviación, y no se activa
solo: una persona la ve y decide. La razón está medida y está en este repositorio — sigue leyendo.

**Y tus `targets` se guardan tal cual los mandaste, sin corregir.** El objetivo y la suma real son
dos cosas distintas en este modelo, y esa diferencia es justo lo que permite decir «pediste 2320 y
esto da otra cosa». Sobrescribir el objetivo con la suma borraría la única prueba de que el modelo
se equivocó.

## Dónde acaba cada cosa

| Lo que mandas | Dónde se guarda |
|---|---|
| `plan.name`, `description`, `objective`, `startDate`, `endDate` | `nutrition_plan` |
| `plan.targets.*` | `nutrition_plan.target_kcal_min/max`, `target_protein_g/carbs_g/fat_g` |
| `plan.days[]` | `nutrition_plan_day` (`week_number`, `day_number` 1 = lunes, `day_type`) |
| `plan.days[].meals[]` | `nutrition_plan_meal` |
| `plan.days[].meals[].items[]` | `nutrition_plan_meal_item` (`amount` cuenta gramos, raciones o platos) |
| `agent.*`, `contractVersion`, `catalogVersion`, `unmatched` | `nutrition_plan.generation_metadata` (JSON dentro de TEXT) |
| la petición entera que se te envió | `nutrition_plan.generation_prompt` y `plan_request.request_payload` |
| — | `nutrition_plan.generated_by = 'AI'`, y el plan entra siempre como `DRAFT` |

El plan entra como borrador **siempre**, diga lo que diga el cuerpo. Activarlo es otra llamada y la
hace la persona desde su pantalla: que la dieta de alguien cambie porque llegó una respuesta HTTP
sería una cosa muy rara que hiciera una integración.

## Ejemplo completo

Un lunes, con el envoltorio entero:

```json
{
  "contractVersion": "1",
  "planRequestId": "8f1c2b64-0e5a-4c7d-9a10-3f2b6d4e7c11",
  "catalogVersion": "2026-09-18",
  "plan": {
    "name": "Recomposición 2000-2150",
    "description": "Semana con cinco días de entrenamiento.",
    "objective": "COMPOSICION",
    "startDate": "2026-09-21",
    "targets": { "kcalMin": 2000, "kcalMax": 2150, "proteinG": 160, "carbsG": 240, "fatG": 65 },
    "generation": { "by": "AI" },
    "days": [{
      "weekNumber": 1, "dayNumber": 1, "dayType": "STRENGTH",
      "targets": { "calories": 2078, "proteinG": 160, "carbsG": 240, "fatG": 65 },
      "notes": "Fuerza por la tarde",
      "meals": [
        { "mealType": "BREAKFAST", "name": "Desayuno", "scheduledTime": "08:00:00", "items": [
          { "foodId": "oats", "amount": 60 },
          { "foodId": "whey-protein", "amount": 30 },
          { "foodId": "banana", "servingId": "banana", "amount": 1 } ] },
        { "mealType": "MID_MORNING", "name": "Media mañana", "items": [
          { "foodId": "fresh-cheese", "servingId": "fresh-cheese", "amount": 1 },
          { "foodId": "berries", "amount": 100 } ] },
        { "mealType": "LUNCH", "name": "Comida",
          "instructions": "Acompañar con una legumbre cocida (lentejas o garbanzos), unos 150 g.",
          "items": [
            { "foodId": "chicken", "amount": 200 },
            { "foodId": "rice", "amount": 80 },
            { "foodId": "vegetables", "servingId": "vegetables", "amount": 1 },
            { "foodId": "olive-oil", "servingId": "olive-oil", "amount": 1 } ] },
        { "mealType": "SNACK", "name": "Merienda", "items": [
          { "foodId": "yogurt", "servingId": "yogurt", "amount": 1 },
          { "foodId": "almonds-walnuts", "servingId": "almonds-walnuts", "amount": 1 } ] },
        { "mealType": "DINNER", "name": "Cena", "items": [
          { "foodId": "fish", "amount": 200 },
          { "foodId": "potato", "amount": 300 },
          { "foodId": "salad", "servingId": "salad", "amount": 1 } ] }
      ]
    }]
  },
  "unmatched": [
    { "wanted": "Lentejas cocidas", "where": "days[0].meals[2]",
      "resolution": "INSTRUCTION", "usedInstead": null,
      "note": "No hay ninguna legumbre en el catálogo." }
  ],
  "agent": { "model": "el-modelo-que-sea", "generatedAt": "2026-09-18T11:04:22Z" }
}
```

---

## Qué NO tiene este contrato

Nada de esto existe, y no por olvido:

| No existe | Por qué |
|---|---|
| alergias, patologías, lesiones | categoría especial del artículo 9; ver arriba |
| totales por día o por comida | se calculan; guardarlos los congela |
| macros en una línea de alimento | los tiene el alimento en el catálogo |
| día de la semana, fecha del día | se derivan de `dayNumber` y `startDate` |
| crear alimentos o recetas | sus datos se miden, no se estiman |
| plan de entrenamiento | fuera de este corte. La petición ya lleva días y equipamiento para cuando llegue |
| reintentos | un fallo es un fallo visible y una petición nueva, no un segundo intento silencioso |

---

## Errores

La misma forma que el resto de la API (`ApiError`, ver
[api-conventions.md](api-conventions.md)). Un plan con un solo fallo estructural **no escribe nada**
y devuelve **todos** sus problemas de una vez, cada uno con su ruta exacta:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "El plan no se puede guardar: 2 problema(s).",
  "details": [
    { "field": "plan.days[2].meals[1].items[0]", "message": "No existe el alimento: lentejas" },
    { "field": "plan.days[0].meals[0].items[1]", "message": "La ración banana no es de oats." }
  ]
}
```

---

## Una advertencia, medida

La dieta que `V56__excel_diet_plan.sql` metió en esta aplicación la generó un modelo, y sus cifras
por día **no son la suma de la comida que él mismo listó**. Cargada y sumada contra el catálogo, los
siete días se quedaron entre 379 y 702 kcal por debajo de lo que afirmaban. El patrón era nítido: la
proteína la clavaba y todo lo demás lo sobrestimaba.

Ese plan sigue siendo el que la aplicación trae sembrado, porque la **comida** estaba bien: lo único
que estaba mal era la suma, y la suma la rehace FORMA en cada lectura. Por eso la comprobación de la
sección 11 no rechaza un plan que no cuadra — lo marca. Y por eso existe: si generas un plan, da por
hecho que tu estimación de calorías es optimista, y cuenta con que aquí se va a comprobar.
