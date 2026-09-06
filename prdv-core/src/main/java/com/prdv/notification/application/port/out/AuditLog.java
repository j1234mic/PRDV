package com.prdv.notification.application.port.out;

/** Audit trail des actions sensibles (module 1.1 secretaires / 8.2 tracabilite DME). */
public interface AuditLog {
    void record(String action, String detail);
}
