package org.kshrd.hrdroomservice.persistence.audit;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.kshrd.hrdroomservice.persistence.entity.AuditLogEntity;
import org.kshrd.hrdroomservice.persistence.repository.AuditLogRepository;
import org.kshrd.hrdroomservice.security.SecurityUtils;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogRecorder {

    private final AuditLogRepository auditLogRepository;

    public void record(String action, Object entity, Object identifier) {
        Object unproxied = Hibernate.unproxy(entity);
        AuditLogEntity row = new AuditLogEntity();
        row.setAuditLogId(UUID.randomUUID());
        row.setOccurredAt(LocalDateTime.now());
        row.setAction(action);
        row.setEntityType(unproxied.getClass().getSimpleName());
        row.setEntityId(String.valueOf(identifier));
        row.setActorId(SecurityUtils.currentUserId().orElse(null));
        String requestId = MDC.get("requestId");
        row.setRequestId(requestId != null && !requestId.isBlank() ? requestId : null);
        row.setSummary(unproxied.getClass().getSimpleName() + " " + identifier);
        row.setDetailsJson(null);
        auditLogRepository.save(row);
    }
}
