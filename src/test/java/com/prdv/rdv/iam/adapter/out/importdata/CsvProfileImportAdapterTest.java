package com.prdv.rdv.iam.adapter.out.importdata;

import com.prdv.rdv.iam.application.port.output.ProfileImportPort;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvProfileImportAdapterTest {

    private final CsvProfileImportAdapter adapter = new CsvProfileImportAdapter();

    @Test
    void parses_standard_csv_export() {
        String csv = """
                firstName,lastName,birthDate,email,phone,sourcePlatform
                Marie,Dupont,1990-05-12,marie.dupont@example.com,+33612345678,Doctolib
                """;

        assertTrue(adapter.supports("csv"));
        ProfileImportPort.ImportedProfile profile = adapter.parse(csv);

        assertEquals("Marie", profile.firstName());
        assertEquals("Dupont", profile.lastName());
        assertEquals("1990-05-12", profile.birthDate().toString());
        assertEquals("Doctolib", profile.sourcePlatform());
    }

    @Test
    void supports_is_case_insensitive() {
        assertTrue(adapter.supports("text/csv"));
    }
}
