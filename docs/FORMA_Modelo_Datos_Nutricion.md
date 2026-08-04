# FORMA -- Propuesta de rediseño del modelo de datos de Nutrición

## Objetivo

Separar completamente los dominios de **nutrición**, **compra** y
**productos comerciales** para disponer de un modelo escalable,
independiente de supermercados y preparado para la generación automática
de planes mediante IA.

------------------------------------------------------------------------

# Arquitectura propuesta

``` text
                ┌──────────────────────────┐
                │      FOOD MASTER         │
                │ "Pechuga de pollo"       │
                │ "Arroz integral"         │
                │ "Avena"                  │
                └────────────┬─────────────┘
                             │
                  valores nutricionales
                             │
              ┌──────────────┴──────────────┐
              │                             │
      PRODUCTOS TIENDA              PLAN NUTRICIÓN
      Mercadona                     comidas
      Carrefour                     recetas
      Lidl                          IA
```

Los tres dominios deben estar completamente desacoplados.

------------------------------------------------------------------------

# 1. Food Master (Catálogo nutricional)

Es la pieza central del sistema y el catálogo que utilizará la IA.

``` text
food
----

id
name
slug

food_group_id

kcal_100
protein_100
carbs_100
fat_100
fiber_100
sugar_100
salt_100

default_serving

enabled
```

Ejemplos:

-   Avena
-   Pechuga de pollo
-   Arroz integral
-   Atún natural
-   Plátano
-   Yogur proteína
-   Leche desnatada

No contiene marcas ni supermercados.

------------------------------------------------------------------------

# 2. Food Groups

Categorías nutricionales propias.

No utilizar las categorías de Mercadona ya que pueden cambiar.

``` text
Carbohidratos
Proteínas
Frutas
Verduras
Grasas
Lácteos
Legumbres
Bebidas
Condimentos
Suplementos
```

Ejemplos:

-   Pechuga → Proteínas
-   Avena → Carbohidratos
-   Aceite → Grasas

------------------------------------------------------------------------

# 3. Productos de tienda

Representa cada producto comercial.

``` text
product

id

store_id

food_id

store_category_id

name

price

weight

barcode

image

url

brand
```

Ejemplo:

Mercadona → Pechuga pollo Hacendado

Carrefour → Pechuga pollo Carrefour

Ambos apuntan al mismo Food.

------------------------------------------------------------------------

# 4. Categorías de tienda

Se copian exactamente las categorías de cada supermercado.

``` text
store_category

id

store

code

name
```

Ejemplo Mercadona:

-   Proteínas
-   Frutas y verduras
-   Grasas y aceites
-   Lácteos y huevos
-   Otros

Cada supermercado mantiene sus propias categorías.

------------------------------------------------------------------------

# 5. Tiendas

``` text
store

id

name

logo

website
```

Ejemplos:

-   Mercadona
-   Carrefour
-   Lidl
-   Alcampo
-   Amazon

------------------------------------------------------------------------

# 6. Macronutrientes

Se elimina la tabla actual de macros.

Los únicos macronutrientes reales son:

-   Proteínas
-   Carbohidratos
-   Grasas

Frutas, verduras y lácteos son grupos de alimentos.

Como los valores nutricionales ya están almacenados, no es necesaria una
tabla adicional.

Se propone añadir:

``` text
primary_macro

PROTEIN

CARBS

FAT
```

------------------------------------------------------------------------

# 7. Tags

Sistema flexible mediante etiquetas.

``` text
food_tag
tag
```

Ejemplos:

-   Vegano
-   Vegetariano
-   Sin gluten
-   Sin lactosa
-   Integral
-   Congelado
-   Fresco
-   Procesado
-   Alto en proteína
-   Bajo en grasa
-   Rico en fibra
-   Post entreno
-   Desayuno
-   Cena
-   Snack

------------------------------------------------------------------------

# Modelo ampliado

``` text
stores
------
id
name
logo
website

store_categories
----------------
id
store_id
parent_id
name
code

food_groups
-----------
id
name
emoji
color
code

foods
-----
id
name
slug
food_group_id
default_serving
density
description

food_nutrition
--------------
food_id
kcal
protein
carbs
fat
fiber
sugar
salt

food_tags
---------
food_id
tag_id

tags
----
id
name

products
---------
id
store_id
store_category_id
food_id
brand
name
ean
format
weight
price
image
url
available
last_sync
```

------------------------------------------------------------------------

# Food Equivalences

Tabla para sustituir alimentos automáticamente.

``` text
food_equivalences

food_id

equivalent_food_id

ratio
```

Ejemplos:

100 g arroz

=

110 g pasta

=

250 g patata

=

220 g boniato

También:

Pollo ↔ Pavo ↔ Atún ↔ Merluza ↔ Claras

------------------------------------------------------------------------

# Food Servings

Separar las raciones del alimento.

``` text
food_servings

id

food_id

name

grams

default
```

Ejemplo:

Plátano

-   Pequeño
-   Mediano
-   Grande

Aceite

-   5 g
-   10 g
-   15 g

------------------------------------------------------------------------

# Recetas

Catálogo independiente.

``` text
recipes

recipe_ingredients

meal_types
```

Ejemplo:

Avena overnight

↓

-   Copos de avena
-   Leche
-   Proteína
-   Frutos rojos

------------------------------------------------------------------------

# Arquitectura por capas

``` text
             FOOD DATABASE
──────────────────────────────────

Food
FoodGroup
Nutrition
Tags
Serving
Equivalences
Recipes

──────────────────────────────────

SHOPPING DATABASE

Store
StoreCategory
Brand
Product
Price
Availability

──────────────────────────────────

NUTRITION ENGINE

Meal Plan
Recipes
Daily Meals
Objectives
User Restrictions
Shopping List
AI Planner
```

Cada capa depende únicamente de la inferior.

------------------------------------------------------------------------

# Ventajas

-   El motor nutricional no depende de ninguna tienda.
-   Permite añadir supermercados sin modificar la lógica.
-   Los precios pueden actualizarse de forma independiente.
-   Un mismo plan puede convertirse en lista de compra para cualquier
    supermercado.
-   La IA trabaja únicamente sobre un catálogo limpio y estable.
-   El modelo es escalable para soportar múltiples usuarios y futuros
    catálogos.

## Conclusión

El motor de IA debe operar exclusivamente sobre el **Food Database**.
Una segunda capa resolverá qué productos comerciales utilizar según la
tienda seleccionada por el usuario. Esta separación proporciona un
diseño limpio, desacoplado y preparado para evolucionar desde un MVP
hasta una plataforma completa de nutrición y planificación inteligente.
