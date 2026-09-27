package com.prdv.rdv.profile.domain.model.medical;

import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;
import lombok.Getter;
import lombok.Setter;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Agregat racine « Dossier Medical Personnel » (module 2.1 — DMP).
 *
 * <p>Regroupe les antecedents structures, les antecedents familiaux, les
 * allergies et intolerances, les vaccinations (avec rappels), le groupe
 * sanguin, les maladies chroniques, les traitements en cours, les chirurgies,
 * les hospitalisations, les constantes vitales / IMC et les handicaps.
 *
 * <p>Chaque entree porte un identifiant technique stable (UUID) pour permettre
 * une modification ou une suppression unitaire sans exposer l'ordre interne
 * des collections. Les regles d'integrite (unicite d'un allergene actif,
 * coherence des dates de traitement) sont verifiees dans l'agregat.
 */
@Getter
@Setter
public class MedicalRecord {

    // ------------------------------------------------------------------
    // Enumerations
    // ------------------------------------------------------------------

    public enum HistoryStatus { ACTIVE, RESOLVED, MONITORED }

    public enum AllergenType { DRUG, FOOD, ENVIRONMENT, CHEMICAL, ANIMAL, OTHER }

    public enum AllergySeverity { MILD, MODERATE, SEVERE, LIFE_THREATENING }

    public enum FamilyRelation {
        FATHER, MOTHER, BROTHER, SISTER, SON, DAUGHTER, GRANDFATHER, GRANDMOTHER,
        UNCLE, AUNT, COUSIN, OTHER
    }

    public enum DisabilityType {
        MOTOR, VISUAL, HEARING, COGNITIVE, PSYCHIC, SPEECH, VISCERAL, OTHER
    }

    // ------------------------------------------------------------------
    // Value Objects (entrees structurees)
    // ------------------------------------------------------------------

    /** Antecedent medical structure (code terminologique + libelle lisible). */
    public record MedicalHistoryEntry(String id,
                                      String code,
                                      String label,
                                      LocalDate diagnosedOn,
                                      HistoryStatus status,
                                      String note,
                                      String source) {
    }

    public record FamilyHistoryEntry(String id,
                                     FamilyRelation relation,
                                     String condition,
                                     Integer relativeAgeAtDiagnosis,
                                     String note) {
    }

    public record Allergy(String id,
                          String allergen,
                          AllergenType type,
                          AllergySeverity severity,
                          String reaction,
                          LocalDate declaredOn,
                          boolean active) {
    }

    public record Vaccination(String id,
                              String vaccine,
                              String dose,
                              LocalDate administeredOn,
                              LocalDate nextReminderOn,
                              String provider,
                              boolean mandatory) {
    }

    /** Maladie chronique (dont affection de longue duree). */
    public record ChronicCondition(String id,
                                   String code,
                                   String label,
                                   LocalDate since,
                                   boolean longTermCondition,
                                   String note) {
    }

    public record Treatment(String id,
                            String drug,
                            String dosage,
                            String frequency,
                            String prescriber,
                            LocalDate startedOn,
                            LocalDate endedOn,
                            String note) {

        public boolean isOngoing() {
            return endedOn == null;
        }
    }

    public record Surgery(String id,
                          String procedure,
                          LocalDate performedOn,
                          String establishment,
                          String surgeon,
                          String note) {
    }

    public record Hospitalization(String id,
                                  String reason,
                                  String establishment,
                                  LocalDate admittedOn,
                                  LocalDate dischargedOn,
                                  String note) {
    }

