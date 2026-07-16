package dev.diegobarrioh.forma.adapter.persistence;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES-256-GCM encryption for provider OAuth tokens at rest (FOR-131 spec Open Questions, resolved:
 * "app-level AES-256-GCM. Key from env WITHINGS_TOKEN_ENC_KEY — a base64-encoded 32-byte random
 * key, generated once by the operator via {@code openssl rand -base64 32}... Each token encrypted
 * with a fresh random 96-bit nonce stored alongside the ciphertext.").
 *
 * <p>Adapter-only (ADR-004, ADR-001): nothing above the persistence adapter ever sees ciphertext, a
 * nonce, or this class. {@link JdbcIntegrationTokenStore} is the only caller.
 *
 * <p>The 128-bit GCM authentication tag is not a separate column: the JCE {@code AES/GCM/NoPadding}
 * transformation appends it to the ciphertext bytes returned by {@link Cipher#doFinal}, and
 * verifies it as part of {@link Cipher#doFinal} on decrypt — this is the standard JCE contract, not
 * a shortcut. Losing the key makes stored tokens unrecoverable by design (spec FOR-131 Open
 * Questions: "acceptable for single-user MVP" — the user simply reconnects).
 */
public class AesGcmTokenCipher {

  private static final String TRANSFORMATION = "AES/GCM/NoPadding";
  private static final String KEY_ALGORITHM = "AES";
  private static final int KEY_LENGTH_BYTES = 32; // AES-256
  private static final int NONCE_LENGTH_BYTES = 12; // 96 bits, GCM's recommended nonce size
  private static final int GCM_TAG_LENGTH_BITS = 128;

  /** {@code null} means "not configured" (see class javadoc on why that does not fail eagerly). */
  private final SecretKeySpec key;

  private final SecureRandom secureRandom = new SecureRandom();

  /**
   * A missing/blank {@code base64Key} does NOT fail construction — local/dev/CI environments that
   * never exercise Withings OAuth must still be able to boot the application without {@code
   * WITHINGS_TOKEN_ENC_KEY} set (spec FOR-131: "empty/placeholder defaults"). The failure is
   * deferred to {@link #encrypt}/{@link #decrypt} instead, since that is the first point where a
   * missing key actually matters. A key that IS present but garbage (not valid base64, or not
   * exactly 32 bytes) fails immediately — that is always a configuration mistake, not an
   * unconfigured-yet environment.
   *
   * @param base64Key a base64-encoded 32-byte (256-bit) AES key, e.g. from {@code
   *     WITHINGS_TOKEN_ENC_KEY}; {@code null} or blank means "not configured"
   * @throws IllegalArgumentException if {@code base64Key} is present but not valid base64, or does
   *     not decode to exactly 32 bytes
   */
  public AesGcmTokenCipher(String base64Key) {
    if (base64Key == null || base64Key.isBlank()) {
      this.key = null;
      return;
    }
    byte[] decoded;
    try {
      decoded = Base64.getDecoder().decode(base64Key.trim());
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("Token encryption key is not valid base64", ex);
    }
    if (decoded.length != KEY_LENGTH_BYTES) {
      throw new IllegalArgumentException(
          "Token encryption key must decode to exactly "
              + KEY_LENGTH_BYTES
              + " bytes (AES-256), got "
              + decoded.length);
    }
    this.key = new SecretKeySpec(decoded, KEY_ALGORITHM);
  }

  /**
   * Encrypts {@code plaintext} with a fresh random 96-bit nonce.
   *
   * @throws IllegalStateException if no encryption key was configured
   */
  public EncryptedValue encrypt(String plaintext) {
    requireConfigured();
    byte[] nonce = new byte[NONCE_LENGTH_BYTES];
    secureRandom.nextBytes(nonce);
    try {
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce));
      byte[] ciphertext =
          cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return new EncryptedValue(ciphertext, nonce);
    } catch (GeneralSecurityException ex) {
      throw new IllegalStateException("Failed to encrypt token", ex);
    }
  }

  /**
   * Decrypts {@code ciphertext}/{@code nonce} produced by {@link #encrypt}.
   *
   * @throws IllegalStateException if no encryption key was configured, or if decryption/
   *     authentication-tag verification fails (wrong key, tampered ciphertext, or mismatched nonce)
   */
  public String decrypt(byte[] ciphertext, byte[] nonce) {
    requireConfigured();
    try {
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce));
      byte[] plaintext = cipher.doFinal(ciphertext);
      return new String(plaintext, java.nio.charset.StandardCharsets.UTF_8);
    } catch (GeneralSecurityException ex) {
      throw new IllegalStateException("Failed to decrypt token", ex);
    }
  }

  private void requireConfigured() {
    if (key == null) {
      throw new IllegalStateException(
          "Token encryption key is not configured (expected a base64-encoded 32-byte key via "
              + "WITHINGS_TOKEN_ENC_KEY). Generate one with: openssl rand -base64 32");
    }
  }

  /**
   * A ciphertext (with the GCM auth tag appended) alongside the nonce it was encrypted with. Both
   * are required to decrypt; neither is ever logged (ADR-008) — the ciphertext is unreadable
   * without the key, but is still not printed anywhere as a matter of hygiene.
   */
  public record EncryptedValue(byte[] ciphertext, byte[] nonce) {}
}
