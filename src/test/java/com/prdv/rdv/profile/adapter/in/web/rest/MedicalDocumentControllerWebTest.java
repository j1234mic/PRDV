package com.prdv.rdv.profile.adapter.in.web.rest;

import com.prdv.rdv.iam.adapter.in.security.JwtAuthenticationFilter;
import com.prdv.rdv.iam.adapter.in.security.RateLimitFilter;
import com.prdv.rdv.profile.application.command.DocumentCommands;
import com.prdv.rdv.profile.application.port.input.DocumentSharingUseCase;
import com.prdv.rdv.profile.application.port.input.MedicalDocumentQueryUseCase;
import com.prdv.rdv.profile.application.port.input.MedicalDocumentUseCase;
import com.prdv.rdv.profile.application.result.ProfileViews;
import com.prdv.rdv.profile.domain.model.document.MedicalDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration web du {@link MedicalDocumentController} : mapping REST,
 * multipart (JSON + fichier), validation des entrees, negociation de contenu
 * du telechargement et enforcement des autorites declarees.
 *
 * <p>Les cas d'usage sont mockes : le contrat HTTP est isole de la logique
 * metiere (couche web testee seule). La securite methodique
 * ({@code @PreAuthorize}) est active via {@code @EnableMethodSecurity}.
 */
@WebMvcTest(
        controllers = MedicalDocumentController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthenticationFilter.class, RateLimitFilter.class}))
class MedicalDocumentControllerWebTest {

    private static final String READ = "profile.document.read";
    private static final String WRITE = "profile.document.write";
    private static final String SHARE = "profile.document.share";

