package org.kshrd.hrdroomservice.persistence.audit;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.kshrd.hrdroomservice.security.SecurityUtils;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogRecorder {

    private static final Object AUDIT_LOG_BUFFER_KEY = new Object();

    private static final String INSERT_SQL =
            """
            INSERT INTO audit_logs (audit_log_id, occurred_at, action, entity_type, entity_id, actor_id, request_id, request_path, summary, details_json)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;

    public void record(String action, Object entity, Object identifier) {
        try {
            AuditLogRow row = buildRow(action, entity, identifier);
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                enqueue(row);
            } else {
                insertRows(List.of(row));
            }
        } catch (Exception e) {
            log.warn("Failed to record audit log for action={}", action, e);
        }
    }

    private AuditLogRow buildRow(String action, Object entity, Object identifier) {
        Object unproxied = Hibernate.unproxy(entity);
        String requestId = MDC.get("requestId");
        String path = MDC.get("path");
        return new AuditLogRow(
                UUID.randomUUID(),
                LocalDateTime.now(),
                action,
                unproxied.getClass().getSimpleName(),
                String.valueOf(identifier),
                SecurityUtils.currentUserId().orElse(null),
                requestId != null && !requestId.isBlank() ? requestId : null,
                path != null && !path.isBlank() ? path : null,
                unproxied.getClass().getSimpleName() + " " + identifier,
                null);
    }

    private void enqueue(AuditLogRow row) {
        @SuppressWarnings("unchecked")
        List<AuditLogRow> buffer =
                (List<AuditLogRow>)
                        TransactionSynchronizationManager.getResource(AUDIT_LOG_BUFFER_KEY);
        if (buffer == null) {
            List<AuditLogRow> newBuffer = new ArrayList<>();
            TransactionSynchronizationManager.bindResource(AUDIT_LOG_BUFFER_KEY, newBuffer);
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            try {
                                @SuppressWarnings("unchecked")
                                List<AuditLogRow> toFlush =
                                        (List<AuditLogRow>)
                                                TransactionSynchronizationManager.getResource(
                                                        AUDIT_LOG_BUFFER_KEY);
                                if (toFlush != null && !toFlush.isEmpty()) {
                                    insertRows(toFlush);
                                }
                            } catch (Exception e) {
                                log.warn("Failed to flush audit log entries after commit", e);
                            } finally {
                                if (TransactionSynchronizationManager.hasResource(
                                        AUDIT_LOG_BUFFER_KEY)) {
                                    TransactionSynchronizationManager.unbindResource(
                                            AUDIT_LOG_BUFFER_KEY);
                                }
                            }
                        }
                    });
            newBuffer.add(row);
        } else {
            buffer.add(row);
        }
    }

    private void insertRows(List<AuditLogRow> rows) {
        try {
            jdbcTemplate.batchUpdate(
                    INSERT_SQL,
                    new BatchPreparedStatementSetter() {
                        @Override
                        public void setValues(PreparedStatement ps, int i) throws SQLException {
                            AuditLogRow r = rows.get(i);
                            ps.setObject(1, r.auditLogId());
                            ps.setObject(2, Timestamp.valueOf(r.occurredAt()));
                            ps.setString(3, r.action());
                            ps.setString(4, r.entityType());
                            ps.setString(5, r.entityId());
                            ps.setObject(6, r.actorId());
                            ps.setString(7, r.requestId());
                            ps.setString(8, r.requestPath());
                            ps.setString(9, r.summary());
                            ps.setString(10, r.detailsJson());
                        }

                        @Override
                        public int getBatchSize() {
                            return rows.size();
                        }
                    });
        } catch (DataAccessException e) {
            log.warn("Failed to insert {} audit log row(s)", rows.size(), e);
        }
    }
}
