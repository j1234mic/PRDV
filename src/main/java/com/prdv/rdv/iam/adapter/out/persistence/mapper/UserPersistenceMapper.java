package com.prdv.rdv.iam.adapter.out.persistence.mapper;

import com.prdv.rdv.iam.adapter.out.persistence.entity.EstablishmentProfileEntity;
import com.prdv.rdv.iam.adapter.out.persistence.entity.PatientProfileEntity;
import com.prdv.rdv.iam.adapter.out.persistence.entity.PermissionEntity;
import com.prdv.rdv.iam.adapter.out.persistence.entity.PractitionerProfileEntity;
import com.prdv.rdv.iam.adapter.out.persistence.entity.RoleEntity;
import com.prdv.rdv.iam.adapter.out.persistence.entity.SecretaryProfileEntity;
import com.prdv.rdv.iam.adapter.out.persistence.entity.UserEntity;
import com.prdv.rdv.iam.domain.model.rbac.Permission;
import com.prdv.rdv.iam.domain.model.rbac.Role;
import com.prdv.rdv.iam.domain.model.user.EstablishmentProfile;
import com.prdv.rdv.iam.domain.model.user.PatientProfile;
import com.prdv.rdv.iam.domain.model.user.PractitionerProfile;
import com.prdv.rdv.iam.domain.model.user.SecretaryProfile;
import com.prdv.rdv.iam.domain.model.user.User;
import org.springframework.stereotype.Component;

import java.util.HashSet;

/**
 * Mapper isole le modele JPA du domaine (l'inverse de la dependance serait
 * une fuite technique dans le coeur metier).
 */
@Component
public class UserPersistenceMapper {

    // -------------------------------------------------------------- User
    public void copyToEntity(User source, UserEntity target) {
        target.setEmail(source.getEmail());
        target.setPhone(source.getPhone());
        target.setPasswordHash(source.getPasswordHash());
        target.setProfileType(source.getProfileType());
        target.setStatus(source.getStatus());
        target.setAdminType(source.getAdminType());
        target.setEmailVerified(source.isEmailVerified());
        target.setPhoneVerified(source.isPhoneVerified());
        target.setMfaEnabled(source.isMfaEnabled());
        target.setTotpSecret(source.getTotpSecret());
        target.setPendingTotpSecret(source.getPendingTotpSecret());
        target.setFailedAttempts(source.getFailedAttempts());
        target.setLockedUntil(source.getLockedUntil());
        target.setLastLoginAt(source.getLastLoginAt());
        target.setTokenVersion(source.getTokenVersion());
        target.setDirectPermissions(new HashSet<>(source.getDirectPermissions()));
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
    }

    public User toDomain(UserEntity e) {
        User u = new User();
        u.setId(e.getId());
        u.setEmail(e.getEmail());
        u.setPhone(e.getPhone());
        u.setPasswordHash(e.getPasswordHash());
        u.setProfileType(e.getProfileType());
        u.setStatus(e.getStatus());
        u.setAdminType(e.getAdminType());
        u.setEmailVerified(e.isEmailVerified());
        u.setPhoneVerified(e.isPhoneVerified());
        u.setMfaEnabled(e.isMfaEnabled());
        u.setTotpSecret(e.getTotpSecret());
        u.setPendingTotpSecret(e.getPendingTotpSecret());
        u.setFailedAttempts(e.getFailedAttempts());
        u.setLockedUntil(e.getLockedUntil());
        u.setLastLoginAt(e.getLastLoginAt());
        u.setTokenVersion(e.getTokenVersion());
        u.setDirectPermissions(new HashSet<>(e.getDirectPermissions()));
        u.setCreatedAt(e.getCreatedAt());
        u.setUpdatedAt(e.getUpdatedAt());

        HashSet<Role> roles = new HashSet<>();
        for (RoleEntity roleEntity : e.getRoles()) {
            roles.add(toRoleDomain(roleEntity));
        }
        u.setRoles(roles);
        return u;
    }

    // -------------------------------------------------------------- RBAC
    public Role toRoleDomain(RoleEntity e) {
        Role role = new Role();
        role.setId(e.getId());
        role.setName(e.getName());
        role.setDescription(e.getDescription());
        role.setSystem(e.isSystemRole());
        HashSet<Permission> permissions = new HashSet<>();
        for (PermissionEntity pe : e.getPermissions()) {
            permissions.add(toPermissionDomain(pe));
        }
        role.setPermissions(permissions);
        return role;
    }

    public Permission toPermissionDomain(PermissionEntity e) {
        Permission p = new Permission();
        p.setId(e.getId());
        p.setCode(e.getCode());
        p.setModule(e.getModule());
        p.setDescription(e.getDescription());
        return p;
    }

    public PermissionEntity toPermissionEntity(Permission p) {
        PermissionEntity e = new PermissionEntity();
        e.setId(p.getId());
        e.setCode(p.getCode());
        e.setModule(p.getModule());
        e.setDescription(p.getDescription());
        return e;
    }

    // ----------------------------------------------------------- Profiles
    public PatientProfileEntity toEntity(PatientProfile d) {
        PatientProfileEntity e = new PatientProfileEntity();
        e.setUserId(d.getUserId());
        e.setFirstName(d.getFirstName());
        e.setLastName(d.getLastName());
        e.setBirthDate(d.getBirthDate());
        e.setGender(d.getGender());
        e.setGuardianUserId(d.getGuardianUserId());
        e.setIdentityDocumentToken(d.getIdentityDocumentToken());
        e.setKycLevel(d.getKycLevel());
        e.setImportedFrom(d.getImportedFrom());
        e.setCreatedAt(d.getCreatedAt());
        e.setUpdatedAt(d.getUpdatedAt());
        return e;
    }

