package com.prdv.rdv.iam.application.port.input;

import com.prdv.rdv.iam.application.result.Views;

/** Consultation des journaux d'audit (audit trail, historique des connexions). */
public interface AuditQueryUseCase {

    Views.PagedResult<Views.AuditView> listAll(int page, int size);

    Views.PagedResult<Views.AuditView> listByUser(Long userId, int page, int size);
}
