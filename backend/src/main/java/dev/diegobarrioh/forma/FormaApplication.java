package dev.diegobarrioh.forma;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application entry point for the FORMA backend.
 *
 * <p>This is bootstrap-only. It wires the Spring Boot runtime and does not
 * contain any product/domain behavior. Domain rules live under
 * {@code dev.diegobarrioh.forma.domain}, use cases under {@code application},
 * infrastructure under {@code adapter} and delivery under {@code delivery}.
 */
@SpringBootApplication
public class FormaApplication {

    public static void main(String[] args) {
        SpringApplication.run(FormaApplication.class, args);
    }
}