    public record Disability(String id,
                             DisabilityType type,
                             Integer disabilityRatePercent,
                             List<String> aids,
                             String note) {

        public Disability {
            if (type == null) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                        "Le type de handicap est obligatoire");
            }
            if (disabilityRatePercent != null
                    && (disabilityRatePercent < 0 || disabilityRatePercent > 100)) {
                throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                        "Le taux d'incapacite doit etre compris entre 0 et 100 %");
            }
            aids = aids == null ? List.of() : List.copyOf(aids);
        }
    }

    /** Dernier releve de constantes (tension, pouls, temperature, SpO2...). */
    public record VitalSignsSnapshot(VitalSigns vitalSigns, Instant recordedAt) {
    }

    // ------------------------------------------------------------------
    // Etat de l'agregat
    // ------------------------------------------------------------------

    private Long id;
    private Long patientUserId;

    private BloodGroup bloodGroup;

    private List<MedicalHistoryEntry> history = new ArrayList<>();
    private List<FamilyHistoryEntry> familyHistory = new ArrayList<>();
    private List<Allergy> allergies = new ArrayList<>();
    private List<Vaccination> vaccinations = new ArrayList<>();
    private List<ChronicCondition> chronicConditions = new ArrayList<>();
    private List<Treatment> treatments = new ArrayList<>();
    private List<Surgery> surgeries = new ArrayList<>();
    private List<Hospitalization> hospitalizations = new ArrayList<>();
    private List<Disability> disabilities = new ArrayList<>();

    private VitalSignsSnapshot lastVitalSigns;

    private Instant createdAt;
    private Instant updatedAt;

    // ------------------------------------------------------------------
    // Fabrique
    // ------------------------------------------------------------------

    public static MedicalRecord open(Long patientUserId, Clock clock) {
        if (patientUserId == null) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "Un dossier medical doit etre rattache a un patient");
        }
        MedicalRecord record = new MedicalRecord();
        record.patientUserId = patientUserId;
        record.createdAt = clock.instant();
        record.updatedAt = record.createdAt;
        return record;
    }

    // ------------------------------------------------------------------
    // Comportements
    // ------------------------------------------------------------------

    public void setBloodGroupValue(BloodGroup newBloodGroup, Clock clock) {
        this.bloodGroup = newBloodGroup;
        touch(clock);
    }

    public MedicalHistoryEntry addHistoryEntry(String code, String label, LocalDate diagnosedOn,
                                               HistoryStatus status, String note, String source,
                                               Clock clock) {
        requireLabel(label, "antecedent medical");
        MedicalHistoryEntry entry = new MedicalHistoryEntry(newId(), code, label.trim(), diagnosedOn,
                status == null ? HistoryStatus.ACTIVE : status, note, source);
        this.history.add(entry);
        touch(clock);
        return entry;
    }

    public void removeHistoryEntry(String entryId, Clock clock) {
        removeOrThrow(history, entryId, "antecedent medical", e -> e.id());
        touch(clock);
    }

    public FamilyHistoryEntry addFamilyHistoryEntry(FamilyRelation relation, String condition,
                                                    Integer relativeAgeAtDiagnosis, String note,
                                                    Clock clock) {
        if (relation == null) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "Le lien de parente est obligatoire");
        }
        requireLabel(condition, "antecedent familial");
        FamilyHistoryEntry entry = new FamilyHistoryEntry(newId(), relation, condition.trim(),
                relativeAgeAtDiagnosis, note);
        this.familyHistory.add(entry);
        touch(clock);
        return entry;
    }

    public void removeFamilyHistoryEntry(String entryId, Clock clock) {
        removeOrThrow(familyHistory, entryId, "antecedent familial", FamilyHistoryEntry::id);
        touch(clock);
    }

    /** Ajoute un allergene ; un allergene deja declare est mis a jour (pas de doublon actif). */
    public Allergy declareAllergy(String allergen, AllergenType type, AllergySeverity severity,
                                  String reaction, LocalDate declaredOn, Clock clock) {
        requireLabel(allergen, "allergene");
        String normalized = allergen.trim().toLowerCase(java.util.Locale.ROOT);
        Optional<Allergy> existing = allergies.stream()
                .filter(allergy -> allergy.allergen().toLowerCase(java.util.Locale.ROOT).equals(normalized))
                .findFirst();
        if (existing.isPresent()) {
            Allergy updated = new Allergy(existing.get().id(), existing.get().allergen(),
                    type == null ? existing.get().type() : type,
                    severity == null ? existing.get().severity() : severity,
                    reaction == null ? existing.get().reaction() : reaction,
                    declaredOn == null ? existing.get().declaredOn() : declaredOn,
                    true);
            this.allergies.set(this.allergies.indexOf(existing.get()), updated);
            touch(clock);
            return updated;
        }
        Allergy allergy = new Allergy(newId(), allergen.trim(),
                type == null ? AllergenType.OTHER : type,
                severity == null ? AllergySeverity.MODERATE : severity,
                reaction, declaredOn, true);
        this.allergies.add(allergy);
        touch(clock);
        return allergy;
    }

    public void resolveAllergy(String entryId, Clock clock) {
        Allergy allergy = findOrThrow(allergies, entryId, "allergie", Allergy::id);
        if (!allergy.active()) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "Cette allergie est deja resolue");
        }
        this.allergies.set(this.allergies.indexOf(allergy), new Allergy(allergy.id(), allergy.allergen(),
                allergy.type(), allergy.severity(), allergy.reaction(), allergy.declaredOn(), false));
        touch(clock);
    }

    public void removeAllergy(String entryId, Clock clock) {
        removeOrThrow(allergies, entryId, "allergie", Allergy::id);
        touch(clock);
    }

    public List<Allergy> activeAllergies() {
        return allergies.stream().filter(Allergy::active).toList();
    }

    public Vaccination recordVaccination(String vaccine, String dose, LocalDate administeredOn,
                                         LocalDate nextReminderOn, String provider, boolean mandatory,
                                         Clock clock) {
        requireLabel(vaccine, "vaccin");
        if (administeredOn == null) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "La date d'injection du vaccin est obligatoire");
        }
        if (nextReminderOn != null && nextReminderOn.isBefore(administeredOn)) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "La date de rappel precede la date d'injection");
        }
        Vaccination vaccination = new Vaccination(newId(), vaccine.trim(), dose, administeredOn,
                nextReminderOn, provider, mandatory);
        this.vaccinations.add(vaccination);
        touch(clock);
        return vaccination;
    }

    public void removeVaccination(String entryId, Clock clock) {
        removeOrThrow(vaccinations, entryId, "vaccination", Vaccination::id);
        touch(clock);
    }

    public ChronicCondition addChronicCondition(String code, String label, LocalDate since,
                                                boolean longTermCondition, String note, Clock clock) {
        requireLabel(label, "maladie chronique");
        ChronicCondition condition = new ChronicCondition(newId(), code, label.trim(), since,
                longTermCondition, note);
        this.chronicConditions.add(condition);
        touch(clock);
        return condition;
    }

    public void removeChronicCondition(String entryId, Clock clock) {
        removeOrThrow(chronicConditions, entryId, "maladie chronique", ChronicCondition::id);
        touch(clock);
    }

    public Treatment startTreatment(String drug, String dosage, String frequency, String prescriber,
                                    LocalDate startedOn, String note, Clock clock) {
        requireLabel(drug, "traitement");
        Treatment treatment = new Treatment(newId(), drug.trim(), dosage, frequency, prescriber,
                startedOn == null ? LocalDate.now(clock) : startedOn, null, note);
        this.treatments.add(treatment);
        touch(clock);
        return treatment;
    }

    public Treatment endTreatment(String entryId, LocalDate endedOn, Clock clock) {
        Treatment treatment = findOrThrow(treatments, entryId, "traitement", Treatment::id);
        if (!treatment.isOngoing()) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR, "Ce traitement est deja arrete");
        }
        LocalDate stopDate = endedOn == null ? LocalDate.now(clock) : endedOn;
        if (treatment.startedOn() != null && stopDate.isBefore(treatment.startedOn())) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "La date d'arret precede la date de debut du traitement");
        }
        Treatment ended = new Treatment(treatment.id(), treatment.drug(), treatment.dosage(),
                treatment.frequency(), treatment.prescriber(), treatment.startedOn(), stopDate,
                treatment.note());
        this.treatments.set(this.treatments.indexOf(treatment), ended);
        touch(clock);
        return ended;
    }

    public void removeTreatment(String entryId, Clock clock) {
        removeOrThrow(treatments, entryId, "traitement", Treatment::id);
        touch(clock);
    }

    public List<Treatment> ongoingTreatments() {
        return treatments.stream().filter(Treatment::isOngoing).toList();
    }

    public Surgery addSurgery(String procedure, LocalDate performedOn, String establishment,
                              String surgeon, String note, Clock clock) {
        requireLabel(procedure, "intervention chirurgicale");
        Surgery surgery = new Surgery(newId(), procedure.trim(), performedOn, establishment, surgeon, note);
        this.surgeries.add(surgery);
        touch(clock);
        return surgery;
    }

    public void removeSurgery(String entryId, Clock clock) {
        removeOrThrow(surgeries, entryId, "intervention chirurgicale", Surgery::id);
        touch(clock);
    }

    public Hospitalization addHospitalization(String reason, String establishment, LocalDate admittedOn,
                                              LocalDate dischargedOn, String note, Clock clock) {
        requireLabel(reason, "hospitalisation");
        if (admittedOn != null && dischargedOn != null && dischargedOn.isBefore(admittedOn)) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "La date de sortie precede la date d'admission");
        }
        Hospitalization hospitalization = new Hospitalization(newId(), reason.trim(), establishment,
                admittedOn, dischargedOn, note);
        this.hospitalizations.add(hospitalization);
        touch(clock);
        return hospitalization;
    }

    public void removeHospitalization(String entryId, Clock clock) {
        removeOrThrow(hospitalizations, entryId, "hospitalisation", Hospitalization::id);
        touch(clock);
    }

    public Disability declareDisability(DisabilityType type, Integer ratePercent, List<String> aids,
                                        String note, Clock clock) {
        Disability disability = new Disability(newId(), type, ratePercent, aids, note);
        this.disabilities.add(disability);
        touch(clock);
        return disability;
    }

    public void removeDisability(String entryId, Clock clock) {
        removeOrThrow(disabilities, entryId, "handicap", Disability::id);
        touch(clock);
    }

    /** Enregistre un releve de constantes ; la validation des plages est portee par {@link VitalSigns}. */
    public VitalSignsSnapshot recordVitalSigns(VitalSigns vitalSigns, Clock clock) {
        if (vitalSigns == null) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR, "Les constantes sont obligatoires");
        }
        this.lastVitalSigns = new VitalSignsSnapshot(vitalSigns, clock.instant());
        touch(clock);
        return this.lastVitalSigns;
    }

    /** IMC courant derive des dernieres constantes, si la taille et le poids sont connus. */
    public Optional<BodyMetrics> currentBodyMetrics() {
        if (lastVitalSigns == null || lastVitalSigns.vitalSigns().bodyMetrics() == null) {
            return Optional.empty();
        }
        return Optional.of(lastVitalSigns.vitalSigns().bodyMetrics());
    }

    /** Efface le contenu medical du dossier (droit a l'oubli) en conservant la coquille. */
    public void erase(Clock clock) {
        this.bloodGroup = null;
        this.history = new ArrayList<>();
        this.familyHistory = new ArrayList<>();
        this.allergies = new ArrayList<>();
        this.vaccinations = new ArrayList<>();
        this.chronicConditions = new ArrayList<>();
        this.treatments = new ArrayList<>();
        this.surgeries = new ArrayList<>();
        this.hospitalizations = new ArrayList<>();
        this.disabilities = new ArrayList<>();
        this.lastVitalSigns = null;
        touch(clock);
    }

    // ------------------------------------------------------------------
    // Support
    // ------------------------------------------------------------------

    private void touch(Clock clock) {
        this.updatedAt = clock.instant();
    }

    private static String newId() {
        return UUID.randomUUID().toString();
    }

    private static void requireLabel(String label, String what) {
        if (label == null || label.isBlank()) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR, "Le libelle de l'" + what + " est obligatoire");
        }
    }

    private interface IdExtractor<T> {
        String id(T value);
    }

    private static <T> T findOrThrow(List<T> items, String entryId, String what, IdExtractor<T> extractor) {
        if (entryId == null) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR, "L'identifiant d'entree est obligatoire");
        }
        return items.stream()
                .filter(item -> entryId.equals(extractor.id(item)))
                .findFirst()
                .orElseThrow(() -> ProfileException.of(ProfileErrorCode.ENTRY_NOT_FOUND,
                        "Aucun " + what + " ne correspond a l'identifiant " + entryId));
    }

    private static <T> void removeOrThrow(List<T> items, String entryId, String what,
                                          IdExtractor<T> extractor) {
        T found = findOrThrow(items, entryId, what, extractor);
        items.remove(found);
    }

    /** Copie defensive des identifiants d'entrees (utilisee par les vues). */
    public Set<String> entryIdentifiers() {
        Set<String> ids = new LinkedHashSet<>();
        history.forEach(entry -> ids.add(entry.id()));
        familyHistory.forEach(entry -> ids.add(entry.id()));
        allergies.forEach(entry -> ids.add(entry.id()));
        vaccinations.forEach(entry -> ids.add(entry.id()));
        chronicConditions.forEach(entry -> ids.add(entry.id()));
        treatments.forEach(entry -> ids.add(entry.id()));
        surgeries.forEach(entry -> ids.add(entry.id()));
        hospitalizations.forEach(entry -> ids.add(entry.id()));
        disabilities.forEach(entry -> ids.add(entry.id()));
        return ids;
    }
}
