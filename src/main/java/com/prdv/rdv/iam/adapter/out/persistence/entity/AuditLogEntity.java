package com.prdv.rdv.iam.adapter.out.persistence.entity;

import com.prdv.rdv.iam.domain.model.audit.AuditLog;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_user", columnList = "user_id"),
        @Index(name = "idx_audit_created", columnList = "created_at")
})
@Getter
@Setter
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private AuditLog.Action action;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private AuditLog.Outcome outcome;

    @Column(length = 60)
    private String resourceType;

    @Column(length = 64)
    private String resourceId;

    @Column(length = 500)
    private String detail;

    @Column(length = 64)
    private String ipAddress;

    @Column(nullable = false)
    private Instant createdAt;
}
