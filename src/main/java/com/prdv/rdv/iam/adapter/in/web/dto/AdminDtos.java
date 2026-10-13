package com.prdv.rdv.iam.adapter.in.web.dto;

import com.prdv.rdv.iam.domain.model.user.AdminType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record CreateRoleRequest(@NotBlank String name, String description,
                                    @NotEmpty Set<String> permissionCodes) {
    }

    public record UpdateRoleRequest(String description,
                                    @NotEmpty Set<String> permissionCodes) {
    }

    public record AssignRolesRequest(@NotEmpty Set<String> roleNames) {
    }

    public record UpdateDirectPermissionsRequest(@NotNull Set<String> directPermissions) {
    }

    public record CreateAdministratorRequest(
            @NotBlank @Email String email,
            String phone,
            @NotBlank @Size(min = 8, message = "au moins 8 caracteres") String password,
            @NotNull AdminType adminType) {
    }

    public record ReviewApplicationRequest(@NotNull boolean approved, String reason) {
    }

    public record ReviewDocumentRequest(@NotNull boolean approved, String note) {
    }

    public record ChangeStatusRequest(@NotNull boolean suspend) {
    }
}