    public PatientProfile toDomain(PatientProfileEntity e) {
        PatientProfile d = new PatientProfile();
        d.setId(e.getId());
        d.setUserId(e.getUserId());
        d.setFirstName(e.getFirstName());
        d.setLastName(e.getLastName());
        d.setBirthDate(e.getBirthDate());
        d.setGender(e.getGender());
        d.setGuardianUserId(e.getGuardianUserId());
        d.setIdentityDocumentToken(e.getIdentityDocumentToken());
        d.setKycLevel(e.getKycLevel());
        d.setImportedFrom(e.getImportedFrom());
        d.setCreatedAt(e.getCreatedAt());
        d.setUpdatedAt(e.getUpdatedAt());
        return d;
    }

    public PractitionerProfileEntity toEntity(PractitionerProfile d) {
        PractitionerProfileEntity e = new PractitionerProfileEntity();
        e.setUserId(d.getUserId());
        e.setFirstName(d.getFirstName());
        e.setLastName(d.getLastName());
        e.setSpecialty(d.getSpecialty());
        e.setRppsNumber(d.getRppsNumber());
        e.setAdeliNumber(d.getAdeliNumber());
        e.setRppsVerified(d.isRppsVerified());
        e.setAdeliVerified(d.isAdeliVerified());
        e.setDiplomaVerified(d.isDiplomaVerified());
        e.setProfessionalInsuranceVerified(d.isProfessionalInsuranceVerified());
        e.setBankAccountVerified(d.isBankAccountVerified());
        e.setRibToken(d.getRibToken());
        e.setMaskedIban(d.getMaskedIban());
        e.setValidationNote(d.getValidationNote());
        e.setCreatedAt(d.getCreatedAt());
        e.setUpdatedAt(d.getUpdatedAt());
        return e;
    }

    public PractitionerProfile toDomain(PractitionerProfileEntity e) {
        PractitionerProfile d = new PractitionerProfile();
        d.setId(e.getId());
        d.setUserId(e.getUserId());
        d.setFirstName(e.getFirstName());
        d.setLastName(e.getLastName());
        d.setSpecialty(e.getSpecialty());
        d.setRppsNumber(e.getRppsNumber());
        d.setAdeliNumber(e.getAdeliNumber());
        d.setRppsVerified(e.isRppsVerified());
        d.setAdeliVerified(e.isAdeliVerified());
        d.setDiplomaVerified(e.isDiplomaVerified());
        d.setProfessionalInsuranceVerified(e.isProfessionalInsuranceVerified());
        d.setBankAccountVerified(e.isBankAccountVerified());
        d.setRibToken(e.getRibToken());
        d.setMaskedIban(e.getMaskedIban());
        d.setValidationNote(e.getValidationNote());
        d.setCreatedAt(e.getCreatedAt());
        d.setUpdatedAt(e.getUpdatedAt());
        return d;
    }

    public SecretaryProfileEntity toEntity(SecretaryProfile d) {
        SecretaryProfileEntity e = new SecretaryProfileEntity();
        e.setUserId(d.getUserId());
        e.setFirstName(d.getFirstName());
        e.setLastName(d.getLastName());
        e.setSupervisedPractitionerIds(new HashSet<>(d.getSupervisedPractitionerIds()));
        e.setCreatedAt(d.getCreatedAt());
        e.setUpdatedAt(e.getUpdatedAt());
        return e;
    }

    public SecretaryProfile toDomain(SecretaryProfileEntity e) {
        SecretaryProfile d = new SecretaryProfile();
        d.setId(e.getId());
        d.setUserId(e.getUserId());
        d.setFirstName(e.getFirstName());
        d.setLastName(e.getLastName());
        d.setSupervisedPractitionerIds(new HashSet<>(e.getSupervisedPractitionerIds()));
        d.setCreatedAt(e.getCreatedAt());
        d.setUpdatedAt(e.getUpdatedAt());
        return d;
    }

    public EstablishmentProfileEntity toEntity(EstablishmentProfile d) {
        EstablishmentProfileEntity e = new EstablishmentProfileEntity();
        e.setUserId(d.getUserId());
        e.setLegalName(d.getLegalName());
        e.setSiret(d.getSiret());
        e.setAddress(d.getAddress());
        e.setDepartments(new java.util.ArrayList<>(d.getDepartments()));
        e.setCreatedAt(d.getCreatedAt());
        e.setUpdatedAt(d.getUpdatedAt());
        return e;
    }

    public EstablishmentProfile toDomain(EstablishmentProfileEntity e) {
        EstablishmentProfile d = new EstablishmentProfile();
        d.setId(e.getId());
        d.setUserId(e.getUserId());
        d.setLegalName(e.getLegalName());
        d.setSiret(e.getSiret());
        d.setAddress(e.getAddress());
        d.setDepartments(new java.util.ArrayList<>(e.getDepartments()));
        d.setCreatedAt(e.getCreatedAt());
        d.setUpdatedAt(e.getUpdatedAt());
        return d;
    }
}
