package com.prdv.rdv.iam.adapter.out.persistence.entity;

import com.prdv.rdv.iam.domain.model.verification.GroupInvoice;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "establishment_group_invoices")
@Getter
@Setter
public class GroupInvoiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long establishmentUserId;

    @Column(nullable = false, length = 80)
    private String invoiceNumber;

    private LocalDate periodStart;

    private LocalDate periodEnd;

    @Column(length = 100)
    private String department;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "establishment_group_invoice_practitioners",
            joinColumns = @JoinColumn(name = "group_invoice_id"))
    @Column(name = "practitioner_user_id")
    private Set<Long> practitionerUserIds = new HashSet<>();

    private int consultationsCount;

    private long totalAmountCents;

    private long platformFeeCents;

    private long netAmountCents;

    @Column(length = 10)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private GroupInvoice.InvoiceStatus status;

    private Instant createdAt;

    private Instant paidAt;
}
