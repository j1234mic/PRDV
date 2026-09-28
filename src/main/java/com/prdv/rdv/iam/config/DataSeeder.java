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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
        rolePermissions().forEach(this::syncSystemRole);
    }

    /**
     * Cree un role systeme absent, ou RECONCILIE ses permissions avec la
     * definition du code.
     *
     * <p>Un simple « creer si absent » suffit au premier demarrage, mais il fige
     * ensuite les roles : toute permission ajoutee a un role par la suite
     * ({@code profile.health.read} / {@code profile.health.write} du module 2.1,
     * par exemple) n'atteint jamais une base deja amorcee — le role existe, le
     * seed le saute — et les endpoints concernes repondent alors
     * {@code 403 FORBIDDEN « permission manquante »} alors que le catalogue de
     * permissions, le controleur et le jeton sont parfaitement corrects.
     *
     * <p>Comme les autorites sont resolues a chaque requete (filtre JWT), la
     * reconciliation prend effet des le redemarrage : inutile de supprimer
     * {@code ./data/prdv.mv.db} (ou la base MySQL) pour recuperer une permission.
     * Les roles non systemes (crees par un administrateur) ne sont jamais
     * touches : seul le code fait reference pour les roles systeme.
     */
    private void syncSystemRole(String roleName, Set<String> expectedCodes) {
        Optional<Role> existing = roleRepository.findByName(roleName);
        if (existing.isEmpty()) {
            roleRepository.save(Role.system(roleName, "Role systeme " + roleName,
                    permissionsFor(expectedCodes, roleName)));
            log.info("Role systeme cree : {}", roleName);
            return;
        }

        Role role = existing.get();
        if (!isSystemRole(role, roleName)) {
            log.warn("Role {} present mais non systeme : amorcage ignore (role personnalise)", roleName);
            return;
        }

        Set<String> currentCodes = role.getPermissions().stream()
                .map(Permission::getCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (currentCodes.equals(expectedCodes)) {
            return; // deja aligne : aucune ecriture
        }

        role.setPermissions(permissionsFor(expectedCodes, roleName));
        roleRepository.save(role);
        log.warn("Role systeme {} reconcilie avec le code : ajoute {} / retire {}",
                roleName, difference(expectedCodes, currentCodes), difference(currentCodes, expectedCodes));
    }

    /**
     * Resout les codes en permissions persistees. Le catalogue est seme avant
     * les roles, donc un code introuvable signale une faute de frappe dans
     * {@link #PERMISSION_CATALOG} ou dans la definition du role.
     */
    private Set<Permission> permissionsFor(Set<String> codes, String roleName) {
        Set<Permission> permissions = new LinkedHashSet<>(permissionRepository.findByCodes(codes));
        Set<String> resolved = permissions.stream()
                .map(Permission::getCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> unresolved = difference(codes, resolved);
        if (!unresolved.isEmpty()) {
            log.error("Permissions absentes du catalogue pour le role {} : {}", roleName, unresolved);
        }
        return permissions;
    }

    private static Set<String> difference(Set<String> left, Set<String> right) {
        Set<String> copy = new LinkedHashSet<>(left);
        copy.removeAll(right);
        return copy;
    }

    /**
     * Vrai si le role est bien un role systeme (les noms de {@link #rolePermissions()}
     * sont reserves par le code). Le drapeau {@code system} fait reference, mais une
     * base amorcee par une version anterieure du seed peut l'avoir a false : la
     * description ecrite par le seed (« Role systeme X ») sert alors de seconde
     * signature, sinon la reconciliation serait silencieusement ignoree.
     */
    private static boolean isSystemRole(Role role, String roleName) {
        return role.isSystem() || ("Role systeme " + roleName).equals(role.getDescription());
    }

    /** Definition de reference des roles systeme (exposee aux tests de coherence RBAC). */
    static Map<String, Set<String>> rolePermissions() {
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

    /** Codes du catalogue de permissions (expose aux tests de coherence RBAC). */
    static Set<String> allPermissionCodes() {
        Set<String> codes = new LinkedHashSet<>();
        for (String[] entry : PERMISSION_CATALOG) {
            codes.add(entry[0]);
        }
        return codes;
    }
}
