package dev.diegobarrioh.forma.config;

import dev.diegobarrioh.forma.adapter.persistence.AesGcmTokenCipher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the AES-256-GCM cipher used to encrypt provider OAuth tokens at rest (FOR-131). The key
 * comes from {@code forma.integrations.withings.token-encryption-key}, which resolves to the {@code
 * WITHINGS_TOKEN_ENC_KEY} environment variable in real environments (see {@code application.yml}) —
 * never a literal value here or anywhere else in this codebase (AGENTS.md: "Do not commit
 * secrets").
 */
@Configuration
public class CryptoConfig {

  @Bean
  public AesGcmTokenCipher withingsTokenCipher(
      @Value("${forma.integrations.withings.token-encryption-key:}") String tokenEncryptionKey) {
    return new AesGcmTokenCipher(tokenEncryptionKey);
  }
}
