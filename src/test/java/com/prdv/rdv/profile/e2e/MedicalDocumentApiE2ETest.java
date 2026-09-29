package com.prdv.rdv.profile.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.prdv.rdv.iam.application.port.output.JwtTokenPort;
import com.prdv.rdv.iam.application.port.output.RoleRepository;
import com.prdv.rdv.iam.application.port.output.UserRepository;
import com.prdv.rdv.iam.domain.model.rbac.Role;
import com.prdv.rdv.iam.domain.model.user.AccountStatus;
import com.prdv.rdv.iam.domain.model.user.Email;
import com.prdv.rdv.iam.domain.model.user.ProfileType;
import com.prdv.rdv.iam.domain.model.user.User;
import com.prdv.rdv.profile.application.port.output.PatientIdentityRepository;
import com.prdv.rdv.profile.application.port.output.PrivacyPreferencesRepository;
import com.prdv.rdv.profile.domain.model.identity.PatientIdentity;
import com.prdv.rdv.profile.domain.model.preference.PrivacyPreferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de bout en bout de l'API « documents medicaux » (module 2.1).
 *
 * <p>Le contexte Spring complet est demarre sur un port aleatoire : les
 * requetes HTTP passent par la vraie chaine de securite (filtre JWT, RBAC),
 * les vrais cas d'usage, la vraie persistance (H2) et les vrais adaptateurs
 * (stockage local, OCR, classification, passerelle DMP simulee).
 *
 * <p>Les comptes sont crees directement dans le referentiel utilisateurs et
 * recoivent de vrais jetons signes par {@link JwtTokenPort} : le flux
 * d'inscription OTP du module IAM est deja couvert par ses propres tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "prdv.profile.documents.max-bytes=2048")
class MedicalDocumentApiE2ETest {

    private static final AtomicLong SEQUENCE = new AtomicLong();

    @Autowired
    private TestRestTemplate rest;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private JwtTokenPort jwtTokenPort;
    @Autowired
    private PatientIdentityRepository identityRepository;
    @Autowired
    private PrivacyPreferencesRepository preferencesRepository;
    @Autowired
    private Clock clock;

    private String patientToken;
    private String practitionerToken;
    private Long patientId;
    private Long practitionerId;

    @BeforeEach
    void setUp() {
        long seq = SEQUENCE.incrementAndGet();
        User patient = registerUser("patient" + seq + "@prdv.test", Role.PATIENT,
                ProfileType.PATIENT);
        User practitioner = registerUser("praticien" + seq + "@prdv.test", Role.PRACTITIONER,
                ProfileType.PRACTITIONER);
        patientId = patient.getId();
        practitionerId = practitioner.getId();
        patientToken = tokenFor(patient);
        practitionerToken = tokenFor(practitioner);
    }

