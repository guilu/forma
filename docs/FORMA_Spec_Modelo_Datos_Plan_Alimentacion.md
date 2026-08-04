# FORMA — Modelo de datos para planes de alimentación

## Objetivo

Definir la estructura de datos necesaria para almacenar planes de alimentación generados por IA de forma normalizada, escalable y reutilizable.

La dieta no debe guardarse como una tabla semanal con columnas como `desayuno`, `comida` o `cena`. Ese formato sirve para visualización, pero no para persistencia.

La estructura recomendada es:

```text
Plan de alimentación
└── Semana
    └── Día
        └── Comida
            └── Alimentos y cantidades
```

Modelo general:

```text
nutrition_plan
    └── nutrition_plan_day
            └── nutrition_plan_meal
                    └── nutrition_plan_meal_item
                            └── food
```

---

# 1. Plan de alimentación

Representa el plan completo asignado a un usuario.

```sql
nutrition_plans
---------------
id
user_id
name
description

objective
status

start_date
end_date

target_kcal_min
target_kcal_max
target_protein_g
target_carbs_g
target_fat_g

generated_by
generation_prompt
generation_metadata_json

version
created_at
updated_at
```

Ejemplo:

```text
name: Dieta semanal — recomposición
objective: BODY_RECOMPOSITION
target_kcal_min: 2200
target_kcal_max: 2400
target_protein_g: 165
target_carbs_g: 250
target_fat_g: 65
generated_by: AI
version: 1
```

`generation_metadata_json` puede utilizarse para auditoría técnica de la generación:

```json
{
  "model": "gpt-5",
  "catalogVersion": "2026-08-03",
  "constraints": {
    "mealsPerDay": 5,
    "preferredStore": "Mercadona",
    "excludedFoods": [],
    "trainingPlanId": "..."
  }
}
```

Este JSON no debe contener la dieta completa. Solo metadatos de generación.

---

# 2. Días del plan

Cada fila representa un día del plan.

```sql
nutrition_plan_days
-------------------
id
nutrition_plan_id

week_number
day_number
day_of_week
calendar_date

target_kcal
target_protein_g
target_carbs_g
target_fat_g

calculated_kcal
calculated_protein_g
calculated_carbs_g
calculated_fat_g

training_type
notes

created_at
updated_at
```

Ejemplo para el lunes:

```text
week_number: 1
day_number: 1
day_of_week: MONDAY
target_kcal: 2320
target_protein_g: 165
target_carbs_g: 270
target_fat_g: 65
training_type: RUNNING
notes: Running 4–5 km
```

Aunque inicialmente el plan sea de una sola semana, debe mantenerse `week_number` para soportar planes de cuatro, ocho o doce semanas.

`calendar_date` puede ser nulo mientras el plan sea una plantilla. Al activarlo para un usuario, se asignan fechas reales.

---

# 3. Comidas del día

Cada desayuno, comida, merienda o cena es una fila.

```sql
nutrition_plan_meals
--------------------
id
nutrition_plan_day_id

meal_type
name
position
scheduled_time

target_kcal
target_protein_g
target_carbs_g
target_fat_g

instructions
optional

created_at
updated_at
```

Valores posibles de `meal_type`:

```text
BREAKFAST
MID_MORNING
LUNCH
SNACK
POST_WORKOUT
DINNER
```

No conviene almacenar un tipo combinado como `SNACK_POST_WORKOUT`.

Según el día debe guardarse:

- `SNACK`
- `POST_WORKOUT`
- o dos comidas distintas si ambas existen.

Ejemplo:

```text
meal_type: BREAKFAST
name: Desayuno
position: 1
scheduled_time: 08:00
```

---

# 4. Elementos de cada comida

Esta tabla enlaza cada comida con el catálogo maestro `foods`.

```sql
nutrition_plan_meal_items
-------------------------
id
nutrition_plan_meal_id

food_id
recipe_id nullable
serving_id nullable

quantity
unit
grams
weight_state

position
preparation_notes
optional

kcal_snapshot
protein_g_snapshot
carbs_g_snapshot
fat_g_snapshot

created_at
updated_at
```

Ejemplo del desayuno del lunes:

```text
Avena 60 g + whey 30 g + plátano
```

Se transforma en tres filas:

| food | quantity | unit | grams |
|---|---:|---|---:|
| Copos de avena | 60 | GRAM | 60 |
| Whey proteína | 30 | GRAM | 30 |
| Plátano | 1 | UNIT | 120 |

`food_id` apunta al alimento maestro, no al producto comercial de Mercadona.

