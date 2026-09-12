package com.prdv.rdv.iam.application.port.output;

import com.prdv.rdv.iam.domain.model.audit.AuditLog;

import java.util.List;

public interface AuditLogRepository {

    AuditLog save(AuditLog logEntry);

    List<AuditLog> findAll(int page, int size);

    List<AuditLog> findByUserId(Long userId, int page, int size);

    long count();
}
