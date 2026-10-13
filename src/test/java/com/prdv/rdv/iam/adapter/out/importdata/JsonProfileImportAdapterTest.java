package com.prdv.rdv.iam.adapter.out.importdata;

import com.prdv.rdv.iam.application.port.output.ProfileImportPort;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonProfileImportAdapterTest {

    private final JsonProfileImportAdapter adapter = new JsonProfileImportAdapter();

    @Test
    void parses_json_and_external_platform_payload() {
        String json = """
                {
                  "firstName": "Thomas",
                  "lastName": "Martin",
                  "birthDate": "1985-11-23",
                  "email": "thomas.martin@example.com",
                  "phone": "+33698765432",
                  "sourcePlatform": "Doctolib"
                }
                """;

        assertTrue(adapter.supports("json"));
        assertTrue(adapter.supports("Doctolib"));
        ProfileImportPort.ImportedProfile profile = adapter.parse(json);

        assertEquals("Thomas", profile.firstName());
        assertEquals("Martin", profile.lastName());
        assertEquals("1985-11-23", profile.birthDate().toString());
        assertEquals("Doctolib", profile.sourcePlatform());
    }
}
