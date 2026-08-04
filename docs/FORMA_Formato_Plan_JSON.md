# FORMA — Formato JSON de planes de alimentación

Este documento describe el fichero que la aplicación importa y que un modelo de lenguaje puede
generar. Está pensado para pegarse entero en un prompt.

## Antes de generar nada: pide el catálogo

```
GET /api/v1/nutrition/plans/import/catalog
```

Devuelve todos los alimentos con su identificador, sus macros por 100 g y sus raciones con nombre:

```json
{ "foods": [
  { "id": "oats", "name": "Copos de avena",
    "per100g": { "kcal": 370, "proteinG": 13.0, "carbsG": 60.0, "fatG": 7.0 },
    "preparation": null,
    "servings": [ { "id": "oats", "name": null, "grams": 60.0, "isDefault": true } ] }
] }
```

**Un plan solo puede nombrar identificadores de esa lista.** Cualquier otro se rechaza. No hay
manera de crear un alimento desde el fichero, y es a propósito: los macros de un alimento son un
dato que alguien mide, no que un modelo estime.

`preparation` dice si esos macros describen el alimento `CRUDO`, `COCINADO` o `TAL_CUAL`. Un `null`
significa que nadie lo ha decidido, que no es lo mismo que «da igual».

## El fichero

```json
{
  "plans": [
    {
      "forUserEmail": "diego@ejemplo.com",
      "plan": {
        "name": "Recomposición 2200-2400",
        "description": "Semana de recomposición con tres carreras.",
        "objective": "COMPOSICION",
        "startDate": "2026-09-01",
        "targets": { "kcalMin": 2200, "kcalMax": 2400, "proteinG": 165 },
        "generation": {
          "by": "AI",
          "prompt": "Genera una semana de recomposición para 73 kg…",
          "metadata": "{\"model\":\"claude-opus-5\"}"
        },
        "days": [ … ]
      }
    }
  ]
}
```

Un fichero puede llevar planes para **varias cuentas**. Cada una se nombra por su correo, que es lo
que quien escribe el fichero ya sabe. Una cuenta que no existe se reporta como error: importar un
plan no crea usuarios.

Lo importa un administrador (`POST /api/v1/nutrition/plans/import`), porque escribir en la cuenta de
otra persona es lo que hace un administrador. **Todos los planes entran como borrador**; cada cuenta
activa el suyo desde su pantalla.

## Los días

```json
{
  "weekNumber": 1,
  "dayNumber": 1,
  "dayType": "RUNNING",
  "targets": { "calories": 2320, "proteinG": 165, "carbsG": 270, "fatG": 65 },
  "notes": "Running 4-5 km",
  "meals": [ … ]
}
```

| Campo | Qué es |
|---|---|
| `weekNumber` | desde 1. Un plan de cuatro semanas son cuatro bloques de siete días |
| `dayNumber` | **1 = lunes**, 7 = domingo |
| `dayType` | `RUNNING`, `STRENGTH` o `REST`. Opcional |
| `targets` | lo que ese día **se pide**. No es lo que suma |

**No escribas totales.** No existe ningún campo para ellos. Lo que el día suma lo calcula la
aplicación con los alimentos del catálogo, en cada lectura, y lo compara con `targets`. Si tu
estimación y la suma no cuadran, el plan lo dirá — que es justamente para lo que sirve.

No hay campo para el día de la semana ni para la fecha: se derivan de `dayNumber` y de `startDate`.

## Las comidas

```json
{
  "mealType": "BREAKFAST",
  "name": "Desayuno",
  "scheduledTime": "08:00:00",
  "optional": false,
  "instructions": "Con una pieza de fruta",
  "items": [ … ]
}
```

`mealType`: `BREAKFAST`, `MID_MORNING`, `LUNCH`, `SNACK`, `PRE_WORKOUT`, `POST_WORKOUT`, `DINNER`.

No combines dos en uno: si un día tiene merienda **y** post-entreno, son dos comidas.

`instructions` es para lo que la lista de alimentos no puede decir. **Úsalo en vez de inventar**:

- una comida que es una regla y no una lista — «una proteína, un carbohidrato y una verdura» — va
  con `items` vacío y la regla en `instructions`;
- algo que no sabes concretar — «+ fruta», sin decir cuál — va como instrucción, no eligiendo tú
  una fruta;
- una elección sin resolver — «pescado o huevos» — va como instrucción, con los alimentos que sí
  son seguros en `items`.

`optional: true` para una comida que se puede saltar.

## Los alimentos de cada comida