    // ------------------------------------------------------------------
    // Parcours complet
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Parcours : depot auto-classe, versions, partage fin, DMP, archivage")
    void completeDocumentJourney() {
        // 1. Televersement sans categorie : OCR + classification automatique
        ResponseEntity<JsonNode> upload = uploadDocument(patientToken,
                "{\"title\":\"Analyses du 12/09\"}", "analyses.txt", "text/plain",
                "Resultats du laboratoire : glycemie 1,08 - NFS".getBytes(StandardCharsets.UTF_8));

        assertThat(upload.getStatusCode().value()).isEqualTo(200);
        JsonNode document = upload.getBody();
        long documentId = document.get("id").asLong();
        assertThat(document.get("category").asText()).isEqualTo("LAB_RESULT");
        assertThat(document.get("status").asText()).isEqualTo("CLASSIFIED");
        assertThat(document.get("classificationSource").asText()).isEqualTo("AUTOMATIC_AI");
        assertThat(document.get("ocr").get("status").asText()).isEqualTo("SUCCEEDED");
        assertThat(document.get("currentVersion").asInt()).isEqualTo(1);

        // 2. Consultation et filtre par categorie
        JsonNode listed = exchangeJson(HttpMethod.GET, patientToken, "/api/v1/documents", null).getBody();
        assertThat(jsonIds(listed)).contains(documentId);
        JsonNode filtered = exchangeJson(HttpMethod.GET, patientToken,
                "/api/v1/documents?category=PRESCRIPTION", null).getBody();
        assertThat(jsonIds(filtered)).doesNotContain(documentId);

        // 3. Requalification manuelle (correction de l'IA)
        ResponseEntity<JsonNode> requalified = exchangeJson(HttpMethod.PUT, patientToken,
                "/api/v1/documents/" + documentId + "/category", "{\"category\":\"PRESCRIPTION\"}");
        assertThat(requalified.getStatusCode().value()).isEqualTo(200);
        assertThat(requalified.getBody().get("category").asText()).isEqualTo("PRESCRIPTION");
        assertThat(requalified.getBody().get("classificationSource").asText())
                .isEqualTo("USER_DEFINED");

        // 4. Nouvelle version : la precedente reste consultable
        ResponseEntity<JsonNode> version2 = uploadVersion(patientToken, documentId,
                "{\"changeNote\":\"Complement du laboratoire\"}", "analyses-v2.txt", "text/plain",
                "NFS complete".getBytes(StandardCharsets.UTF_8));
        assertThat(version2.getStatusCode().value()).isEqualTo(200);
        assertThat(version2.getBody().get("currentVersion").asInt()).isEqualTo(2);
        assertThat(version2.getBody().get("versions").size()).isEqualTo(2);

        // 5. Partage VIEW au praticien : lecture seule, pas de telechargement
        ResponseEntity<JsonNode> shareView = exchangeJson(HttpMethod.POST, patientToken,
                "/api/v1/documents/" + documentId + "/shares",
                "{\"granteeUserId\":" + practitionerId + ",\"permission\":\"VIEW\","
                        + "\"reason\":\"Avis specialise\"}");
        assertThat(shareView.getStatusCode().value()).isEqualTo(200);
        assertThat(shareView.getBody().get("active").asBoolean()).isTrue();
        String shareId = shareView.getBody().get("id").asText();

        JsonNode sharedWithMe = exchangeJson(HttpMethod.GET, practitionerToken,
                "/api/v1/documents/shared-with-me", null).getBody();
        assertThat(jsonIds(sharedWithMe)).contains(documentId);

        JsonNode readable = exchangeJson(HttpMethod.GET, practitionerToken,
                "/api/v1/documents/" + documentId, null).getBody();
        assertThat(readable.get("id").asLong()).isEqualTo(documentId);

        ResponseEntity<JsonNode> downloadDenied = exchange(HttpMethod.GET, practitionerToken,
                "/api/v1/documents/" + documentId + "/download", null, JsonNode.class);
        assertThat(downloadDenied.getStatusCode().value()).isEqualTo(403);

        // 6. Revocation tracée puis nouveau partage avec droit de telechargement
        ResponseEntity<JsonNode> revoked = exchangeJson(HttpMethod.DELETE, patientToken,
                "/api/v1/documents/" + documentId + "/shares/" + shareId, null);
        assertThat(revoked.getBody().get("active").asBoolean()).isFalse();
        assertThat(revoked.getBody().get("revokedAt")).isNotNull();

        ResponseEntity<JsonNode> shareDownload = exchangeJson(HttpMethod.POST, patientToken,
                "/api/v1/documents/" + documentId + "/shares",
                "{\"granteeUserId\":" + practitionerId
                        + ",\"permission\":\"VIEW_AND_DOWNLOAD\"}");
        assertThat(shareDownload.getStatusCode().value()).isEqualTo(200);

        ResponseEntity<byte[]> downloaded = exchange(HttpMethod.GET, practitionerToken,
                "/api/v1/documents/" + documentId + "/download", null, byte[].class);
        assertThat(downloaded.getStatusCode().value()).isEqualTo(200);
        assertThat(downloaded.getBody())
                .isEqualTo("NFS complete".getBytes(StandardCharsets.UTF_8));
        assertThat(downloaded.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("attachment");

        // 7. DMP : refuse sans consentement, puis publie avec consentement + compte rattache
        ResponseEntity<JsonNode> dmpDenied = exchangeJson(HttpMethod.POST, patientToken,
                "/api/v1/documents/" + documentId + "/dmp", null);
        assertThat(dmpDenied.getStatusCode().value()).isEqualTo(403);
        assertThat(dmpDenied.getBody().get("code").asText()).isEqualTo("CONSENT_REQUIRED");

        grantDmpConsentAndLinkAccount(patientId);

        ResponseEntity<JsonNode> dmpPublished = exchangeJson(HttpMethod.POST, patientToken,
                "/api/v1/documents/" + documentId + "/dmp", null);
        assertThat(dmpPublished.getStatusCode().value()).isEqualTo(200);
        assertThat(dmpPublished.getBody().get("dmpReference").asText()).startsWith("DMPDOC-");

        // 8. Archivage : le document devient immutable
        ResponseEntity<JsonNode> archived = exchange(HttpMethod.POST, patientToken,
                "/api/v1/documents/" + documentId + "/archive", null, JsonNode.class);
        assertThat(archived.getStatusCode().value()).isEqualTo(204);

        ResponseEntity<JsonNode> versionRejected = uploadVersion(patientToken, documentId,
                "{}", "trop-tard.txt", "text/plain", "x".getBytes(StandardCharsets.UTF_8));
        assertThat(versionRejected.getStatusCode().value()).isEqualTo(400);
        assertThat(versionRejected.getBody().get("code").asText()).isEqualTo("DOCUMENT_IMMUTABLE");
    }

    // ------------------------------------------------------------------
    // Securite : anonyme, RBAC, isolation entre patients
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Securite : 401 sans jeton, 403 sans autorite, 403 sur document d'autrui")
    void securityMatrix() {
        JsonNode uploaded = uploadDocument(patientToken, "{\"title\":\"Ordonnance\"}",
                "ordonnance.txt", "text/plain", "ordonnance".getBytes(StandardCharsets.UTF_8))
                .getBody();
        long documentId = uploaded.get("id").asLong();

        // Anonyme
        ResponseEntity<JsonNode> anonymous = exchangeJson(HttpMethod.GET, null,
                "/api/v1/documents", null);
        assertThat(anonymous.getStatusCode().value()).isEqualTo(401);

        // Le praticien n'a pas profile.document.write ni profile.document.share
        ResponseEntity<JsonNode> writeDenied = uploadDocument(practitionerToken,
                "{\"title\":\"Injection\"}", "x.txt", "text/plain",
                "x".getBytes(StandardCharsets.UTF_8));
        assertThat(writeDenied.getStatusCode().value()).isEqualTo(403);

        ResponseEntity<JsonNode> shareDenied = exchangeJson(HttpMethod.POST,
                practitionerToken, "/api/v1/documents/" + documentId + "/shares",
                "{\"granteeUserId\":" + patientId + ",\"permission\":\"VIEW\"}");
        assertThat(shareDenied.getStatusCode().value()).isEqualTo(403);

        // Un autre patient ne voit pas ce document (isolation des donnees)
        User stranger = registerUser("etranger" + SEQUENCE.get() + "@prdv.test", Role.PATIENT,
                ProfileType.PATIENT);
        ResponseEntity<JsonNode> strangerRead = exchangeJson(HttpMethod.GET, tokenFor(stranger),
                "/api/v1/documents/" + documentId, null);
        assertThat(strangerRead.getStatusCode().value()).isEqualTo(403);
        assertThat(strangerRead.getBody().get("code").asText()).isEqualTo("ACCESS_DENIED");
    }

    // ------------------------------------------------------------------
    // Validation des entrees
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Validation : titre absent, format refuse, taille depassee, categorie inconnue")
    void inputValidation() {
        ResponseEntity<JsonNode> blankTitle = uploadDocument(patientToken, "{\"title\":\"\"}",
                "a.txt", "text/plain", "x".getBytes(StandardCharsets.UTF_8));
        assertThat(blankTitle.getStatusCode().value()).isEqualTo(400);
        assertThat(blankTitle.getBody().get("code").asText()).isEqualTo("VALIDATION_ERROR");

        ResponseEntity<JsonNode> badFormat = uploadDocument(patientToken, "{\"title\":\"Maliciel\"}",
                "virus.exe", "application/x-msdownload", "MZ".getBytes(StandardCharsets.UTF_8));
        assertThat(badFormat.getStatusCode().value()).isEqualTo(400);
        assertThat(badFormat.getBody().get("code").asText())
                .isEqualTo("DOCUMENT_FORMAT_UNSUPPORTED");

        byte[] tooBig = new byte[2_049];
        ResponseEntity<JsonNode> oversized = uploadDocument(patientToken, "{\"title\":\"Trop gros\"}",
                "gros.pdf", "application/pdf", tooBig);
        assertThat(oversized.getStatusCode().value()).isEqualTo(400);
        assertThat(oversized.getBody().get("code").asText()).isEqualTo("DOCUMENT_TOO_LARGE");

        ResponseEntity<JsonNode> badCategory = exchangeJson(HttpMethod.GET, patientToken,
                "/api/v1/documents?category=INCONNU", null);
        assertThat(badCategory.getStatusCode().value()).isEqualTo(400);
    }

    // ------------------------------------------------------------------
    // Fixtures et helpers HTTP
    // ------------------------------------------------------------------

    private User registerUser(String email, String roleName, ProfileType profileType) {
        Role role = roleRepository.findByName(roleName).orElseThrow();
        User user = User.register(Email.of(email), null, "test-password-hash", profileType, clock);
        user.markEmailVerified();
        user.setStatus(AccountStatus.ACTIVE);
        user.getRoles().add(role);
        return userRepository.save(user);
    }

    private String tokenFor(User user) {
        return jwtTokenPort.createAccessToken(user.getId(), user.getEmail(), List.of(),
                user.getTokenVersion());
    }

    private void grantDmpConsentAndLinkAccount(Long userId) {
        PrivacyPreferences preferences = preferencesRepository.findByUserId(userId)
                .orElseGet(() -> PrivacyPreferences.defaults(userId, clock));
        preferences.recordConsent(PrivacyPreferences.ConsentPurpose.DMP_SHARING, true, "2026-09",
                "127.0.0.1", clock);
        preferencesRepository.save(preferences);

        PatientIdentity identity = identityRepository.findByUserId(userId)
                .orElseGet(() -> PatientIdentity.create(userId, null, clock));
        identity.linkDmp("DMP-E2E-" + userId, true, clock);
        identityRepository.save(identity);
    }

    private ResponseEntity<JsonNode> uploadDocument(String token, String metadataJson,
                                                    String filename, String contentType,
                                                    byte[] content) {
        HttpHeaders headers = authHeaders(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        HttpHeaders metadataHeaders = new HttpHeaders();
        metadataHeaders.setContentType(MediaType.APPLICATION_JSON);
        body.add("document", new HttpEntity<>(metadataJson, metadataHeaders));

        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.parseMediaType(contentType));
        body.add("file", new HttpEntity<>(namedResource(content, filename), fileHeaders));

        return rest.exchange("/api/v1/documents", HttpMethod.POST,
                new HttpEntity<>(body, headers), JsonNode.class);
    }

    private ResponseEntity<JsonNode> uploadVersion(String token, long documentId,
                                                   String metadataJson, String filename,
                                                   String contentType, byte[] content) {
        HttpHeaders headers = authHeaders(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        HttpHeaders metadataHeaders = new HttpHeaders();
        metadataHeaders.setContentType(MediaType.APPLICATION_JSON);
        body.add("version", new HttpEntity<>(metadataJson, metadataHeaders));

        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.parseMediaType(contentType));
        body.add("file", new HttpEntity<>(namedResource(content, filename), fileHeaders));

        return rest.exchange("/api/v1/documents/" + documentId + "/versions", HttpMethod.POST,
                new HttpEntity<>(body, headers), JsonNode.class);
    }

    private static List<Long> jsonIds(JsonNode array) {
        List<Long> ids = new ArrayList<>();
        if (array != null && array.isArray()) {
            array.forEach(node -> ids.add(node.get("id").asLong()));
        }
        return ids;
    }

    private ResponseEntity<JsonNode> exchangeJson(HttpMethod method, String token, String path,
                                                  String body) {
        return exchange(method, token, path, body, JsonNode.class);
    }

    private <T> ResponseEntity<T> exchange(HttpMethod method, String token, String path,
                                           String body, Class<T> responseType) {
        HttpHeaders headers = authHeaders(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(path, method, new HttpEntity<>(body, headers), responseType);
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return headers;
    }

    private static ByteArrayResource namedResource(byte[] content, String filename) {
        return new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }
}
