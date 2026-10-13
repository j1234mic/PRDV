package com.prdv.rdv.iam.domain.model.verification;

import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import lombok.Getter;
import lombok.Setter;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Facture groupee d'un etablissement de sante multi-praticiens.
 *
 * <p>Permet la facturation centralisee / consolidee par periode et par
 * service/departement pour les praticiens rattaches a l'etablissement.
 */
@Getter
@Setter
public class GroupInvoice {

    public enum InvoiceStatus {
        DRAFT,
        ISSUED,
        PAID,
        CANCELLED
    }

    private Long id;
    private Long establishmentUserId;
    private String invoiceNumber;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String department;
    private Set<Long> practitionerUserIds = new HashSet<>();
    private int consultationsCount;
    private long totalAmountCents;
    private long platformFeeCents;
    private long netAmountCents;
    private String currency = "EUR";
    private InvoiceStatus status = InvoiceStatus.ISSUED;
    private Instant createdAt;
    private Instant paidAt;

    public static GroupInvoice issue(Long establishmentUserId, String invoiceNumber,
                                     LocalDate periodStart, LocalDate periodEnd,
                                     String department, Set<Long> practitionerUserIds,
                                     int consultationsCount, long totalAmountCents,
                                     long platformFeeRateBasisPoints, Clock clock) {
        if (periodStart != null && periodEnd != null && periodEnd.isBefore(periodStart)) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR,
                    "La fin de periode de facturation ne peut pas preceder le debut");
        }
        if (totalAmountCents < 0) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR,
                    "Le montant total de la facture groupee doit etre positif ou nul");
        }
        if (consultationsCount < 0) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR,
                    "Le nombre de consultations ne peut pas etre negatif");
        }
        long feeCents = Math.max(0L, (totalAmountCents * platformFeeRateBasisPoints) / 10_000L);
        long netCents = Math.max(0L, totalAmountCents - feeCents);

        GroupInvoice invoice = new GroupInvoice();
        invoice.establishmentUserId = establishmentUserId;
        invoice.invoiceNumber = invoiceNumber;
        invoice.periodStart = periodStart;
        invoice.periodEnd = periodEnd;
        invoice.department = department;
        if (practitionerUserIds != null) {
            invoice.practitionerUserIds = new HashSet<>(practitionerUserIds);
        }
        invoice.consultationsCount = consultationsCount;
        invoice.totalAmountCents = totalAmountCents;
        invoice.platformFeeCents = feeCents;
        invoice.netAmountCents = netCents;
        invoice.currency = "EUR";
        invoice.status = InvoiceStatus.ISSUED;
        invoice.createdAt = clock.instant();
        return invoice;
    }

    public void markPaid(Clock clock) {
        if (this.status == InvoiceStatus.CANCELLED) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR,
                    "Impossible de payer une facture groupee annulee");
        }
        this.status = InvoiceStatus.PAID;
        this.paidAt = clock.instant();
    }

    public void cancel(Clock clock) {
        if (this.status == InvoiceStatus.PAID) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR,
                    "Impossible d'annuler une facture groupee deja payee");
        }
        this.status = InvoiceStatus.CANCELLED;
    }
}
