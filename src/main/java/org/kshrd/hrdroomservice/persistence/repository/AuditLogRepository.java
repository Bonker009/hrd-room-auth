package org.kshrd.hrdroomservice.persistence.repository;

import java.util.UUID;
import org.kshrd.hrdroomservice.persistence.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {}
