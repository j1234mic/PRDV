package com.prdv.rdv.iam.adapter.in.web.rest;

import com.prdv.rdv.iam.adapter.in.web.dto.EstablishmentDtos;
import com.prdv.rdv.iam.application.command.ProfileCommands;
import com.prdv.rdv.iam.application.command.RegistrationCommands;
import com.prdv.rdv.iam.application.port.input.EstablishmentRegistrationUseCase;
import com.prdv.rdv.iam.application.result.Views;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/establishments")
@Tag(name = "Etablissements", description = "Comptes entreprise, departements, validation des rattachements")
public class EstablishmentController {

    private final EstablishmentRegistrationUseCase establishmentUseCase;

    public EstablishmentController(EstablishmentRegistrationUseCase establishmentUseCase) {
        this.establishmentUseCase = establishmentUseCase;
    }

    @PostMapping("/register")
    public Views.OtpSentView register(@Valid @RequestBody EstablishmentDtos.RegisterEstablishmentRequest request) {
        return establishmentUseCase.register(new RegistrationCommands.RegisterEstablishment(
                request.email(), request.phone(), request.password(), request.legalName(),
                request.siret(), request.address(), request.departments(), null));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('iam.establishment.read')")
    public Views.EstablishmentView me() {
        return establishmentUseCase.currentProfile();
    }

    @GetMapping("/me/memberships")
    @PreAuthorize("hasAuthority('iam.establishment.read')")
    public List<Views.MembershipView> memberships() {
        return establishmentUseCase.memberships();
    }

    @PostMapping("/me/memberships/{id}/decision")
    @PreAuthorize("hasAuthority('iam.establishment.manage')")
    public Views.MembershipView reviewMembership(@PathVariable Long id,
                                                 @Valid @RequestBody EstablishmentDtos.MembershipDecisionRequest request) {
        return establishmentUseCase.reviewMembership(
                new ProfileCommands.ReviewMembership(id, request.approved()));
    }
}
