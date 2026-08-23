package dev.diegobarrioh.forma.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on Spring's scheduler.
 *
 * <p>Its own class rather than an annotation on {@code FormaApplication}, so that what enables
 * background work in this application is a file somebody can find by name. Today the only scheduled
 * work is {@code PlanLeadRetentionJob}, which is what makes the privacy notice's twelve-month
 * retention period true.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {}
