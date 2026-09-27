package com.prdv.rdv.profile.application.port.output;

import com.prdv.rdv.profile.domain.model.document.MedicalDocument;

/**
 * Port de sortie : extraction de texte et de champs structures (OCR).
 *
 * <p>Adapteurs : Tesseract / AWS Textract / Google Document AI. L'adapteur
 * fourni reconnait deja les documents texte (PDF textuels, TXT, CSV) et
 * extrait des champs reglementaires (NIR, dates, biologiques) ; pour les
 * images, il declare le format comme non pris en charge afin que le document
 * reste classable manuellement.
 */
public interface OcrExtractionPort {

    MedicalDocument.OcrResult extract(String originalFilename, String contentType, byte[] content);
}
