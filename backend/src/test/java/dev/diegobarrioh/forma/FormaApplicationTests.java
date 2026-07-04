package dev.diegobarrioh.forma;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies the Spring application context starts. This is the skeleton's
 * startup smoke test (FOR-80) and guards against broken wiring before any
 * product code exists. Uses the {@code test} profile so the context boots
 * against in-memory H2 instead of a real PostgreSQL (FOR-83).
 */
@SpringBootTest
@ActiveProfiles("test")
class FormaApplicationTests {

    @Test
    void contextLoads() {
    }
}
