package com.prdv.rdv.iam.adapter.in.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Set;

public final class DelegationDtos {

    private DelegationDtos() {
    }

    public record GrantDelegationRequest(
            @NotNull Long granteeUserId,
            @NotEmpty Set<String> permissionCodes,
            String reason,
            Instant validFrom,
            Instant validUntil) {
    }
}