Una línea nombra **un alimento o una receta, nunca las dos cosas ni ninguna**. La cantidad se dice de
una de estas tres maneras, y solo una:

```json
{ "foodId": "oats",   "amount": 60 }                          ← 60 gramos
{ "foodId": "banana", "servingId": "banana", "amount": 1 }    ← 1 ración de esa ración
{ "recipeId": "guiso-arroz", "amount": 1 }                    ← 1 ración del plato
```

**Prefiere la ración cuando la haya.** «Un plátano» dicho como ración sigue siendo un plátano si
mañana alguien corrige lo que pesa; dicho como 120 g deja de serlo. Escribe gramos solo cuando la
cantidad sea distinta de la ración.

Una ración pertenece a su alimento: contar «rebanadas» de aceite de oliva se rechaza.

Campos opcionales: `preparationNotes` («a la plancha»), `optional`.

## Qué NO tiene el formato

Nada de esto existe, y no por olvido:

| No existe | Por qué |
|---|---|
| totales por día o por comida | se calculan; guardarlos los congela |
| macros en una línea de alimento | los tiene el alimento en el catálogo |
| día de la semana, fecha del día | se derivan |
| estado de una comida (hecha, pendiente) | se deriva del registro de comidas |
| crear alimentos o recetas | sus datos se miden, no se estiman |

## Errores

Un fichero con un solo fallo **no escribe nada** y devuelve **todos** sus problemas de una vez, cada
uno con la ruta exacta:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "El fichero no se puede importar: 3 problema(s).",
  "details": [
    { "field": "plans[0].days[2].meals[1].items[0]", "message": "No existe el alimento: quinoa" },
    { "field": "plans[0].days[0].meals[0].items[1]", "message": "La ración banana no es de oats." },
    { "field": "plans[1].forUserEmail", "message": "No existe ninguna cuenta con el correo x@y.z." }
  ]
}
```

Todos a la vez y no el primero, precisamente porque quien escribe estos ficheros es un modelo: una
respuesta con un solo fallo convierte un fichero con cinco erratas en cinco intentos.

## Ejemplo completo

Un lunes de la dieta que V56 importó desde el Excel:

```json
{
  "plans": [{
    "forUserEmail": "diego@ejemplo.com",
    "plan": {
      "name": "Dieta semanal — recomposición",
      "objective": "COMPOSICION",
      "targets": { "kcalMin": 2200, "kcalMax": 2400 },
      "generation": { "by": "AI" },
      "days": [{
        "weekNumber": 1, "dayNumber": 1, "dayType": "RUNNING",
        "targets": { "calories": 2320, "proteinG": 165, "carbsG": 270, "fatG": 65 },
        "notes": "Running 4-5 km",
        "meals": [
          { "mealType": "BREAKFAST", "name": "Desayuno", "items": [
            { "foodId": "oats", "amount": 60 },
            { "foodId": "whey-protein", "amount": 30 },
            { "foodId": "banana", "servingId": "banana", "amount": 1 } ] },
          { "mealType": "MID_MORNING", "name": "Media mañana", "items": [
            { "foodId": "fresh-cheese", "amount": 250 } ] },
          { "mealType": "LUNCH", "name": "Comida", "items": [
            { "foodId": "chicken", "amount": 200 },
            { "foodId": "rice", "amount": 80 },
            { "foodId": "vegetables", "servingId": "vegetables", "amount": 1 },
            { "foodId": "olive-oil", "servingId": "olive-oil", "amount": 1 } ] },
          { "mealType": "SNACK", "name": "Merienda",
            "instructions": "Con una pieza de fruta. No se dice cuál.",
            "items": [ { "foodId": "yogurt", "servingId": "yogurt", "amount": 1 } ] },
          { "mealType": "DINNER", "name": "Cena", "items": [
            { "foodId": "fish", "amount": 200 },
            { "foodId": "potato", "amount": 300 },
            { "foodId": "salad", "servingId": "salad", "amount": 1 } ] }
        ]
      }]
    }
  }]
}
```

## Una advertencia, medida

La dieta de arriba la generó un modelo, y sus cifras por día **no son la suma de la comida que él
mismo listó**. Cargada y sumada contra el catálogo, los siete días se quedan entre 379 y 702 kcal
por debajo de lo que afirmaban. El patrón era nítido: la proteína la clavaba y todo lo demás lo
sobrestimaba.

Por eso `targets` y los totales calculados son cosas distintas en este modelo, y por eso el formato
no te deja escribir un total. Si generas un plan, da por hecho que tu estimación de calorías es
optimista y comprueba lo que la aplicación calcula.
