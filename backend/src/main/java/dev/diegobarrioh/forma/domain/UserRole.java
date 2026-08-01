package dev.diegobarrioh.forma.domain;

/**
 * What an account is allowed to do (FOR-190).
 *
 * <p>Two values, not a permission system: {@link #USER} is everyone, {@link #ADMIN} additionally
 * maintains the shared reference catalogs (foods, and later store products). An admin is still a
 * user — the admin screens are an addition to the app, not a separate one.
 *
 * <p>Granted deliberately, by an {@code UPDATE} on the account, and never inferred: no "first
 * registration becomes admin" rule, which on a fresh deployment would turn the first sign-up into a
 * privilege escalation.
 */
public enum UserRole {
  USER,
  ADMIN
}
