package com.prdv.rdv.iam.application.service;

import com.prdv.rdv.iam.application.port.input.AuditQueryUseCase;
import com.prdv.rdv.iam.application.port.output.AuditLogRepository;
import com.prdv.rdv.iam.application.result.Views;
import com.prdv.rdv.iam.application.service.support.ViewMapper;
import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditQueryService implements AuditQueryUseCase {

    private final AuditLogRepository auditLogRepository;
    private final ViewMapper viewMapper;

    public AuditQueryService(AuditLogRepository auditLogRepository, ViewMapper viewMapper) {
        this.auditLogRepository = auditLogRepository;
        this.viewMapper = viewMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Views.PagedResult<Views.AuditView> listAll(int page, int size) {
        validate(page, size);
        var items = auditLogRepository.findAll(page, size).stream().map(viewMapper::auditView).toList();
        return new Views.PagedResult<>(items, auditLogRepository.count(), page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public Views.PagedResult<Views.AuditView> listByUser(Long userId, int page, int size) {
        validate(page, size);
        var items = auditLogRepository.findByUserId(userId, page, size).stream()
                .map(viewMapper::auditView).toList();
        return new Views.PagedResult<>(items, items.size(), page, size);
    }

    private void validate(int page, int size) {
        if (page < 0 || size <= 0 || size > 200) {
            throw IamException.of(IamErrorCode.VALIDATION_ERROR, "Pagination invalide");
        }
    }
}
