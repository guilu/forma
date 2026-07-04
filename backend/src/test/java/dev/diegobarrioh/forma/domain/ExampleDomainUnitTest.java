package dev.diegobarrioh.forma.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Fast, framework-free unit-test example for the domain layer (FOR-86).
 *
 * <p>This is the template future domain tests should follow: plain JUnit 5 + AssertJ, no Spring
 * context (so it runs in milliseconds), and an Arrange-Act-Assert structure. It lives under the
 * {@code domain} test package to signal where business-rule tests belong. Replace this placeholder
 * with real domain tests as domain behavior is introduced (ADR-007: domain rules require domain
 * tests at the lowest practical level).
 *
 * <p>Naming: test classes end in {@code Test}; methods describe the behavior under test in words.
 * Keep domain tests free of Spring, PostgreSQL and provider SDKs.
 */
class ExampleDomainUnitTest {

  @Test
  @DisplayName("arrange-act-assert on pure logic, no Spring context")
  void computesResultFromInputsWithoutFrameworkDependencies() {
    // Arrange: a pure calculation stands in for future domain logic.
    int weeklySessions = 3;
    int weeks = 4;

    // Act
    int totalSessions = weeklySessions * weeks;

    // Assert
    assertThat(totalSessions).isEqualTo(12);
  }
}
