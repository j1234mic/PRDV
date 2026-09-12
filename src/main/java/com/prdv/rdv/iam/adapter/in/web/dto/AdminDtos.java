package com.prdv.rdv.iam.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record CreateRoleRequest(@NotBlank String name, String description,
                                    @NotEmpty Set<String> permissionCodes) {
    }

    public record AssignRolesRequest(@NotEmpty Set<String> roleNames) {
    }

    public record ReviewApplicationRequest(@NotNull boolean approved, String reason) {
    }

    public record ReviewDocumentRequest(@NotNull boolean approved, String note) {
    }

    public record ChangeStatusRequest(@NotNull boolean suspend) {
    }
}
