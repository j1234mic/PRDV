package com.prdv.rdv.iam.adapter.in.web.rest;

import com.prdv.rdv.iam.adapter.in.web.dto.DelegationDtos;
import com.prdv.rdv.iam.application.command.AdminCommands;
import com.prdv.rdv.iam.application.port.input.DelegationManagementUseCase;
import com.prdv.rdv.iam.application.result.Views;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/delegations")
@Tag(name = "Delegations", description = "Delegation temporaire de permissions")
public class DelegationController {

    private final DelegationManagementUseCase delegationUseCase;

    public DelegationController(DelegationManagementUseCase delegationUseCase) {
        this.delegationUseCase = delegationUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('iam.delegation.write')")
    public Views.DelegationView grant(@Valid @RequestBody DelegationDtos.GrantDelegationRequest request) {
        return delegationUseCase.grant(new AdminCommands.GrantDelegation(
                request.granteeUserId(), request.permissionCodes(), request.reason(),
                request.validFrom(), request.validUntil()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('iam.delegation.read')")
    public List<Views.DelegationView> list() {
        return delegationUseCase.delegationsForMe();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('iam.delegation.write')")
    public ResponseEntity<Void> revoke(@PathVariable Long id) {
        delegationUseCase.revoke(id);
        return ResponseEntity.noContent().build();
    }
}