La selección del producto concreto se realiza posteriormente al generar la lista de compra.

---

# 5. Snapshots nutricionales

Aunque las calorías y macros pueden calcularse desde `foods`, conviene guardar una copia en cada item:

```text
kcal_snapshot
protein_g_snapshot
carbs_g_snapshot
fat_g_snapshot
```

Esto evita que un plan histórico cambie si se modifican posteriormente los valores nutricionales del alimento maestro.

Ejemplo:

```text
food:
Copos de avena
kcal_100g: 370
```

Para una ración de 60 g:

```text
kcal_snapshot: 222
protein_g_snapshot: 7.8
carbs_g_snapshot: 36
fat_g_snapshot: 4.2
```

El catálogo conserva el dato actual y el plan conserva el dato con el que fue generado.

---

# 6. Tipos de comida configurables

Para el MVP puede utilizarse un enum.

Si se necesita que el administrador configure los tipos de comida, puede utilizarse una tabla maestra:

```sql
meal_types
----------
id
code
name
default_position
enabled
```

Ejemplos:

```text
BREAKFAST       Desayuno
MID_MORNING     Media mañana
LUNCH           Comida
SNACK           Merienda
POST_WORKOUT    Post-entreno
DINNER          Cena
```

---

# 7. Recetas

Algunas comidas serán combinaciones simples de alimentos:

```text
Pollo 200 g
Arroz 80 g
Verduras 300 g
AOVE 10 g
```

Otras serán recetas:

```text
Tortilla de patata
Poke de salmón
Avena overnight
```

Modelo propuesto:

```sql
recipes
-------
id
name
description
servings
instructions
enabled
```

```sql
recipe_items
------------
id
recipe_id
food_id
quantity
unit
grams
position
```

En `nutrition_plan_meal_items`, cada fila debe apuntar a un alimento o a una receta:

```text
food_id != null XOR recipe_id != null
```

No debe apuntar a ambos simultáneamente.

Para simplificar el MVP, también puede añadirse `recipe_id` directamente a `nutrition_plan_meals` y mantener los ingredientes como items.

---

# 8. Comidas flexibles

Ejemplo:

```text
Comida libre controlada: proteína + carbohidrato + verdura
```

Esto no es una comida cerrada, sino una regla de composición.

## Opción simple para el MVP

Guardar la regla como texto:

```text
instructions:
"Seleccionar una proteína, un carbohidrato y una verdura,
manteniendo los objetivos nutricionales de la comida."
```

## Opción estructurada futura

```sql
nutrition_meal_rules
--------------------
id
nutrition_plan_meal_id

food_group_id
min_items
max_items
target_grams
target_kcal
required
```

Ejemplo:

| Grupo | Mínimo | Máximo |
|---|---:|---:|
| Proteínas | 1 | 1 |
| Carbohidratos | 1 | 1 |
| Verduras | 1 | 2 |

Para el MVP se recomienda utilizar `instructions`.

---

# 9. Alternativas y sustituciones

Ejemplos:

```text
Merluza o salmón
Queso fresco batido o yogur proteína
```

No conviene guardar las opciones como texto concatenado.

Modelo estructurado:

```sql
nutrition_meal_item_groups
--------------------------
id
nutrition_plan_meal_id
name
selection_type
min_selections
max_selections
position
```

```sql
nutrition_meal_item_options
---------------------------
id
meal_item_group_id
food_id
serving_id
grams

kcal_snapshot
protein_g_snapshot
carbs_g_snapshot
fat_g_snapshot
```

Ejemplo:

```text
Grupo: Proteína de la cena
selection_type: ONE_OF
```

Opciones:

```text
Merluza 200 g
Salmón 150 g
Atún natural 120 g
```

Para una primera versión puede simplificarse con:

```sql
nutrition_plan_item_alternatives
--------------------------------
id
meal_item_id
alternative_food_id
grams
```

---

# 10. Seguimiento del usuario

El plan define lo que el usuario debería comer.

El seguimiento registra lo que realmente ha consumido.

No deben mezclarse.

```sql
nutrition_meal_logs
-------------------
id
user_id
nutrition_plan_meal_id
scheduled_date

status
completed_at
notes

created_at
updated_at
```

Estados posibles:

```text
PENDING
COMPLETED
PARTIALLY_COMPLETED
SKIPPED
REPLACED
```

Para registrar lo consumido:

```sql
nutrition_meal_log_items
------------------------
id
nutrition_meal_log_id

planned_meal_item_id nullable
food_id nullable
product_id nullable

quantity
unit
grams

actual_kcal
actual_protein_g
actual_carbs_g
actual_fat_g
```

