package org.kshrd.hrdroomservice.persistence.audit;

import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import org.kshrd.hrdroomservice.persistence.entity.AuditLogEntity;

public class AuditTableEntityListener {

    private static volatile AuditLogRecorder recorder;
    private static volatile PersistenceUnitUtil persistenceUnitUtil;

    public static void register(AuditLogRecorder auditLogRecorder, PersistenceUnitUtil util) {
        AuditTableEntityListener.recorder = auditLogRecorder;
        AuditTableEntityListener.persistenceUnitUtil = util;
    }

    @PostPersist
    public void afterPersist(Object entity) {
        handle("CREATE", entity);
    }

    @PostUpdate
    public void afterUpdate(Object entity) {
        handle("UPDATE", entity);
    }

    @PostRemove
    public void afterRemove(Object entity) {
        handle("DELETE", entity);
    }

    private void handle(String action, Object entity) {
        AuditLogRecorder rec = recorder;
        PersistenceUnitUtil util = persistenceUnitUtil;
        if (rec == null || util == null) {
            return;
        }
        if (entity instanceof AuditLogEntity) {
            return;
        }
        if (!util.isLoaded(entity)) {
            return;
        }
        Object id = util.getIdentifier(entity);
        if (id == null) {
            return;
        }
        rec.record(action, entity, id);
    }
}
