package com.prdv.rdv.iam.config;

import com.prdv.rdv.iam.application.port.output.PasswordHasher;
import com.prdv.rdv.iam.application.port.output.PermissionRepository;
import com.prdv.rdv.iam.application.port.output.RoleRepository;
import com.prdv.rdv.iam.application.port.output.UserRepository;
import com.prdv.rdv.iam.domain.model.rbac.Permission;
import com.prdv.rdv.iam.domain.model.rbac.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Amorcage des roles systeme : creation si absent, RECONCILIATION si presents.
 *
 * <p>Le scenario de regression est le 403 « permission manquante » du module
 * 2.1 : la base avait ete amorcee avant l'ajout de {@code profile.health.read}
 * au role patient, le role existait donc le seed le sautait, et plus aucun
 * endpoint {@code /api/v1/health/**} n'etait accessible.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DataSeederRoleSyncTest {

    @Mock
    private PermissionRepository permissionRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordHasher passwordHasher;

    private DataSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new DataSeeder(permissionRepository, roleRepository, userRepository,
                passwordHasher, new IamProperties(), Clock.systemUTC());

        // Catalogue deja seme : seules les permissions demandees existent.
        lenient().when(permissionRepository.findByCode(anyString())).thenReturn(Optional.empty());
        lenient().when(permissionRepository.findByCodes(anyCollection())).thenAnswer(invocation -> {
            Collection<String> codes = invocation.getArgument(0);
            return codes.stream()
                    .map(code -> Permission.create(code, "test", "permission de test"))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        });

        // Le compte super-administrateur est deja cree : on sort immediatement.
        lenient().when(userRepository.existsByEmail(anyString())).thenReturn(true);
    }

    @Test
    @DisplayName("Base vierge : chaque role systeme est cree avec ses permissions")
    void createsMissingSystemRoles() {
        when(roleRepository.findByName(anyString())).thenReturn(Optional.empty());

        seeder.run(null);

        Map<String, Set<String>> saved = savedRolesByName();
        assertThat(saved.keySet()).containsAll(DataSeeder.rolePermissions().keySet());
        assertThat(saved.get(Role.PATIENT)).contains("profile.health.read", "profile.health.write");
        assertThat(saved.get(Role.PRACTITIONER)).contains("profile.health.read");
    }

    @Test
    @DisplayName("Role deja present mais incomplet : les permissions du code sont propagees")
    void reconcilesPermissionsOfAnOutdatedSystemRole() {
        Role stalePatient = Role.system(Role.PATIENT, "Role systeme " + Role.PATIENT, Set.of(
                Permission.create("iam.user.read", "iam", "ancienne permission"),
                Permission.create("profile.identity.read", "profile", "ancienne permission")));
        when(roleRepository.findByName(anyString())).thenAnswer(invocation ->
                Optional.of(Role.PATIENT.equals(invocation.getArgument(0))
                        ? stalePatient
                        : alignedRole(invocation.getArgument(0))));

        seeder.run(null);

        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository).save(captor.capture());
        Role reconciled = captor.getValue();
        assertThat(reconciled.getName()).isEqualTo(Role.PATIENT);
        assertThat(codesOf(reconciled)).containsAll(DataSeeder.rolePermissions().get(Role.PATIENT));
    }

    @Test
    @DisplayName("Roles deja alignes : aucune ecriture au demarrage")
    void writesNothingWhenRolesAreAligned() {
        when(roleRepository.findByName(anyString())).thenAnswer(invocation ->
                Optional.of(alignedRole(invocation.getArgument(0))));

        seeder.run(null);

        verify(roleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un role personnalise qui porte un nom systeme n'est jamais ecrase")
    void neverOverwritesACustomRole() {
        Role custom = Role.custom(Role.PATIENT, "role ajuste par un administrateur", Set.of(
                Permission.create("iam.user.read", "iam", "seule permission voulue")));
        when(roleRepository.findByName(anyString())).thenReturn(Optional.of(custom));

        seeder.run(null);

        verify(roleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Role systeme herite sans drapeau system : reconnu par sa description de seed")
    void reconcilesALegacySystemRoleWithoutTheSystemFlag() {
        Role legacy = Role.custom(Role.PATIENT, "Role systeme " + Role.PATIENT, Set.of(
                Permission.create("iam.user.read", "iam", "ancienne permission")));
        when(roleRepository.findByName(anyString())).thenAnswer(invocation ->
                Optional.of(Role.PATIENT.equals(invocation.getArgument(0))
                        ? legacy
                        : alignedRole(invocation.getArgument(0))));

        seeder.run(null);

        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository).save(captor.capture());
        assertThat(codesOf(captor.getValue()))
                .containsAll(DataSeeder.rolePermissions().get(Role.PATIENT));
    }

    // ------------------------------------------------------------------ outils

    private Map<String, Set<String>> savedRolesByName() {
        ArgumentCaptor<Role> captor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues().stream()
                .collect(Collectors.toMap(Role::getName, DataSeederRoleSyncTest::codesOf));
    }

    private Role alignedRole(String name) {
        Set<String> expected = DataSeeder.rolePermissions().get(name);
        Set<Permission> permissions = expected.stream()
                .map(code -> Permission.create(code, "test", "permission de test"))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return Role.system(name, "Role systeme " + name, permissions);
    }

    private static Set<String> codesOf(Role role) {
        return role.getPermissions().stream()
                .map(Permission::getCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
