# Forma — Muscle SVG overlays

Generado a partir de las cuatro siluetas suministradas.

> **Esto es el material de origen, no lo que sirve la app.**
>
> Los assets en uso viven en `frontend/src/assets/anatomy/`, con dos
> diferencias: las siluetas están en WebP (6,1 MB → 700 KB, mismas
> dimensiones en píxeles, que deben ser exactas para que las máscaras encajen)
> y el fichero se llama `silhouette.png` → `silhouette.webp` en todas las
> carpetas, corrigiendo el `shilouette.png` de `front/` y `back/`.
>
> Editar algo aquí no cambia la app. `index.html` se conserva porque documenta
> la técnica —los SVG como `mask-image` sobre un bloque de color, que es lo que
> permite teñir el músculo con el token de acento— y sigue abriéndose tal cual
> desde este directorio.

## Estructura

- `male/front/_master.svg`
- `male/back/_master.svg`
- `female/front/_master.svg`
- `female/back/_master.svg`
- Además, cada carpeta contiene un SVG independiente por grupo muscular.
- `muscle-map.json` contiene dimensiones y códigos disponibles.

## Recomendación

Para cambiar el color desde CSS, usa el SVG maestro **inline** (no como `<img>`).
Cada grupo tiene `data-muscle="..."`.

```css
.body-map {
  position: relative;
  aspect-ratio: 854 / 1842;
}

.body-map__silhouette,
.body-map__muscles {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.body-map__muscles [data-muscle] {
  fill: transparent;
}

.body-map__muscles [data-muscle].is-primary {
  fill: var(--color-accent);
  opacity: .92;
}

.body-map__muscles [data-muscle].is-secondary {
  fill: var(--color-accent);
  opacity: .42;
}
```

Los SVG individuales tienen relleno blanco y están pensados también para poder
usarse como `mask-image` si se prefiere mantenerlos como recursos externos.

## Códigos

FRONT:
- DELTOID_FRONT
- PECTORAL
- BICEPS
- FOREARM_FRONT
- ABS
- OBLIQUES
- ADDUCTORS
- QUADRICEPS
- LOWER_LEG_FRONT

BACK:
- DELTOID_REAR
- TRAPEZIUS
- UPPER_BACK
- LATS
- TRICEPS
- FOREARM_BACK
- LOWER_BACK
- GLUTES
- HAMSTRINGS
- CALVES
- SOLEUS

## Modelo de datos recomendado

`exercise_muscle(exercise_id, muscle_code, role, activation)`

`role`: `PRIMARY | SECONDARY`
`activation`: decimal entre `0.0` y `1.0`.

Los músculos del entrenamiento se derivan de sus ejercicios; no hace falta
duplicarlos en `training_muscle`.
