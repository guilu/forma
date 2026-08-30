/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

/**
 * Commit corto del build, inyectado por `vite.config.ts`.
 *
 * Cadena vacía cuando el build no pudo averiguarlo — dentro del contenedor
 * `.git/` está en el `.dockerignore` y la imagen de node no trae git, así que
 * ahí llega por `VITE_BUILD_SHA` o no llega.
 */
declare const __BUILD_SHA__: string;
