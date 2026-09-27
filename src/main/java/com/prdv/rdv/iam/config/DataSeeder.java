package com.prdv.rdv.iam.config;

import com.prdv.rdv.iam.application.port.output.PasswordHasher;
import com.prdv.rdv.iam.application.port.output.PermissionRepository;
import com.prdv.rdv.iam.application.port.output.RoleRepository;
import com.prdv.rdv.iam.application.port.output.UserRepository;
import com.prdv.rdv.iam.domain.model.rbac.Permission;
import com.prdv.rdv.iam.domain.model.rbac.Role;
import com.prdv.rdv.iam.domain.model.user.AccountStatus;
import com.prdv.rdv.iam.domain.model.user.AdminType;
import com.prdv.rdv.iam.domain.model.user.ProfileType;
import com.prdv.rdv.iam.domain.model.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Amorcage (seed) idempotent :
 * catalogue de permissions, roles systemes predefinis et super-administrateur.
 */
@Configuration
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final IamProperties properties;
    private final Clock clock;

    public DataSeeder(PermissionRepository permissionRepository, RoleRepository roleRepository,
                      UserRepository userRepository, PasswordHasher passwordHasher,
                      IamProperties properties, Clock clock) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedPermissions();
        seedRoles();
        seedSuperAdmin();
    }

    private void seedPermissions() {
        for (String[] entry : PERMISSION_CATALOG) {
            String code = entry[0];
            if (permissionRepository.findByCode(code).isEmpty()) {
                permissionRepository.saveAll(List.of(Permission.create(code, entry[1], entry[2])));
            }
        }
    }

    private void seedRoles() {
        rolePermissions().forEach((roleName, codes) -> {
            if (roleRepository.findByName(roleName).isEmpty()) {
                Set<Permission> permissions = new LinkedHashSet<>(permissionRepository.findByCodes(codes));
                Role role = Role.system(roleName, "Role systeme " + roleName, permissions);
                roleRepository.save(role);
                log.info("Role systeme cree : {}", roleName);
            }
        });
    }

    private Map<String, Set<String>> rolePermissions() {
        Map<String, Set<String>> rolePermissions = new LinkedHashMap<>();
        rolePermissions.put(Role.SUPER_ADMIN, allPermissionCodes());
        rolePermissions.put(Role.PATIENT, new LinkedHashSet<>(Arrays.asList(
                "iam.user.read", "iam.user.update", "iam.session.read", "iam.session.revoke",
                "iam.kyc.upload", "iam.kyc.read", "iam.import.data", "iam.delegation.read",
                "appointment.read", "appointment.write", "appointment.cancel",
                // Module 2 : le patient gere son profil, son dossier et ses donnees
                "profile.identity.read", "profile.identity.update",
                "medicalrecord.read", "medicalrecord.write",
                "profile.document.read", "profile.document.write", "profile.document.share",
                "profile.health.read", "profile.health.write",
                "profile.privacy.read", "profile.privacy.update",
                "profile.data.export", "profile.data.erase", "profile.rating.write")));
        rolePermissions.put(Role.PRACTITIONER, new LinkedHashSet<>(Arrays.asList(
                "iam.user.read", "iam.user.update", "iam.session.read", "iam.session.revoke",
                "iam.kyc.upload", "iam.kyc.read", "iam.practitioner.register", "iam.practitioner.read",
                "iam.establishment.read", "iam.delegation.read", "iam.delegation.write",
                "agenda.read", "agenda.write", "appointment.read", "appointment.write",
                "appointment.cancel", "medicalrecord.read", "prescription.write", "billing.read",
                // Module 2 : consultation des donnees patient (filtrée par les regles de
                // visibilite du patient) et gestion de son propre profil professionnel
                "profile.identity.read", "profile.document.read", "profile.health.read",
                "profile.privacy.read", "profile.data.export",
                "profile.dossier.read", "profile.dossier.write",
                "profile.location.read", "profile.location.write")));
        rolePermissions.put(Role.SECRETARY, new LinkedHashSet<>(Arrays.asList(
                "iam.user.read", "iam.session.read", "iam.practitioner.read",
                "iam.establishment.read", "iam.kyc.read", "iam.delegation.read",
                "agenda.read", "appointment.read", "appointment.write", "appointment.cancel",
                // Module 2 : acces delegé, toujours re-verifié par les regles de visibilite
                "profile.identity.read", "profile.document.read")));
        rolePermissions.put(Role.ESTABLISHMENT, new LinkedHashSet<>(Arrays.asList(
                "iam.user.read", "iam.user.update", "iam.session.read", "iam.session.revoke",
                "iam.establishment.register", "iam.establishment.read", "iam.establishment.manage",
                "iam.practitioner.read", "iam.secretary.register", "iam.kyc.read",
                "agenda.read", "appointment.read", "billing.read", "billing.write",
                // Module 2 : annuaire et lieux d'exercice des praticiens rattachés
                "profile.dossier.read", "profile.location.read")));
        rolePermissions.put(Role.MODERATOR, new LinkedHashSet<>(Arrays.asList(
                "iam.user.read", "iam.user.update", "iam.practitioner.read", "iam.establishment.read",
                "iam.kyc.read", "iam.kyc.review", "iam.practitioner.approve",
                "iam.audit.read", "iam.role.read",
                // Module 2 : consultation des dossiers professionnels (moderation des avis)
                "profile.dossier.read")));
        rolePermissions.put(Role.TECHNICAL_SUPPORT, new LinkedHashSet<>(Arrays.asList(
                "iam.user.read", "iam.session.read", "iam.session.revoke",
                "iam.audit.read", "iam.role.read")));
        rolePermissions.put(Role.COMMERCIAL_SUPPORT, new LinkedHashSet<>(Arrays.asList(
                "iam.user.read", "iam.practitioner.read", "iam.establishment.read",
                "iam.analytics.read")));
        rolePermissions.put(Role.DATA_ANALYST, new LinkedHashSet<>(Set.of("iam.analytics.read")));
        return rolePermissions;
    }

    private void seedSuperAdmin() {
        String email = properties.getAdmin().getEmail().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            return;
        }
        Role superAdminRole = roleRepository.findByName(Role.SUPER_ADMIN)
                .orElseThrow(() -> new IllegalStateException("Role super admin absent"));

        User admin = new User();
        admin.setEmail(email);
        admin.setPasswordHash(passwordHasher.hash(properties.getAdmin().getPassword()));
        admin.setProfileType(ProfileType.ADMIN);
        admin.setAdminType(AdminType.SUPER_ADMIN);
        admin.setStatus(AccountStatus.ACTIVE);
        admin.setEmailVerified(true);
        admin.setPhoneVerified(true);
        admin.setCreatedAt(clock.instant());
        admin.setUpdatedAt(clock.instant());
        admin.addRole(superAdminRole);
        userRepository.save(admin);

        log.warn("================================================================");
        log.warn(" Compte super-administrateur initial : {} (pensez a changer le mot de passe)", email);
        log.warn("================================================================");
    }

    // Catalogue des permissions : code | module | description
    private static final String[][] PERMISSION_CATALOG = {
            // Module IAM
            {"iam.user.read", "iam", "Consulter les comptes utilisateurs"},
            {"iam.user.update", "iam", "Modifier / suspendre les comptes"},
            {"iam.role.read", "iam", "Consulter roles et permissions"},
            {"iam.role.write", "iam", "Creer / modifier les roles et affectations"},
            {"iam.session.read", "iam", "Consulter ses sessions de connexion"},
            {"iam.session.revoke", "iam", "Revoquer des sessions"},
            {"iam.audit.read", "iam", "Consulter les journaux d'audit"},
            {"iam.practitioner.register", "iam", "S'inscrire comme praticien"},
            {"iam.practitioner.read", "iam", "Consulter les dossiers praticiens"},
            {"iam.practitioner.approve", "iam", "Valider / refuser un dossier praticien"},
            {"iam.establishment.register", "iam", "S'inscrire comme etablissement"},
            {"iam.establishment.read", "iam", "Consulter les etablissements et rattachements"},
            {"iam.establishment.manage", "iam", "Gerer un etablissement (equipe, rattachements)"},
            {"iam.secretary.register", "iam", "Creer des comptes secretaires"},
            {"iam.kyc.upload", "iam", "Deposer des documents KYC"},
            {"iam.kyc.read", "iam", "Consulter les documents KYC"},
            {"iam.kyc.review", "iam", "Revoir / valider les documents KYC"},
            {"iam.delegation.read", "iam", "Consulter les delegations de droits"},
            {"iam.delegation.write", "iam", "Accorder ou revoquer une delegation"},
            {"iam.import.data", "iam", "Importer ses donnees depuis une autre plateforme"},
            {"iam.analytics.read", "iam", "Consulter les donnees analytics anonymisees"},
            // Modules metiers suivants (apercu du plan de permissions)
            {"agenda.read", "agenda", "Consulter un agenda"},
            {"agenda.write", "agenda", "Gerer un agenda"},
            {"appointment.read", "appointment", "Consulter les rendez-vous"},
            {"appointment.write", "appointment", "Creer / deplacer des rendez-vous"},
            {"appointment.cancel", "appointment", "Annuler des rendez-vous"},
            {"medicalrecord.read", "medical", "Consulter des dossiers medicaux"},
            {"medicalrecord.write", "medical", "Completer un dossier medical"},
            // Module 2 : profils & gestion des donnees
            {"profile.identity.read", "profile", "Consulter un profil patient"},
            {"profile.identity.update", "profile", "Modifier un profil patient"},
            {"profile.document.read", "profile", "Consulter des documents medicaux"},
            {"profile.document.write", "profile", "Televerser et versionner des documents medicaux"},
            {"profile.document.share", "profile", "Partager un document avec un praticien"},
            {"profile.health.read", "profile", "Consulter mesures et alertes de sante"},
            {"profile.health.write", "profile", "Connecter un objet et importer des mesures"},
            {"profile.privacy.read", "profile", "Consulter preferences et consentements RGPD"},
            {"profile.privacy.update", "profile", "Modifier preferences et consentements RGPD"},
            {"profile.data.export", "profile", "Exporter ses donnees (RGPD art. 20)"},
            {"profile.data.erase", "profile", "Demander l'effacement de son compte (RGPD art. 17)"},
            {"profile.dossier.read", "profile", "Consulter un dossier professionnel praticien"},
            {"profile.dossier.write", "profile", "Gerer son dossier professionnel praticien"},
            {"profile.location.read", "profile", "Consulter les lieux d'exercice"},
            {"profile.location.write", "profile", "Gerer ses lieux d'exercice"},
            {"profile.rating.write", "profile", "Deposer un avis sur un praticien"},
            {"prescription.write", "medical", "Rediger des ordonnances"},
            {"billing.read", "billing", "Consulter la facturation"},
            {"billing.write", "billing", "Gerer la facturation groupee"}
    };

    private static Set<String> allPermissionCodes() {
        Set<String> codes = new LinkedHashSet<>();
        for (String[] entry : PERMISSION_CATALOG) {
            codes.add(entry[0]);
        }
        return codes;
    }
}