Ejemplo:

```text
Planificado:
Merluza 200 g

Consumido:
Salmón 180 g
```

---

# 11. Objetivos frente a valores calculados

En cada día conviene distinguir:

```text
target_kcal
calculated_kcal
```

Y lo mismo para cada macro.

- `target_*`: objetivo solicitado a la IA.
- `calculated_*`: suma real de los alimentos generados.

Esto es necesario porque la IA puede indicar un total incorrecto.

Antes de guardar o activar un plan, el backend debe recalcular todos los valores y validar tolerancias.

Ejemplo de tolerancias:

```text
Calorías: ±5 %
Proteína: ±5 g
Carbohidratos: ±10 g
Grasas: ±5 g
```

---

# 12. Estado del peso del alimento

Para arroz, pasta, legumbres, carne o pescado es necesario especificar si el peso está expresado en crudo o cocinado.

Añadir:

```text
weight_state

RAW
COOKED
DRAINED
AS_SOLD
```

Campo:

```sql
nutrition_plan_meal_items.weight_state
```

Ejemplo:

```text
Arroz 80 g RAW
```

Sin este dato, una cantidad como `arroz 80 g` es ambigua.

---

# Modelo relacional recomendado

```text
users
  │
  └── nutrition_plans
          │
          └── nutrition_plan_days
                  │
                  └── nutrition_plan_meals
                          │
                          ├── nutrition_plan_meal_items
                          │       ├── foods
                          │       ├── food_servings
                          │       └── recipes
                          │
                          └── nutrition_meal_item_groups
                                  └── nutrition_meal_item_options
```

Seguimiento:

```text
nutrition_plan_meals
        │
        └── nutrition_meal_logs
                └── nutrition_meal_log_items
```

---

# Ejemplo completo: lunes

## Día

```json
{
  "dayOfWeek": "MONDAY",
  "weekNumber": 1,
  "targetKcal": 2320,
  "targetProteinG": 165,
  "targetCarbsG": 270,
  "targetFatG": 65,
  "trainingType": "RUNNING",
  "notes": "Running 4–5 km"
}
```

## Desayuno

```json
{
  "mealType": "BREAKFAST",
  "position": 1,
  "items": [
    {
      "foodCode": "ROLLED_OATS",
      "grams": 60
    },
    {
      "foodCode": "WHEY_PROTEIN",
      "grams": 30
    },
    {
      "foodCode": "BANANA",
      "serving": "MEDIUM_UNIT",
      "grams": 120
    }
  ]
}
```

## Comida

```json
{
  "mealType": "LUNCH",
  "position": 3,
  "items": [
    {
      "foodCode": "CHICKEN_BREAST",
      "grams": 200
    },
    {
      "foodCode": "RICE",
      "grams": 80,
      "weightState": "RAW"
    },
    {
      "foodCode": "MIXED_VEGETABLES",
      "grams": 300
    },
    {
      "foodCode": "EXTRA_VIRGIN_OLIVE_OIL",
      "grams": 10
    }
  ]
}
```

## Cena

```json
{
  "mealType": "DINNER",
  "position": 5,
  "items": [
    {
      "foodCode": "HAKE",
      "grams": 200
    },
    {
      "foodCode": "POTATO",
      "grams": 300
    },
    {
      "foodCode": "PREPARED_SALAD",
      "grams": 150
    }
  ]
}
```

---

# Tablas mínimas para el MVP

Para generar y almacenar el plan semanal bastan:

```text
nutrition_plans
nutrition_plan_days
nutrition_plan_meals
nutrition_plan_meal_items
```

Reutilizando:

```text
foods
food_servings
recipes
```

Posteriormente pueden añadirse:

```text
nutrition_meal_logs
nutrition_meal_log_items
nutrition_plan_item_alternatives
nutrition_meal_item_groups
nutrition_meal_item_options
nutrition_meal_rules
```

---

# Decisión recomendada

La estructura inicial debe ser:

```sql
nutrition_plans
nutrition_plan_days
nutrition_plan_meals
nutrition_plan_meal_items
```

Mapeo con la tabla semanal:

- Cada fila de la tabla es un `nutrition_plan_day`.
- Cada celda de desayuno, media mañana, comida, merienda o cena es un `nutrition_plan_meal`.
- Cada alimento y su cantidad es un `nutrition_plan_meal_item`.

La tabla semanal debe generarse como una proyección de estos datos.

No debe utilizarse como estructura de almacenamiento.
