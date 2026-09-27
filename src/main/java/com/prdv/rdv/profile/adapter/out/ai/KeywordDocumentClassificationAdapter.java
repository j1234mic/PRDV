package com.prdv.rdv.profile.adapter.out.ai;

import com.prdv.rdv.profile.application.port.output.DocumentClassificationPort;
import com.prdv.rdv.profile.domain.model.document.MedicalDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Classification automatique par referentiel de mots-cles (Strategy).
 *
 * <p>Chaque categorie porte un lexique medical/administratif pondere : le
 * score d'une categorie est la somme des poids des termes trouves dans le nom
 * du fichier, le type MIME, les metadonnees DICOM et le texte extrait par
 * OCR. La confiance est le score de la meilleure categorie normalise par le
 * total des scores, ce qui permet au cas d'usage de decider si la
 * classification est appliquee directement ou soumise a confirmation humaine
 * (seuil {@code MedicalDocument.AUTO_CLASSIFICATION_TRUST}).
 *
 * <p>Un modele d'IA (vision pour l'imagerie, NLP pour les comptes rendus)
 * s'insere en implementant le meme {@link DocumentClassificationPort}.
 */
@Component
public class KeywordDocumentClassificationAdapter implements DocumentClassificationPort {

    private static final Logger log = LoggerFactory.getLogger(KeywordDocumentClassificationAdapter.class);

    /** Lexique pondere par categorie (ordre significatif : premiers termes les plus discriminants). */
    private static final Map<MedicalDocument.DocumentCategory, Map<String, Integer>> LEXICON = lexicon();

    @Override
    public Classification classify(ClassificationRequest request) {
        String haystack = buildHaystack(request);
        if (haystack.isBlank()) {
            return new Classification(MedicalDocument.DocumentCategory.OTHER, 0.1);
        }

        MedicalDocument.DocumentCategory best = MedicalDocument.DocumentCategory.OTHER;
        double bestScore = 0;
        double total = 0;
        for (Map.Entry<MedicalDocument.DocumentCategory, Map<String, Integer>> entry : LEXICON.entrySet()) {
            double score = 0;
            for (Map.Entry<String, Integer> keyword : entry.getValue().entrySet()) {
                if (haystack.contains(keyword.getKey())) {
                    score += keyword.getValue();
                }
            }
            total += score;
            if (score > bestScore) {
                bestScore = score;
                best = entry.getKey();
            }
        }

        if (bestScore == 0) {
            log.debug("Aucun terme discriminant pour [{}] : categorie OTHER", request.filename());
            return new Classification(MedicalDocument.DocumentCategory.OTHER, 0.15);
        }
        double confidence = Math.min(0.99, bestScore / Math.max(total, 1) + Math.min(bestScore, 4) * 0.05);
        log.debug("Classification [{}] -> {} (confiance {})", request.filename(), best, confidence);
        return new Classification(best, Math.round(confidence * 100) / 100.0);
    }

    @Override
    public String name() {
        return "keyword-lexicon-v1";
    }

    private String buildHaystack(ClassificationRequest request) {
        StringBuilder builder = new StringBuilder();
        append(builder, request.filename());
        append(builder, request.contentType());
        append(builder, request.extractedText());
        if (request.dicomModality() != null) {
            builder.append(" dicom modality ").append(request.dicomModality().toLowerCase(Locale.ROOT));
        }
        return builder.toString();
    }

    private static void append(StringBuilder builder, String value) {
        if (value != null && !value.isBlank()) {
            builder.append(value.toLowerCase(Locale.ROOT)).append(' ');
        }
    }

    private static Map<MedicalDocument.DocumentCategory, Map<String, Integer>> lexicon() {
        Map<MedicalDocument.DocumentCategory, Map<String, Integer>> lexicon = new LinkedHashMap<>();
        lexicon.put(MedicalDocument.DocumentCategory.PRESCRIPTION, Map.of(
                "ordonnance", 5, "prescription", 5, "prescrire", 3, "posologie", 3, "renouvellement", 2));
        lexicon.put(MedicalDocument.DocumentCategory.LAB_RESULT, Map.of(
                "analyse", 4, "biologie", 4, "laboratoire", 4, "resultat", 3, "hemogramme", 5,
                "glycemie", 4, "cholesterol", 4, "nfs", 4, "serologie", 4));
        lexicon.put(MedicalDocument.DocumentCategory.IMAGING_XRAY, Map.of(
                "radiographie", 5, "radio", 4, "cliche", 3, "modality cr", 5, "modality dx", 5));
        lexicon.put(MedicalDocument.DocumentCategory.IMAGING_MRI, Map.of(
                "irm", 5, "mri", 5, "modality mr", 5, "reconstruction", 2));
        lexicon.put(MedicalDocument.DocumentCategory.IMAGING_CT_SCAN, Map.of(
                "scanner", 5, "tomodensitometrie", 5, "modality ct", 5));
        lexicon.put(MedicalDocument.DocumentCategory.IMAGING_ULTRASOUND, Map.of(
                "echographie", 5, "ultrasounds", 4, "ultrason", 4, "modality us", 5, "doppler", 3));
        lexicon.put(MedicalDocument.DocumentCategory.OPERATIVE_REPORT, Map.of(
                "compte rendu operatoire", 6, "operatoire", 5, "chirurgie", 4, "intervention", 3,
                "anesthesie", 3, "bloc", 2));
        lexicon.put(MedicalDocument.DocumentCategory.HOSPITALIZATION_REPORT, Map.of(
                "hospitalisation", 5, "sortie", 3, "sejour", 3, "admission", 3, "crh", 4));
        lexicon.put(MedicalDocument.DocumentCategory.CORRESPONDENCE, Map.of(
                "courrier", 5, "correspondance", 5, "confrere", 4, "lettre", 3, "adresse", 1));
        lexicon.put(MedicalDocument.DocumentCategory.MEDICAL_CERTIFICATE, Map.of(
                "certificat", 5, "arret de travail", 5, "aptitude", 3, "attestation", 3));
        lexicon.put(MedicalDocument.DocumentCategory.VACCINATION_RECORD, Map.of(
                "vaccin", 5, "vaccination", 5, "carnet", 3, "rappel", 2, "dtp", 3));
        return lexicon;
    }
}
