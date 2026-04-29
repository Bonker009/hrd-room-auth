package org.kshrd.hrdroomservice.persistence.audit;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable snapshot of one row to insert into {@code audit_logs}. Used to defer JDBC until after
 * Hibernate finishes flushing, avoiding {@link java.util.ConcurrentModificationException} on the
 * action queue when recording from JPA lifecycle callbacks.
 */
public record AuditLogRow(
        UUID auditLogId,
        LocalDateTime occurredAt,
        String action,
        String entityType,
        String entityId,
        UUID actorId,
        String requestId,
        String requestPath,
        String summary,
        String detailsJson) {}
