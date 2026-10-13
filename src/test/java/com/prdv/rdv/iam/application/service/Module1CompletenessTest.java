package com.prdv.rdv.iam.application.service;

import com.prdv.rdv.iam.adapter.in.security.WafDdosProtectionRegistry;
import com.prdv.rdv.iam.adapter.out.external.StubInsuranceVerificationAdapter;
import com.prdv.rdv.iam.adapter.out.security.AesGcmCipherAdapter;
import com.prdv.rdv.iam.adapter.out.security.BiometricSignatureVerifierAdapter;
import com.prdv.rdv.iam.application.command.AdminCommands;
import com.prdv.rdv.iam.application.command.AuthCommands;
import com.prdv.rdv.iam.application.command.ProfileCommands;
import com.prdv.rdv.iam.application.command.RegistrationCommands;
import com.prdv.rdv.iam.application.command.RequestMetadata;
import com.prdv.rdv.iam.application.command.SecurityCommands;
import com.prdv.rdv.iam.application.port.output.BiometricCredentialRepository;
import com.prdv.rdv.iam.application.port.output.BugBountyReportRepository;
import com.prdv.rdv.iam.application.port.output.DomainEventPublisher;
import com.prdv.rdv.iam.application.port.output.E2eeKeyRepository;
import com.prdv.rdv.iam.application.port.output.EstablishmentProfileRepository;
import com.prdv.rdv.iam.application.port.output.GroupInvoiceRepository;
import com.prdv.rdv.iam.application.port.output.JwtTokenPort;
import com.prdv.rdv.iam.application.port.output.KycDocumentRepository;
import com.prdv.rdv.iam.application.port.output.MembershipRepository;
import com.prdv.rdv.iam.application.port.output.PasswordHasher;
import com.prdv.rdv.iam.application.port.output.PatientProfileRepository;
import com.prdv.rdv.iam.application.port.output.PentestReportRepository;
import com.prdv.rdv.iam.application.port.output.RefreshTokenRepository;
import com.prdv.rdv.iam.application.port.output.RoleRepository;
import com.prdv.rdv.iam.application.port.output.SecretaryProfileRepository;
import com.prdv.rdv.iam.application.port.output.SecurityContextPort;
import com.prdv.rdv.iam.application.port.output.TokenizationPort;
import com.prdv.rdv.iam.application.port.output.UserRepository;
import com.prdv.rdv.iam.application.port.output.UserSessionRepository;
import com.prdv.rdv.iam.application.result.AuthResults;
import com.prdv.rdv.iam.application.result.Views;
import com.prdv.rdv.iam.application.service.support.AuditLogger;
import com.prdv.rdv.iam.application.service.support.RegistrationSupport;
import com.prdv.rdv.iam.application.service.support.TokenIssuanceSupport;
import com.prdv.rdv.iam.application.service.support.TransactionalStateSaver;
import com.prdv.rdv.iam.application.service.support.ViewMapper;
import com.prdv.rdv.iam.config.IamProperties;
import com.prdv.rdv.iam.domain.model.auth.BiometricCredential;
import com.prdv.rdv.iam.domain.model.rbac.Role;
import com.prdv.rdv.iam.domain.model.security.BugBountyReport;
import com.prdv.rdv.iam.domain.model.security.E2eeUserKey;
import com.prdv.rdv.iam.domain.model.security.PentestReport;
import com.prdv.rdv.iam.domain.model.user.AccountStatus;
import com.prdv.rdv.iam.domain.model.user.AdminType;
import com.prdv.rdv.iam.domain.model.user.EstablishmentProfile;
import com.prdv.rdv.iam.domain.model.user.Email;
import com.prdv.rdv.iam.domain.model.user.PatientProfile;
import com.prdv.rdv.iam.domain.model.user.ProfileType;
import com.prdv.rdv.iam.domain.model.user.User;
import com.prdv.rdv.iam.domain.model.verification.EstablishmentMembership;
import com.prdv.rdv.iam.domain.model.verification.GroupInvoice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires couvrant les fonctionnalites completees du Module 1 :
 * profils mineurs, assurance RC Pro, facturation groupee et dashboard consolide
 * d'etablissement, 5 types d'administrateurs systeme, biometrie (Touch ID / Face ID)
 * et securite avancee (E2EE medical, WAF/DDoS, Pentest, Bug Bounty).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class Module1CompletenessTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-10-13T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private RegistrationSupport registrationSupport;
    @Mock
    private PatientProfileRepository patientProfileRepository;
    @Mock
    private KycDocumentRepository kycDocumentRepository;
    @Mock
    private TokenizationPort tokenization;
    @Mock
    private SecurityContextPort securityContext;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordHasher passwordHasher;
    @Mock
    private UserSessionRepository sessionRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private EstablishmentProfileRepository establishmentProfileRepository;
    @Mock
    private MembershipRepository membershipRepository;
    @Mock
    private GroupInvoiceRepository groupInvoiceRepository;
    @Mock
    private SecretaryProfileRepository secretaryProfileRepository;
    @Mock
    private BiometricCredentialRepository biometricCredentialRepository;
    @Mock
    private JwtTokenPort jwtTokenPort;
    @Mock
    private TokenIssuanceSupport tokenIssuanceSupport;
    @Mock
    private E2eeKeyRepository e2eeKeyRepository;
    @Mock
    private PentestReportRepository pentestReportRepository;
    @Mock
    private BugBountyReportRepository bugBountyReportRepository;
    @Mock
    private TransactionalStateSaver stateSaver;
    @Mock
    private DomainEventPublisher eventPublisher;

    private ViewMapper viewMapper;
    private AuditLogger auditLogger;

    @BeforeEach
    void setUp() {
        viewMapper = new ViewMapper();
        auditLogger = new AuditLogger(stateSaver, FIXED_CLOCK);
    }

    @Test
    @DisplayName("1.1 Patient : creation d'un profil mineur rattache au parent/tuteur")
    void registersMinorLinkedToGuardian() {
        Long guardianId = 10L;
        User guardian = new User();
        guardian.setId(guardianId);
        guardian.setEmail("parent@prdv.app");
        guardian.setProfileType(ProfileType.PATIENT);
        guardian.setStatus(AccountStatus.ACTIVE);

        when(securityContext.requireCurrentUserId()).thenReturn(guardianId);
        when(userRepository.findById(guardianId)).thenReturn(Optional.of(guardian));
        when(registrationSupport.createAccount(any(), any(), any(), any(), any())).thenAnswer(inv ->
                User.register(Email.of(inv.getArgument(0)), null, "$2a$hash", ProfileType.PATIENT, FIXED_CLOCK));
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(11L);
            return u;
        });
        when(patientProfileRepository.save(any())).thenAnswer(inv -> {
            PatientProfile p = inv.getArgument(0);
            p.setId(100L);
            return p;
        });

        PatientRegistrationService service = new PatientRegistrationService(
                registrationSupport, userRepository, patientProfileRepository, null,
                securityContext, viewMapper, eventPublisher, FIXED_CLOCK);

        Views.PatientView minorView = service.registerMinor(new RegistrationCommands.RegisterMinorPatient(
                null, null, null, "Leo", "Martin", LocalDate.of(2016, 4, 15), PatientProfile.Gender.M));

        assertThat(minorView.minor()).isTrue();
        assertThat(minorView.guardianUserId()).isEqualTo(guardianId);
        assertThat(minorView.firstName()).isEqualTo("Leo");
    }

    @Test
    @DisplayName("1.1 Praticien : verification automatique de l'assurance RC Pro")
    void verifiesProfessionalInsurance() {
        StubInsuranceVerificationAdapter adapter = new StubInsuranceVerificationAdapter(FIXED_CLOCK);
        var valid = adapter.verify("MACSF", "RC-2026-998877", LocalDate.of(2027, 12, 31), "10001234567");
        assertThat(valid.valid()).isTrue();
        assertThat(valid.insuranceCompany()).isEqualTo("MACSF");

        var expired = adapter.verify("MACSF", "RC-2020-112233", LocalDate.of(2025, 1, 1), "10001234567");
        assertThat(expired.valid()).isFalse();
    }

    @Test
    @DisplayName("1.1 Etablissement : facturation groupee et tableau de bord consolide")
    void issuesGroupInvoiceAndComputesConsolidatedDashboard() {
        Long estUserId = 20L;
        EstablishmentProfile est = EstablishmentProfile.create(
                estUserId, "Clinique Saint-Louis", "81234567800012",
                "12 avenue de la Republique, Paris", List.of("Cardiologie", "Pediatrie"), FIXED_CLOCK);
        est.setId(1L);

        User estUser = User.register(Email.of("clinique@prdv.app"), null, "$2a$hash", ProfileType.ESTABLISHMENT, FIXED_CLOCK);
        estUser.setId(estUserId);
        when(securityContext.requireCurrentUserId()).thenReturn(estUserId);
        when(userRepository.findById(estUserId)).thenReturn(Optional.of(estUser));
        when(establishmentProfileRepository.findByUserId(estUserId)).thenReturn(Optional.of(est));
        when(groupInvoiceRepository.save(any())).thenAnswer(inv -> {
            GroupInvoice gi = inv.getArgument(0);
            if (gi.getId() == null) {
                gi.setId(500L);
            }
            return gi;
        });

        EstablishmentRegistrationService service = new EstablishmentRegistrationService(
                registrationSupport, userRepository, establishmentProfileRepository, membershipRepository,
                groupInvoiceRepository, secretaryProfileRepository, securityContext,
                viewMapper, auditLogger, eventPublisher, FIXED_CLOCK);

        Views.GroupInvoiceView invoice = service.issueGroupInvoice(new ProfileCommands.IssueGroupInvoice(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                "Cardiologie", Set.of(30L, 31L), 120, 600_000L, 5));

        assertThat(invoice.totalAmountCents()).isEqualTo(600_000L);
        assertThat(invoice.platformFeeCents()).isEqualTo(30_000L);
        assertThat(invoice.netAmountCents()).isEqualTo(570_000L);
        assertThat(invoice.status()).isEqualTo(GroupInvoice.InvoiceStatus.ISSUED);

        EstablishmentMembership approvedMember = EstablishmentMembership.request(
                estUserId, 30L, EstablishmentMembership.MemberRole.EMPLOYEE, null, null, FIXED_CLOCK);
        approvedMember.accept(FIXED_CLOCK);
        when(membershipRepository.findByEstablishmentUserId(estUserId)).thenReturn(List.of(approvedMember));
        when(secretaryProfileRepository.findByAccessibleEstablishmentId(estUserId)).thenReturn(List.of());
        when(groupInvoiceRepository.findByEstablishmentUserId(estUserId)).thenReturn(List.of());

        Views.EstablishmentDashboardView dashboard = service.consolidatedDashboard();
        assertThat(dashboard.activePractitionersCount()).isEqualTo(1);
        assertThat(dashboard.departmentsCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("1.1 Administrateurs systeme : creation des 5 sous-profils d'administrateurs")
    void createsAllFiveSystemAdministratorTypes() {
        when(securityContext.currentUserId()).thenReturn(Optional.of(1L));
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordHasher.hash(any())).thenReturn("$2a$hash");
        for (String roleName : List.of(Role.SUPER_ADMIN, Role.MODERATOR,
                Role.TECHNICAL_SUPPORT, Role.COMMERCIAL_SUPPORT, Role.DATA_ANALYST)) {
            when(roleRepository.findByName(roleName))
                    .thenReturn(Optional.of(Role.system(roleName, "System " + roleName, Set.of())));
        }
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(99L);
            return u;
        });

        UserAdministrationService service = new UserAdministrationService(
                userRepository, roleRepository, passwordHasher, sessionRepository,
                refreshTokenRepository, securityContext, viewMapper, auditLogger, FIXED_CLOCK);

        for (AdminType type : AdminType.values()) {
            Views.UserView created = service.createAdministrator(new AdminCommands.CreateAdministrator(
                    type.name().toLowerCase() + "@prdv.app", null, "StrongPass#2026", type));
            assertThat(created.profileType()).isEqualTo(ProfileType.ADMIN);
            assertThat(created.adminType()).isEqualTo(type);
        }
    }

    @Test
    @DisplayName("1.2 Biometrie : enrolement, defi et authentification Face ID / Touch ID")
    void enrollsAndAuthenticatesWithBiometricCredential() throws Exception {
        Long userId = 42L;
        User user = new User();
        user.setId(userId);
        user.setEmail("patient@prdv.app");
        user.setProfileType(ProfileType.PATIENT);
        user.setStatus(AccountStatus.ACTIVE);
        user.setEmailVerified(true);

        when(securityContext.requireCurrentUserId()).thenReturn(userId);
        when(biometricCredentialRepository.findByCredentialId("cred-touchid-42"))
                .thenReturn(Optional.empty());
        when(biometricCredentialRepository.save(any())).thenAnswer(inv -> {
            BiometricCredential c = inv.getArgument(0);
            if (c.getId() == null) {
                c.setId(7L);
            }
            return c;
        });

        BiometricAuthenticationService bioService = new BiometricAuthenticationService(
                biometricCredentialRepository, new BiometricSignatureVerifierAdapter(),
                userRepository, jwtTokenPort, securityContext, tokenIssuanceSupport,
                viewMapper, auditLogger, eventPublisher, FIXED_CLOCK);

        Views.BiometricCredentialView enrolled = bioService.enroll(new AuthCommands.EnrollBiometric(
                "cred-touchid-42", "pub-key-secret-42",
                BiometricCredential.BiometricModality.FINGERPRINT, "iPhone 16 Pro"));
        assertThat(enrolled.modality()).isEqualTo(BiometricCredential.BiometricModality.FINGERPRINT);

        BiometricCredential stored = BiometricCredential.enroll(
                userId, "cred-touchid-42", "pub-key-secret-42",
                BiometricCredential.BiometricModality.FINGERPRINT, "iPhone 16 Pro", FIXED_CLOCK);
        stored.setId(7L);
        when(biometricCredentialRepository.findByCredentialId("cred-touchid-42"))
                .thenReturn(Optional.of(stored));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtTokenPort.createChallengeToken(userId, "BIOMETRIC")).thenReturn("bio-challenge-jwt");
        when(jwtTokenPort.parseChallengeToken("bio-challenge-jwt", "BIOMETRIC"))
                .thenReturn(new JwtTokenPort.ChallengeClaims(
                        userId, "BIOMETRIC", FIXED_CLOCK.instant().plusSeconds(300)));
        when(tokenIssuanceSupport.completeLogin(any(), any(), any(Boolean.class)))
                .thenReturn(AuthResults.TokenSet.bearer("access-jwt", "refresh-opaque", 900));

        AuthResults.Challenge challenge = bioService.issueChallenge(
                new AuthCommands.RequestBiometricChallenge(null, "cred-touchid-42"));
        assertThat(challenge.challengeToken()).isEqualTo("bio-challenge-jwt");

        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        String sigHex = HexFormat.of().formatHex(sha256.digest(
                "pub-key-secret-42:bio-challenge-jwt:cred-touchid-42".getBytes(StandardCharsets.UTF_8)));

        AuthResults.AuthResult result = bioService.authenticate(new AuthCommands.VerifyBiometric(
                "bio-challenge-jwt", "cred-touchid-42", sigHex, 1L, RequestMetadata.unknown()));
        assertThat(result.isAuthenticated()).isTrue();
    }

    @Test
    @DisplayName("1.2 Securite Avancee : E2EE medical, WAF/DDoS, Pentest et Bug Bounty")
    void sealsAndOpensE2eeMedicalDataAndRunsPentestAndBugBounty() {
        IamProperties props = new IamProperties();
        props.getSecurity().getJwt().setSecret("0123456789abcdef0123456789abcdef0123456789abcdef");
        props.getSecurity().getCipher().setSecret("master-aes-secret-2026-prdv");

        AesGcmCipherAdapter cipher = new AesGcmCipherAdapter(props);
        WafDdosProtectionRegistry wafRegistry = new WafDdosProtectionRegistry(FIXED_CLOCK);

        when(securityContext.requireCurrentUserId()).thenReturn(2L);
        when(securityContext.currentUserId()).thenReturn(Optional.of(2L));
        when(passwordHasher.digest(any())).thenReturn("abcdef0123456789abcdef0123456789abcdef0123456789");
        when(e2eeKeyRepository.findByUserId(2L)).thenReturn(List.of());
        when(e2eeKeyRepository.save(any())).thenAnswer(inv -> {
            E2eeUserKey k = inv.getArgument(0);
            k.setId(1L);
            return k;
        });

        AdvancedSecurityService securityService = new AdvancedSecurityService(
                e2eeKeyRepository, pentestReportRepository, bugBountyReportRepository,
                wafRegistry, cipher, passwordHasher, userRepository, securityContext,
                props, viewMapper, auditLogger, eventPublisher, FIXED_CLOCK);

        Views.E2eePublicKeyView keyView = securityService.registerE2eeKey(
                new SecurityCommands.RegisterE2eeKey("key-doc-2", "ECDH-P256-AES256GCM", "-----BEGIN PUBLIC KEY-----MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A-----END PUBLIC KEY-----"));
        assertThat(keyView.active()).isTrue();

        E2eeUserKey activeKey = E2eeUserKey.register(
                2L, "key-doc-2", "ECDH-P256-AES256GCM",
                "-----BEGIN PUBLIC KEY-----MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A-----END PUBLIC KEY-----",
                "sha256:abcdef", FIXED_CLOCK);
        when(e2eeKeyRepository.findActiveByUserId(2L)).thenReturn(Optional.of(activeKey));

        Views.E2eeEnvelopeView envelope = securityService.sealMedicalData(
                new SecurityCommands.SealMedicalPayload(2L, "Compte-rendu cardiologique confidentiel : RAS"));
        String decrypted = securityService.openMedicalData(
                new SecurityCommands.OpenMedicalPayload(envelope.cipherText()));
        assertThat(decrypted).isEqualTo("Compte-rendu cardiologique confidentiel : RAS");

        when(userRepository.findAll(0, 200)).thenReturn(List.of());
        when(pentestReportRepository.save(any())).thenAnswer(inv -> {
            PentestReport r = inv.getArgument(0);
            r.setId(10L);
            return r;
        });
        Views.PentestReportView pentest = securityService.runAutomatedSecuritySelfCheck();
        assertThat(pentest.securityScore()).isEqualTo(100);

        when(bugBountyReportRepository.save(any())).thenAnswer(inv -> {
            BugBountyReport b = inv.getArgument(0);
            if (b.getId() == null) {
                b.setId(20L);
            }
            return b;
        });
        Views.BugBountyReportView bounty = securityService.submitBugBountyReport(
                new SecurityCommands.SubmitBugBountyReport(
                        "researcher@sec.org", "sec_hunter",
                        "IDOR potentiel sur previsualisation documentaire",
                        "Description detaillee PoC...", "profile-documents",
                        BugBountyReport.Severity.HIGH, 7.5));
        assertThat(bounty.status()).isEqualTo(BugBountyReport.Status.SUBMITTED);

        wafRegistry.blockIpTemporarily("203.0.113.99", Duration.ofMinutes(10));
        assertThat(securityService.wafDdosStatus().blockedIps()).contains("203.0.113.99");
        securityService.unblockIpAddress("203.0.113.99");
        assertThat(securityService.wafDdosStatus().blockedIps()).doesNotContain("203.0.113.99");
    }
}
