package com.prdv.rdv.iam.config;

import com.prdv.rdv.iam.domain.model.rbac.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Garde-fou RBAC : toute autorite exigee par un {@code @PreAuthorize} doit
 * exister dans le catalogue de permissions ET etre accordee par au moins un
 * role systeme.
 *
 * <p>Sans ce controle, un endpoint peut exiger une permission que personne ne
 * possede et repondre systematiquement {@code 403 FORBIDDEN} sans erreur
 * apparente dans le code : c'est exactement le piege du module 2.1, ou les
 * endpoints {@code /api/v1/health/**} exigeaient {@code profile.health.read}
 * alors que le role patient amorce ne la portait pas.
 *
 * <p>Le balayage lit les sources (le classpath des tests contient les classes
 * compilees de {@code src/main/java}) : surefire travaille par defaut a la
 * racine du module.
 */
class ControllerPermissionCoverageTest {

    private static final Path SOURCES = Path.of("src/main/java");

    private static final Pattern AUTHORITY = Pattern.compile("hasAuthority\\('([^']+)'\\)");

    @Test
    @DisplayName("Chaque permission exigee par un controleur est au catalogue et accordee a un role")
    void everyRequiredAuthorityIsSeededAndGranted() throws IOException {
        assertThat(Files.isDirectory(SOURCES))
                .as("tests executes depuis la racine du module (src/main/java doit exister)")
                .isTrue();

        Set<String> required = requiredAuthorities();
        Set<String> catalog = DataSeeder.allPermissionCodes();
        Set<String> granted = DataSeeder.rolePermissions().values().stream()
                .flatMap(codes -> codes.stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        assertThat(required).isNotEmpty();

        Set<String> unknown = required.stream().filter(code -> !catalog.contains(code))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        assertThat(unknown)
                .as("permissions exigees par un controleur mais absentes du catalogue DataSeeder")
                .isEmpty();

        Set<String> ungranted = required.stream().filter(code -> !granted.contains(code))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        assertThat(ungranted)
                .as("permissions exigees par un controleur mais accordees a aucun role systeme")
                .isEmpty();
    }

    @Test
    @DisplayName("Module 2.1 : le patient lit et ecrit sa sante, le praticien la lit")
    void connectedHealthPermissionsAreGranted() {
        assertThat(DataSeeder.rolePermissions().get(Role.PATIENT))
                .contains("profile.health.read", "profile.health.write");
        assertThat(DataSeeder.rolePermissions().get(Role.PRACTITIONER))
                .contains("profile.health.read");
    }

    // ------------------------------------------------------------------ outils

    private static Set<String> requiredAuthorities() throws IOException {
        Set<String> required = new LinkedHashSet<>();
        try (var paths = Files.walk(SOURCES)) {
            for (Path file : paths.filter(p -> p.getFileName().toString().endsWith("Controller.java"))
                    .collect(Collectors.toSet())) {
                String className = SOURCES.relativize(file).toString()
                        .replace('/', '.')
                        .replace(".java", "");
                Class<?> controller = load(className);
                if (!controller.isAnnotationPresent(RestController.class)) {
                    continue;
                }
                collect(controller.getAnnotation(PreAuthorize.class), required);
                for (Method method : controller.getDeclaredMethods()) {
                    collect(method.getAnnotation(PreAuthorize.class), required);
                }
            }
        }
        return required;
    }

    private static Class<?> load(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Controleur introuvable sur le classpath : " + className, ex);
        }
    }

    private static void collect(PreAuthorize annotation, Set<String> required) {
        if (annotation == null) {
            return;
        }
        Matcher matcher = AUTHORITY.matcher(annotation.value());
        while (matcher.find()) {
            required.add(matcher.group(1));
        }
    }
}
