package org.kshrd.hrdroomservice.persistence.audit;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditTableListenerRegistrar implements InitializingBean {

    private final AuditLogRecorder auditLogRecorder;
    private final EntityManagerFactory entityManagerFactory;

    @Override
    public void afterPropertiesSet() {
        AuditTableEntityListener.register(
                auditLogRecorder, entityManagerFactory.getPersistenceUnitUtil());
    }
}
