package com.prdv.rdv.profile.adapter.out.persistence.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.prdv.rdv.profile.domain.model.document.MedicalDocument;
import com.prdv.rdv.profile.domain.model.identity.PatientIdentity;
import com.prdv.rdv.profile.domain.model.medical.MedicalRecord;
import com.prdv.rdv.profile.domain.model.preference.PrivacyPreferences;
import com.prdv.rdv.profile.domain.model.practitioner.PracticeLocation;
import com.prdv.rdv.profile.domain.model.practitioner.PractitionerDossier;
import jakarta.persistence.AttributeConverter;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converters JPA serialisant en JSON les collections de Value Objects qui
 * appartiennent a un agregat.
 *
 * <p>Choix assume : ces collections n'ont pas d'existence propre (elles sont
 * toujours chargees et sauvegardees avec leur agregat racine, jamais
 * interrogees individuellement). Les stocker en JSON evite une trentaine de
 * tables de jointure sans perdre le typage fort : chaque converter connait
 * son type cible grace a une {@link TypeReference}.
 *
 * <p>Les agregats a forte volumetrie ou interrogeables (mesures de sante,
 * alertes, documents, lieux d'exercice, avis) restent, eux, des tables
 * dediees.
 */
public final class JsonConverters {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private JsonConverters() {
    }

    /** Socle commun : serialisation / deserialisation typée d'un attribut. */
    abstract static class JsonAttributeConverter<T> implements AttributeConverter<T, String> {

        private final JavaType javaType;

        protected JsonAttributeConverter(TypeReference<? extends T> typeReference) {
            this.javaType = MAPPER.getTypeFactory().constructType(typeReference);
        }

        @Override
        public String convertToDatabaseColumn(T attribute) {
            if (attribute == null) {
                return null;
            }
            try {
                return MAPPER.writeValueAsString(attribute);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Serialisation JSON impossible", e);
            }
        }

        @Override
        public T convertToEntityAttribute(String dbData) {
            if (dbData == null || dbData.isBlank()) {
                return null;
            }
            try {
                return MAPPER.readValue(dbData, javaType);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Deserialisation JSON impossible", e);
            }
        }
    }

    // ------------------------------------------------------------------
    // Identite patient
    // ------------------------------------------------------------------

    public static class ContactPointList extends JsonAttributeConverter<List<PatientIdentity.ContactPoint>> {
        public ContactPointList() {
            super(new TypeReference<List<PatientIdentity.ContactPoint>>() {
            });
        }
    }

    public static class PostalAddressList extends JsonAttributeConverter<List<PatientIdentity.PostalAddress>> {
        public PostalAddressList() {
            super(new TypeReference<List<PatientIdentity.PostalAddress>>() {
            });
        }
    }

    public static class HealthInsuranceList extends JsonAttributeConverter<List<PatientIdentity.HealthInsurance>> {
        public HealthInsuranceList() {
            super(new TypeReference<List<PatientIdentity.HealthInsurance>>() {
            });
        }
    }

    // ------------------------------------------------------------------
    // Dossier medical
    // ------------------------------------------------------------------

    public static class HistoryEntryList extends JsonAttributeConverter<List<MedicalRecord.MedicalHistoryEntry>> {
        public HistoryEntryList() {
            super(new TypeReference<List<MedicalRecord.MedicalHistoryEntry>>() {
            });
        }
    }

    public static class FamilyHistoryList extends JsonAttributeConverter<List<MedicalRecord.FamilyHistoryEntry>> {
        public FamilyHistoryList() {
            super(new TypeReference<List<MedicalRecord.FamilyHistoryEntry>>() {
            });
        }
    }

    public static class AllergyList extends JsonAttributeConverter<List<MedicalRecord.Allergy>> {
        public AllergyList() {
            super(new TypeReference<List<MedicalRecord.Allergy>>() {
            });
        }
    }

    public static class VaccinationList extends JsonAttributeConverter<List<MedicalRecord.Vaccination>> {
        public VaccinationList() {
            super(new TypeReference<List<MedicalRecord.Vaccination>>() {
            });
        }
    }

    public static class ChronicConditionList extends JsonAttributeConverter<List<MedicalRecord.ChronicCondition>> {
        public ChronicConditionList() {
            super(new TypeReference<List<MedicalRecord.ChronicCondition>>() {
            });
        }
    }

    public static class TreatmentList extends JsonAttributeConverter<List<MedicalRecord.Treatment>> {
        public TreatmentList() {
            super(new TypeReference<List<MedicalRecord.Treatment>>() {
            });
        }
    }

    public static class SurgeryList extends JsonAttributeConverter<List<MedicalRecord.Surgery>> {
        public SurgeryList() {
            super(new TypeReference<List<MedicalRecord.Surgery>>() {
            });
        }
    }

    public static class HospitalizationList extends JsonAttributeConverter<List<MedicalRecord.Hospitalization>> {
        public HospitalizationList() {
            super(new TypeReference<List<MedicalRecord.Hospitalization>>() {
            });
        }
    }

    public static class DisabilityList extends JsonAttributeConverter<List<MedicalRecord.Disability>> {
        public DisabilityList() {
            super(new TypeReference<List<MedicalRecord.Disability>>() {
            });
        }
    }

    // ------------------------------------------------------------------
    // Documents medicaux
    // ------------------------------------------------------------------

    public static class DocumentVersionList extends JsonAttributeConverter<List<MedicalDocument.DocumentVersion>> {
        public DocumentVersionList() {
            super(new TypeReference<List<MedicalDocument.DocumentVersion>>() {
            });
        }
    }

    public static class OcrFields extends JsonAttributeConverter<Map<String, String>> {
        public OcrFields() {
            super(new TypeReference<Map<String, String>>() {
            });
        }
    }

    // ------------------------------------------------------------------
    // Preferences et confidentialite
    // ------------------------------------------------------------------

    public static class StringList extends JsonAttributeConverter<List<String>> {
        public StringList() {
            super(new TypeReference<List<String>>() {
            });
        }
    }

    public static class ConsentRecordList extends JsonAttributeConverter<List<PrivacyPreferences.ConsentRecord>> {
        public ConsentRecordList() {
            super(new TypeReference<List<PrivacyPreferences.ConsentRecord>>() {
            });
        }
    }

    public static class VisibilityRuleList extends JsonAttributeConverter<List<PrivacyPreferences.VisibilityRule>> {
        public VisibilityRuleList() {
            super(new TypeReference<List<PrivacyPreferences.VisibilityRule>>() {
            });
        }
    }

    public static class AccessibilityNeedSet extends JsonAttributeConverter<Set<PrivacyPreferences.AccessibilityNeed>> {
        public AccessibilityNeedSet() {
            super(new TypeReference<LinkedHashSet<PrivacyPreferences.AccessibilityNeed>>() {
            });
        }
    }

    public static class CommunicationChannelSet
            extends JsonAttributeConverter<Set<PrivacyPreferences.CommunicationChannel>> {
        public CommunicationChannelSet() {
            super(new TypeReference<LinkedHashSet<PrivacyPreferences.CommunicationChannel>>() {
            });
        }
    }

    // ------------------------------------------------------------------
    // Dossier praticien
    // ------------------------------------------------------------------

    public static class DiplomaList extends JsonAttributeConverter<List<PractitionerDossier.Diploma>> {
        public DiplomaList() {
            super(new TypeReference<List<PractitionerDossier.Diploma>>() {
            });
        }
    }

    public static class TariffList extends JsonAttributeConverter<List<PractitionerDossier.Tariff>> {
        public TariffList() {
            super(new TypeReference<List<PractitionerDossier.Tariff>>() {
            });
        }
    }

    public static class AccreditationList extends JsonAttributeConverter<List<PractitionerDossier.Accreditation>> {
        public AccreditationList() {
            super(new TypeReference<List<PractitionerDossier.Accreditation>>() {
            });
        }
    }

    public static class NetworkContactList extends JsonAttributeConverter<List<PractitionerDossier.NetworkContact>> {
        public NetworkContactList() {
            super(new TypeReference<List<PractitionerDossier.NetworkContact>>() {
            });
        }
    }

    public static class PaymentMethodSet extends JsonAttributeConverter<Set<PractitionerDossier.PaymentMethod>> {
        public PaymentMethodSet() {
            super(new TypeReference<LinkedHashSet<PractitionerDossier.PaymentMethod>>() {
            });
        }
    }

    // ------------------------------------------------------------------
    // Lieux d'exercice
    // ------------------------------------------------------------------

    public static class OpeningHoursList extends JsonAttributeConverter<List<PracticeLocation.OpeningHours>> {
        public OpeningHoursList() {
            super(new TypeReference<List<PracticeLocation.OpeningHours>>() {
            });
        }
    }

    public static class LocationPhotoList extends JsonAttributeConverter<List<PracticeLocation.Photo>> {
        public LocationPhotoList() {
            super(new TypeReference<List<PracticeLocation.Photo>>() {
            });
        }
    }

    public static class SocialLinkMap extends JsonAttributeConverter<Map<String, String>> {
        public SocialLinkMap() {
            super(new TypeReference<LinkedHashMap<String, String>>() {
            });
        }
    }
}
