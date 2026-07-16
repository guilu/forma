package dev.diegobarrioh.forma.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.SecureRandom;
import java.util.Base64;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AesGcmTokenCipher} (FOR-131 spec Open Questions, resolved: "app-level
 * AES-256-GCM... key from env WITHINGS_TOKEN_ENC_KEY — a base64-encoded 32-byte random key... Each
 * token encrypted with a fresh random 96-bit nonce"). This is the encryption-at-rest proof: {@code
 * tests.md} Adapter Tests — "Token store round-trip is encrypted at rest — the stored bytes are NOT
 * the plaintext token (assert)."
 */
class AesGcmTokenCipherTest {

  private static final String KEY_BASE64 = randomBase64Key();

  private final AesGcmTokenCipher cipher = new AesGcmTokenCipher(KEY_BASE64);

  @Test
  void encryptThenDecryptRoundTripsThePlaintext() {
    String plaintext = "a-very-real-withings-access-token";

    AesGcmTokenCipher.EncryptedValue encrypted = cipher.encrypt(plaintext);
    String decrypted = cipher.decrypt(encrypted.ciphertext(), encrypted.nonce());

    assertThat(decrypted).isEqualTo(plaintext);
  }

  @Test
  void theStoredCiphertextIsNeverThePlaintextBytes() {
    String plaintext = "another-very-real-withings-refresh-token";

    AesGcmTokenCipher.EncryptedValue encrypted = cipher.encrypt(plaintext);

    assertThat(encrypted.ciphertext()).isNotEqualTo(plaintext.getBytes());
    assertThat(new String(encrypted.ciphertext())).doesNotContain(plaintext);
  }

  @Test
  void eachEncryptionUsesAFreshNonce() {
    String plaintext = "same-plaintext-both-times";

    AesGcmTokenCipher.EncryptedValue first = cipher.encrypt(plaintext);
    AesGcmTokenCipher.EncryptedValue second = cipher.encrypt(plaintext);

    assertThat(first.nonce()).isNotEqualTo(second.nonce());
    // GCM with a fresh nonce also yields different ciphertext bytes for identical plaintext.
    assertThat(first.ciphertext()).isNotEqualTo(second.ciphertext());
  }

  @Test
  void nonceIs96Bits() {
    AesGcmTokenCipher.EncryptedValue encrypted = cipher.encrypt("x");

    assertThat(encrypted.nonce()).hasSize(12);
  }

  @Test
  void decryptingWithATamperedCiphertextFailsInsteadOfReturningGarbage() {
    // GCM's authentication tag makes tampering detectable — this is the "auth tag" half of
    // "ciphertext + nonce (and auth tag)" (spec FOR-131 Data Model Notes): the JCE GCM cipher
    // appends the 128-bit tag to the ciphertext bytes automatically, and decryption fails loudly
    // if either the ciphertext or the tag was altered, rather than silently returning corrupted
    // plaintext.
    AesGcmTokenCipher.EncryptedValue encrypted = cipher.encrypt("tamper-me");
    byte[] tampered = encrypted.ciphertext().clone();
    tampered[0] ^= 0x01;

    assertThatThrownBy(() -> cipher.decrypt(tampered, encrypted.nonce()))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  void rejectsAKeyThatIsNotExactly32BytesAfterBase64Decoding() {
    String tooShortKey = Base64.getEncoder().encodeToString(new byte[16]);

    assertThatThrownBy(() -> new AesGcmTokenCipher(tooShortKey))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void aBlankKeyDoesNotFailConstructionButFailsLazilyOnFirstUse() {
    // Local/dev environments may not have WITHINGS_TOKEN_ENC_KEY configured yet (spec FOR-131:
    // "empty/placeholder defaults" — the whole application context must still boot without it).
    // The failure is deferred to the moment encryption is actually attempted, not to bean
    // construction, so nothing blocks the app from starting just because nobody has connected
    // Withings yet.
    AesGcmTokenCipher unconfigured = new AesGcmTokenCipher(" ");

    assertThatThrownBy(() -> unconfigured.encrypt("anything"))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void anAbsentKeyAlsoFailsLazilyOnFirstUse() {
    AesGcmTokenCipher unconfigured = new AesGcmTokenCipher(null);

    assertThatThrownBy(() -> unconfigured.encrypt("anything"))
        .isInstanceOf(IllegalStateException.class);
  }

  private static String randomBase64Key() {
    byte[] key = new byte[32];
    new SecureRandom().nextBytes(key);
    return Base64.getEncoder().encodeToString(key);
  }
}
