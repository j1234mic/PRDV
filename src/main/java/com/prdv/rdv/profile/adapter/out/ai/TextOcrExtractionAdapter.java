package com.prdv.rdv.profile.adapter.out.ai;

import com.prdv.rdv.profile.application.port.output.OcrExtractionPort;
import com.prdv.rdv.profile.application.service.support.MedicalFilePolicy;
import com.prdv.rdv.profile.domain.model.document.MedicalDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extraction de texte et de champs structures (OCR).
 *
 * <p>L'adapteur fourni traite nativement les fichiers deja textuels (PDF
 * textuels, TXT, CSV, XML) : il en extrait les champs reglementaires utiles
 * au classement et a la saisie automatique (numero de securite sociale,
 * dates, identifiants d'acte).
 *
 * <p>Pour les images (JPG, PNG, TIFF) et les PDF scannes, le format est
 * declare non pris en charge : brancher Tesseract, AWS Textract ou Google
 * Document AI sur ce meme port suffit a les couvrir, sans modifier les cas
 * d'usage (Open/Closed).
 */
@Component
public class TextOcrExtractionAdapter implements OcrExtractionPort {

    private static final Logger log = LoggerFactory.getLogger(TextOcrExtractionAdapter.class);
    private static final String ENGINE = "text-extractor-v1";

    private static final Pattern SOCIAL_SECURITY = Pattern.compile("\\b[12]\\s?\\d{2}(?:\\s?\\d{2}){2}"
            + "(?:\\s?\\d{2}|\\s?2[AB])(?:\\s?\\d{3}){2}\\s?\\d{2}\\b");
    private static final Pattern DATE = Pattern.compile("\\b(\\d{2}[/.-]\\d{2}[/.-]\\d{4}"
            + "|\\d{4}-\\d{2}-\\d{2})\\b");
    private static final Pattern PRACTITIONER_ID = Pattern.compile("\\b(?:RPPS|ADELI)\\s*[:#]?\\s*([0-9]{5,11})\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern LAB_VALUE = Pattern.compile(
            "\\b(glycemie|hemoglobine|cholesterol|creatinine|tsh|plaquettes)\\b\\s*[:=]?\\s*([0-9]+(?:[.,][0-9]+)?)",
            Pattern.CASE_INSENSITIVE);

    private final MedicalFilePolicy filePolicy;
    private final Clock clock;

    public TextOcrExtractionAdapter(MedicalFilePolicy filePolicy, Clock clock) {
        this.filePolicy = filePolicy;
        this.clock = clock;
    }

    @Override
    public MedicalDocument.OcrResult extract(String originalFilename, String contentType, byte[] content) {
        String text = filePolicy.asText(contentType, content);
        if (text == null) {
            log.debug("OCR indisponible pour [{}] ({}), un moteur OCR externe est requis",
                    originalFilename, contentType);
            return MedicalDocument.OcrResult.unsupported(ENGINE, clock.instant());
        }

        Map<String, String> fields = new LinkedHashMap<>();
        firstMatch(SOCIAL_SECURITY, text).ifPresent(value ->
                fields.put("socialSecurityNumber", value.replaceAll("\\s+", "")));
        firstMatch(DATE, text).ifPresent(value -> fields.put("documentDate", value));
        firstMatch(PRACTITIONER_ID, text).ifPresent(value -> fields.put("practitionerIdentifier", value));
        Matcher labMatcher = LAB_VALUE.matcher(text);
        int labValues = 0;
        while (labMatcher.find() && labValues < 10) {
            fields.put("lab_" + labMatcher.group(1).toLowerCase(java.util.Locale.ROOT), labMatcher.group(2));
            labValues++;
        }

        double confidence = fields.isEmpty() ? 0.35 : 0.75;
        String truncated = text.length() > 20_000 ? text.substring(0, 20_000) : text;
        return new MedicalDocument.OcrResult(MedicalDocument.OcrStatus.SUCCEEDED, truncated, fields,
                confidence, ENGINE, clock.instant());
    }

    private static java.util.Optional<String> firstMatch(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? java.util.Optional.of(matcher.group().trim()) : java.util.Optional.empty();
    }
}
