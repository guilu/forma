package dev.diegobarrioh.forma.domain;

/**
 * The user's stored theme preference (FOR-107).
 *
 * <p>Mirrors the frontend's {@code ThemeMode} vocabulary ({@code frontend/src/theme/theme.ts},
 * FOR-62) so a later story (FOR-120, server-side theme) can map this value directly without a
 * translation layer. {@link #SYSTEM} means "follow the OS setting"; the frontend resolves it to a
 * concrete theme, the backend only stores the user's choice.
 */
public enum ThemeMode {
  LIGHT,
  DARK,
  SYSTEM
}
