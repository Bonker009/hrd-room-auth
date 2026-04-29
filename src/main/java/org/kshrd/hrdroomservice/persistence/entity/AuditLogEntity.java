package org.kshrd.hrdroomservice.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
public class AuditLogEntity {

    @Id
    @Column(name = "audit_log_id")
    private UUID auditLogId;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "action", nullable = false, length = 16)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 128)
    private String entityType;

    @Column(name = "entity_id", nullable = false, length = 64)
    private String entityId;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @Column(name = "request_path", length = 1024)
    private String requestPath;

    @Column(name = "summary")
    private String summary;

    @Column(name = "details_json")
    private String detailsJson;
}
