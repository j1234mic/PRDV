package com.prdv.rdv.profile.application.port.output;

import java.util.Map;

/**
 * Port de sortie : generation de l'export de portabilite (RGPD art. 20).
 *
 * <p>Adapteurs : JSON (fourni), CSV, FHIR Bundle... Le format est un detail
 * technique : le cas d'usage prepare un arbre de donnees neutre.
 */
public interface DataExportPort {

    ExportedData export(String format, Map<String, Object> data);

    String supportedFormats();

    record ExportedData(String filename, String contentType, String content) {
    }
}
