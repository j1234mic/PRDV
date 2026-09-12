package com.prdv.rdv.iam.application.service.support;

import com.prdv.rdv.iam.application.result.Views;
import com.prdv.rdv.iam.domain.model.audit.AuditLog;
import com.prdv.rdv.iam.domain.model.auth.Delegation;
import com.prdv.rdv.iam.domain.model.auth.UserSession;
import com.prdv.rdv.iam.domain.model.rbac.Permission;
import com.prdv.rdv.iam.domain.model.rbac.Role;
import com.prdv.rdv.iam.domain.model.user.EstablishmentProfile;
import com.prdv.rdv.iam.domain.model.user.PatientProfile;
import com.prdv.rdv.iam.domain.model.user.PractitionerProfile;
import com.prdv.rdv.iam.domain.model.user.SecretaryProfile;
import com.prdv.rdv.iam.domain.model.user.User;
import com.prdv.rdv.iam.domain.model.verification.EstablishmentMembership;
import com.prdv.rdv.iam.domain.model.verification.KycDocument;
import com.prdv.rdv.iam.domain.model.verification.PractitionerContract;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Mapper domaine -> vues de lecture (les entites JPA ne franchissent jamais
 * la frontiere des adapteurs web).
 */
@Component
public class ViewMapper {

    public Views.UserView userView(User user) {
        List<String> roles = user.getRoles().stream().map(Role::getName).sorted().toList();
        return new Views.UserView(user.getId(), user.getEmail(), user.getPhone(),
                user.getProfileType(), user.getStatus(), user.getAdminType(),
                user.isEmailVerified(), user.isPhoneVerified(), user.isMfaEnabled(),
                roles, user.getCreatedAt());
    }

    public Views.PatientView patientView(User user, PatientProfile p) {
        return new Views.PatientView(userView(user), p.getFirstName(), p.getLastName(),
                p.getBirthDate(), p.getGender(), p.getGuardianUserId(),
                p.getKycLevel(), p.getImportedFrom());
    }

    public Views.PractitionerView practitionerView(User user, PractitionerProfile p) {
        return new Views.PractitionerView(userView(user), p.getFirstName(), p.getLastName(),
                p.getSpecialty(), p.getRppsNumber(), p.getAdeliNumber(),
                p.isRppsVerified(), p.isAdeliVerified(), p.isDiplomaVerified(),
                p.isProfessionalInsuranceVerified(), p.isBankAccountVerified(),
                p.getMaskedIban(), p.isApplicationComplete());
    }

    public Views.SecretaryView secretaryView(User user, SecretaryProfile s) {
        return new Views.SecretaryView(userView(user), s.getFirstName(), s.getLastName(),
                s.getSupervisedPractitionerIds().stream().sorted().toList());
    }

    public Views.EstablishmentView establishmentView(User user, EstablishmentProfile e) {
        return new Views.EstablishmentView(userView(user), e.getLegalName(), e.getSiret(),
                e.getAddress(), List.copyOf(e.getDepartments()));
    }

    public Views.SessionView sessionView(UserSession s, boolean current) {
        UserSession.GeoLocation geo = s.getGeoLocation();
        return new Views.SessionView(s.getId(), s.getIpAddress(), s.getUserAgent(),
                s.getDeviceLabel(), geo == null ? null : geo.country(),
                geo == null ? null : geo.city(),
                s.isSuspicious(), current, s.getCreatedAt(), s.getLastSeenAt());
    }

    public Views.KycDocumentView kycView(KycDocument d) {
        return new Views.KycDocumentView(d.getId(), d.getOwnerUserId(), d.getType(),
                d.getOriginalFilename(), d.getContentType(), d.getSizeBytes(),
                d.getStatus(), d.getReviewNote(), d.getUploadedAt(), d.getReviewedAt());
    }

    public Views.MembershipView membershipView(EstablishmentMembership m) {
        return new Views.MembershipView(m.getId(), m.getEstablishmentUserId(),
                m.getPractitionerUserId(), m.getMemberRole(), m.getStatus(),
                m.getValidFrom(), m.getValidUntil(), m.getRequestedAt());
    }

    public Views.ContractView contractView(PractitionerContract c) {
        return new Views.ContractView(c.getId(), c.getPractitionerUserId(), c.getVersion(),
                c.getContentHash(), c.getIpAddress(), c.getAcceptedAt());
    }

    public Views.RoleView roleView(Role role) {
        List<String> codes = role.getPermissions().stream()
                .map(Permission::getCode).sorted().toList();
        return new Views.RoleView(role.getId(), role.getName(), role.getDescription(),
                role.isSystem(), codes);
    }

    public Views.PermissionView permissionView(Permission p) {
        return new Views.PermissionView(p.getCode(), p.getModule(), p.getDescription());
    }

    public Views.AuditView auditView(AuditLog a) {
        return new Views.AuditView(a.getId(), a.getUserId(),
                a.getAction() == null ? null : a.getAction().name(),
                a.getOutcome() == null ? null : a.getOutcome().name(),
                a.getResourceType(), a.getResourceId(), a.getDetail(),
                a.getIpAddress(), a.getCreatedAt());
    }

    public Views.DelegationView delegationView(Delegation d) {
        return new Views.DelegationView(d.getId(), d.getGranterUserId(), d.getGranteeUserId(),
                d.getPermissionCodes().stream().sorted().toList(),
                d.getReason(), d.getValidFrom(), d.getValidUntil(), d.getRevokedAt());
    }

    public List<Views.UserView> userViews(List<User> users) {
        if (users == null) {
            return Collections.emptyList();
        }
        return users.stream().map(this::userView).toList();
    }
}