    /** Chaine ouverte : seules les autorites {@code @PreAuthorize} font foi ici. */
    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityTestConfig {

        @Bean
        SecurityFilterChain openSecurityFilterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MedicalDocumentUseCase documentUseCase;
    @MockBean
    private MedicalDocumentQueryUseCase queryUseCase;
    @MockBean
    private DocumentSharingUseCase sharingUseCase;

    // ------------------------------------------------------------------
    // Lectures
    // ------------------------------------------------------------------

    @Test
    @DisplayName("GET /documents liste mes documents avec le filtre de categorie")
    void listsMyDocuments() throws Exception {
        when(queryUseCase.myDocuments(MedicalDocument.DocumentCategory.LAB_RESULT))
                .thenReturn(List.of(documentView()));

        mockMvc.perform(get("/api/v1/documents")
                        .param("category", "LAB_RESULT")
                        .with(user("42").authorities(READ)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("LAB_RESULT"));

        verify(queryUseCase).myDocuments(MedicalDocument.DocumentCategory.LAB_RESULT);
    }

    @Test
    @DisplayName("Une categorie inconnue repond 400 VALIDATION_ERROR")
    void rejectsUnknownCategory() throws Exception {
        mockMvc.perform(get("/api/v1/documents")
                        .param("category", "INCONNU")
                        .with(user("42").authorities(READ)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(queryUseCase, never()).myDocuments(any());
    }

    @Test
    @DisplayName("GET /documents/{id} restitue la vue du document")
    void returnsDocumentView() throws Exception {
        when(queryUseCase.document(99L)).thenReturn(documentView());

        mockMvc.perform(get("/api/v1/documents/99")
                        .with(user("42").authorities(READ)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(99))
                .andExpect(jsonPath("$.currentVersion").value(1));
    }

    // ------------------------------------------------------------------
    // Televersement
    // ------------------------------------------------------------------

    @Test
    @DisplayName("POST /documents accepte le multipart JSON + fichier")
    void uploadsDocument() throws Exception {
        when(documentUseCase.upload(any())).thenReturn(documentView());
        byte[] fileContent = "pdf".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "analyses.pdf",
                MediaType.APPLICATION_PDF_VALUE, fileContent);
        MockMultipartFile metadata = new MockMultipartFile("document", "",
                MediaType.APPLICATION_JSON_VALUE,
                "{\"title\":\"Analyses\",\"category\":\"LAB_RESULT\"}"
                        .getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/documents").file(file).file(metadata)
                        .with(user("42").authorities(WRITE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Analyses"));

        ArgumentCaptor<DocumentCommands.UploadDocument> command =
                ArgumentCaptor.forClass(DocumentCommands.UploadDocument.class);
        verify(documentUseCase).upload(command.capture());
        assertThat(command.getValue().title()).isEqualTo("Analyses");
        assertThat(command.getValue().category())
                .isEqualTo(MedicalDocument.DocumentCategory.LAB_RESULT);
        assertThat(command.getValue().originalFilename()).isEqualTo("analyses.pdf");
        assertThat(command.getValue().content()).isEqualTo(fileContent);
    }

    @Test
    @DisplayName("Un titre absent est rejete par la validation (400)")
    void rejectsBlankTitle() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "analyses.pdf",
                MediaType.APPLICATION_PDF_VALUE, new byte[]{1});
        MockMultipartFile metadata = new MockMultipartFile("document", "",
                MediaType.APPLICATION_JSON_VALUE, "{\"title\":\"\"}".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/documents").file(file).file(metadata)
                        .with(user("42").authorities(WRITE)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(documentUseCase, never()).upload(any());
    }

    @Test
    @DisplayName("Une partie obligatoire manquante repond 400, pas 500")
    void rejectsMissingFilePart() throws Exception {
        MockMultipartFile metadata = new MockMultipartFile("document", "",
                MediaType.APPLICATION_JSON_VALUE,
                "{\"title\":\"Analyses\"}".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/documents").file(metadata)
                        .with(user("42").authorities(WRITE)))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // Telechargement
    // ------------------------------------------------------------------

    @Test
    @DisplayName("GET /download renvoie les octets avec un Content-Disposition UTF-8")
    void downloadsWithContentDisposition() throws Exception {
        byte[] content = "contenu pdf".getBytes(StandardCharsets.UTF_8);
        when(queryUseCase.download(99L)).thenReturn(new ProfileViews.DocumentFile(
                "analyses.pdf", "application/pdf", content.length, content));

        mockMvc.perform(get("/api/v1/documents/99/download")
                        .with(user("42").authorities(READ)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(content));
    }

    // ------------------------------------------------------------------
    // Autorites
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Sans autorite de lecture, la lecture est refusee (403)")
    void deniesReadWithoutAuthority() throws Exception {
        mockMvc.perform(get("/api/v1/documents")
                        .with(user("42").authorities(SHARE)))
                .andExpect(status().isForbidden());

        verify(queryUseCase, never()).myDocuments(any());
    }

    @Test
    @DisplayName("Sans autorite de partage, le partage est refuse (403)")
    void deniesShareWithoutAuthority() throws Exception {
        mockMvc.perform(post("/api/v1/documents/99/shares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"granteeUserId\":7,\"permission\":\"VIEW\"}")
                        .with(user("42").authorities(READ)))
                .andExpect(status().isForbidden());

        verify(sharingUseCase, never()).share(any());
    }

    @Test
    @DisplayName("POST /shares transmet la demande de partage")
    void sharesDocument() throws Exception {
        when(sharingUseCase.share(any())).thenReturn(new ProfileViews.DocumentShareView(
                "share-1", 7L, MedicalDocument.SharePermission.VIEW_AND_DOWNLOAD, "Avis",
                Instant.parse("2026-09-28T10:00:00Z"), null, null, true));

        mockMvc.perform(post("/api/v1/documents/99/shares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"granteeUserId\":7,\"permission\":\"VIEW_AND_DOWNLOAD\""
                                + ",\"reason\":\"Avis\"}")
                        .with(user("42").authorities(SHARE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("share-1"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("POST /archive repond 204 sans contenu")
    void archivesWithNoContent() throws Exception {
        mockMvc.perform(post("/api/v1/documents/99/archive")
                        .with(user("42").authorities(WRITE)))
                .andExpect(status().isNoContent());

        verify(documentUseCase).archive(99L);
    }

    @Test
    @DisplayName("PUT /category requalifie le document")
    void reclassifiesDocument() throws Exception {
        when(documentUseCase.reclassify(any())).thenReturn(documentView());

        mockMvc.perform(put("/api/v1/documents/99/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"LAB_RESULT\"}")
                        .with(user("42").authorities(WRITE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("LAB_RESULT"));

        ArgumentCaptor<DocumentCommands.Reclassify> command =
                ArgumentCaptor.forClass(DocumentCommands.Reclassify.class);
        verify(documentUseCase).reclassify(command.capture());
        assertThat(command.getValue().category())
                .isEqualTo(MedicalDocument.DocumentCategory.LAB_RESULT);
    }

    // ------------------------------------------------------------------

    private static ProfileViews.MedicalDocumentView documentView() {
        return new ProfileViews.MedicalDocumentView(99L, 42L, "Analyses",
                MedicalDocument.DocumentCategory.LAB_RESULT,
                MedicalDocument.DocumentStatus.CLASSIFIED,
                MedicalDocument.ClassificationSource.USER_DEFINED, 1.0, null, null, null, 1,
                "analyses.pdf", "application/pdf", 10, null, null,
                List.of(), List.of(),
                Instant.parse("2026-09-28T10:00:00Z"), Instant.parse("2026-09-28T10:00:00Z"));
    }
}
