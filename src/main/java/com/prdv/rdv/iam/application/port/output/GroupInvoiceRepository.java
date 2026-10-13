package com.prdv.rdv.iam.application.port.output;

import com.prdv.rdv.iam.domain.model.verification.GroupInvoice;

import java.util.List;
import java.util.Optional;

public interface GroupInvoiceRepository {

    GroupInvoice save(GroupInvoice invoice);

    Optional<GroupInvoice> findById(Long id);

    List<GroupInvoice> findByEstablishmentUserId(Long establishmentUserId);
}
