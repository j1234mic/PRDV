package com.prdv.rdv.iam.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prdv.rdv.iam.adapter.in.web.dto.PractitionerDtos;
import com.prdv.rdv.iam.application.command.ProfileCommands;
import com.prdv.rdv.iam.application.port.input.KycDocumentsUseCase;
import com.prdv.rdv.iam.application.port.output.AuditLogRepository;
import com.prdv.rdv.iam.application.port.output.BankAccountVerificationPort;
import com.prdv.rdv.iam.application.port.output.ContractRepository;
import com.prdv.rdv.iam.application.port.output.DomainEventPublisher;
import com.prdv.rdv.iam.application.port.output.EstablishmentProfileRepository;
import com.prdv.rdv.iam.application.port.output.MedicalRegistryPort;
import com.prdv.rdv.iam.application.port.output.MembershipRepository;
import com.prdv.rdv.iam.application.port.output.PractitionerProfileRepository;
import com.prdv.rdv.iam.application.port.output.SecurityContextPort;
import com.prdv.rdv.iam.application.port.output.TokenizationPort;
import com.prdv.rdv.iam.application.port.output.UserRepository;
import com.prdv.rdv.iam.application.result.Views;
import com.prdv.rdv.iam.application.service.support.AuditLogger;
import com.prdv.rdv.iam.application.service.support.RegistrationSupport;
import com.prdv.rdv.iam.application.service.support.ViewMapper;
import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import com.prdv.rdv.iam.domain.model.user.AccountStatus;
import com.prdv.rdv.iam.domain.model.user.EstablishmentProfile;
import com.prdv.rdv.iam.domain.model.user.PractitionerProfile;
import com.prdv.rdv.iam.domain.model.user.ProfileType;
import com.prdv.rdv.iam.domain.model.user.User;
import com.prdv.rdv.iam.domain.model.verification.EstablishmentMembership;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests de non-regression sur {@code POST /api/v1/practitioners/me/memberships} :
 * resolution de l'etablissement (par {@code users.id} ou {@code establishment_profiles.id}),
 * idempotence sur demande en attente, validation des dates et deserialisation tolerante
 * des roles et dates.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PractitionerMembershipTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-10-03T10:00:00Z"), ZoneOffset.UTC);
    private static final Long PRACTITIONER_USER_ID = 3L;
    private static final Long ESTABLISHMENT_USER_ID = 4L;
    private static final Long ESTABLISHMENT_PROFILE_ID = 1L;

    @Mock
    private RegistrationSupport registrationSupport;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PractitionerProfileRepository practitionerProfileRepository;
    @Mock
    private EstablishmentProfileRepository establishmentProfileRepository;
    @Mock
    private MedicalRegistryPort medicalRegistry;
    @Mock
    private BankAccountVerificationPort bankVerification;
    @Mock
    private TokenizationPort tokenization;
    @Mock
    private KycDocumentsUseCase kycDocumentsUseCase;
    @Mock
    private ContractRepository contractRepository;
    @Mock
    private MembershipRepository membershipRepository;
    @Mock
    private SecurityContextPort securityContext;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private DomainEventPublisher eventPublisher;

    private PractitionerRegistrationService service;

    @BeforeEach
    void setUp() {
        ViewMapper viewMapper = new ViewMapper();
        AuditLogger auditLogger = new AuditLogger(auditLogRepository, FIXED_CLOCK);
        service = new PractitionerRegistrationService(
                registrationSupport, userRepository, practitionerProfileRepository,
                establishmentProfileRepository, medicalRegistry, bankVerification,
                tokenization, kycDocumentsUseCase, contractRepository,
                membershipRepository, securityContext, viewMapper, auditLogger,
                eventPublisher, FIXED_CLOCK);

        when(securityContext.requireCurrentUserId()).thenReturn(PRACTITIONER_USER_ID);
        PractitionerProfile practitionerProfile = PractitionerProfile.create(
                PRACTITIONER_USER_ID, "Jean", "Dupont", "Cardiologie",
                "10001234567", "123456789", FIXED_CLOCK);
        when(practitionerProfileRepository.findByUserId(PRACTITIONER_USER_ID))
                .thenReturn(Optional.of(practitionerProfile));
        when(membershipRepository.save(any())).thenAnswer(invocation -> {
            EstablishmentMembership m = invocation.getArgument(0);
            if (m.getId() == null) {
                m.setId(10L);
            }
            return m;
        });
    }

    @Test
    @DisplayName("Rattachement par userId d'un compte ESTABLISHMENT")
    void requestsMembershipByEstablishmentUserId() {
        User establishmentUser = user(ESTABLISHMENT_USER_ID, ProfileType.ESTABLISHMENT);
        when(userRepository.findById(ESTABLISHMENT_USER_ID)).thenReturn(Optional.of(establishmentUser));
        when(membershipRepository.findByPractitionerUserId(PRACTITIONER_USER_ID)).thenReturn(List.of());

        Views.MembershipView view = service.requestMembership(new ProfileCommands.RequestMembership(
                ESTABLISHMENT_USER_ID, EstablishmentMembership.MemberRole.EMPLOYEE,
                LocalDate.of(2026, 10, 3), LocalDate.of(2026, 12, 31)));

        assertThat(view.establishmentUserId()).isEqualTo(ESTABLISHMENT_USER_ID);
        assertThat(view.practitionerUserId()).isEqualTo(PRACTITIONER_USER_ID);
        assertThat(view.role()).isEqualTo(EstablishmentMembership.MemberRole.EMPLOYEE);
        assertThat(view.status()).isEqualTo(EstablishmentMembership.MembershipStatus.PENDING);
    }

    @Test
    @DisplayName("Rattachement par establishment_profiles.id (= 1) meme quand users.id = 1 est le super-admin")
    void resolvesEstablishmentByProfileIdWhenUserId1IsSuperAdmin() {
        User superAdmin = user(1L, ProfileType.ADMIN);
        User establishmentUser = user(ESTABLISHMENT_USER_ID, ProfileType.ESTABLISHMENT);
        EstablishmentProfile establishmentProfile = EstablishmentProfile.create(
                ESTABLISHMENT_USER_ID, "Cabinet du Parc", "81234567800012",
                "10 rue des Medecins, Paris", List.of("Cardiologie"), FIXED_CLOCK);
        establishmentProfile.setId(ESTABLISHMENT_PROFILE_ID);

        when(userRepository.findById(1L)).thenReturn(Optional.of(superAdmin));
        when(userRepository.findById(ESTABLISHMENT_USER_ID)).thenReturn(Optional.of(establishmentUser));
        when(establishmentProfileRepository.findById(ESTABLISHMENT_PROFILE_ID))
                .thenReturn(Optional.of(establishmentProfile));
        when(membershipRepository.findByPractitionerUserId(PRACTITIONER_USER_ID)).thenReturn(List.of());

        Views.MembershipView view = service.requestMembership(new ProfileCommands.RequestMembership(
                ESTABLISHMENT_PROFILE_ID, EstablishmentMembership.MemberRole.OWNER, null, null));

        assertThat(view.establishmentUserId()).isEqualTo(ESTABLISHMENT_USER_ID);
        assertThat(view.role()).isEqualTo(EstablishmentMembership.MemberRole.OWNER);
    }

    @Test
    @DisplayName("Compte non-etablissement sans profil d'etablissement correspondant -> VALIDATION_ERROR explicite")
    void rejectsNonEstablishmentAccountWhenNoProfileMatches() {
        User patientUser = user(2L, ProfileType.PATIENT);
        when(userRepository.findById(2L)).thenReturn(Optional.of(patientUser));
        when(establishmentProfileRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requestMembership(new ProfileCommands.RequestMembership(
                2L, EstablishmentMembership.MemberRole.EMPLOYEE, null, null)))
                .isInstanceOf(IamException.class)
                .satisfies(ex -> {
                    IamException iamEx = (IamException) ex;
                    assertThat(iamEx.getErrorCode()).isEqualTo(IamErrorCode.VALIDATION_ERROR);
                    assertThat(iamEx.getMessage()).contains("PATIENT").contains("ESTABLISHMENT");
                });
    }

    @Test
    @DisplayName("Un rattachement PENDING existant est mis a jour de maniere idempotente au lieu d'echouer")
    void updatesExistingPendingMembershipInsteadOfFailing() {
        User establishmentUser = user(ESTABLISHMENT_USER_ID, ProfileType.ESTABLISHMENT);
        when(userRepository.findById(ESTABLISHMENT_USER_ID)).thenReturn(Optional.of(establishmentUser));

        EstablishmentMembership pending = EstablishmentMembership.request(
                ESTABLISHMENT_USER_ID, PRACTITIONER_USER_ID,
                EstablishmentMembership.MemberRole.EMPLOYEE,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), FIXED_CLOCK);
        pending.setId(42L);
        when(membershipRepository.findByPractitionerUserId(PRACTITIONER_USER_ID)).thenReturn(List.of(pending));

        Views.MembershipView updated = service.requestMembership(new ProfileCommands.RequestMembership(
                ESTABLISHMENT_USER_ID, EstablishmentMembership.MemberRole.OWNER,
                LocalDate.of(2026, 10, 3), LocalDate.of(2026, 12, 31)));

        assertThat(updated.id()).isEqualTo(42L);
        assertThat(updated.role()).isEqualTo(EstablishmentMembership.MemberRole.OWNER);
        assertThat(updated.validFrom()).isEqualTo(LocalDate.of(2026, 10, 3));
        assertThat(updated.validUntil()).isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    @DisplayName("Date de fin anterieure a la date de debut -> VALIDATION_ERROR")
    void rejectsValidUntilBeforeValidFrom() {
        User establishmentUser = user(ESTABLISHMENT_USER_ID, ProfileType.ESTABLISHMENT);
        when(userRepository.findById(ESTABLISHMENT_USER_ID)).thenReturn(Optional.of(establishmentUser));

        assertThatThrownBy(() -> service.requestMembership(new ProfileCommands.RequestMembership(
                ESTABLISHMENT_USER_ID, EstablishmentMembership.MemberRole.EMPLOYEE,
                LocalDate.of(2026, 12, 31), LocalDate.of(2026, 10, 3))))
                .isInstanceOf(IamException.class)
                .satisfies(ex -> assertThat(((IamException) ex).getErrorCode())
                        .isEqualTo(IamErrorCode.VALIDATION_ERROR));
    }

    @Test
    @DisplayName("Deserialisation tolerante de MemberRole (casse, francais/anglais, vide -> defaut EMPLOYEE)")
    void deserializesMemberRoleAliasesAndDefaults() throws Exception {
        assertThat(EstablishmentMembership.MemberRole.from("OWNER"))
                .isEqualTo(EstablishmentMembership.MemberRole.OWNER);
        assertThat(EstablishmentMembership.MemberRole.from("titulaire"))
                .isEqualTo(EstablishmentMembership.MemberRole.OWNER);
        assertThat(EstablishmentMembership.MemberRole.from("Gérant"))
                .isEqualTo(EstablishmentMembership.MemberRole.OWNER);
        assertThat(EstablishmentMembership.MemberRole.from("employee"))
                .isEqualTo(EstablishmentMembership.MemberRole.EMPLOYEE);
        assertThat(EstablishmentMembership.MemberRole.from("Salarié"))
                .isEqualTo(EstablishmentMembership.MemberRole.EMPLOYEE);
        assertThat(EstablishmentMembership.MemberRole.from("COLLABORATEUR"))
                .isEqualTo(EstablishmentMembership.MemberRole.EMPLOYEE);
        assertThat(EstablishmentMembership.MemberRole.from("PRACTITIONER"))
                .isEqualTo(EstablishmentMembership.MemberRole.EMPLOYEE);
        assertThat(EstablishmentMembership.MemberRole.from("Remplaçant"))
                .isEqualTo(EstablishmentMembership.MemberRole.REPLACER);
        assertThat(EstablishmentMembership.MemberRole.from(""))
                .isNull();

        ObjectMapper mapper = new ObjectMapper();
        PractitionerDtos.MembershipRequest dto = mapper.readValue("""
                {
                  "establishmentId": 1,
                  "memberRole": "collaborateur",
                  "validFrom": "2026-09-12T08:00:00Z",
                  "validUntil": "19/09/2026"
                }
                """, PractitionerDtos.MembershipRequest.class);

        assertThat(dto.establishmentUserId()).isEqualTo(1L);
        assertThat(dto.role()).isEqualTo(EstablishmentMembership.MemberRole.EMPLOYEE);
        assertThat(dto.validFrom()).isEqualTo(LocalDate.of(2026, 9, 12));
        assertThat(dto.validUntil()).isEqualTo(LocalDate.of(2026, 9, 19));

        PractitionerDtos.MembershipRequest defaultedRole = mapper.readValue("""
                {
                  "establishmentUserId": 4
                }
                """, PractitionerDtos.MembershipRequest.class);
        assertThat(defaultedRole.role()).isEqualTo(EstablishmentMembership.MemberRole.EMPLOYEE);
    }

    private static User user(Long id, ProfileType profileType) {
        User u = new User();
        u.setId(id);
        u.setEmail("user" + id + "@example.com");
        u.setProfileType(profileType);
        u.setStatus(AccountStatus.ACTIVE);
        return u;
    }
}
