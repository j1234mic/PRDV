package com.prdv.rdv.profile.application.port.input;

import com.prdv.rdv.profile.application.command.MedicalRecordCommands;
import com.prdv.rdv.profile.application.result.ProfileViews;

/**
 * Cas d'usage : ecriture dans le dossier medical personnel.
 *
 * <p>Un praticien habilité (consentement + regle de visibilite) peut alimenter
 * le dossier du patient qu'il suit ; le patient garde la main sur ses propres
 * donnees.
 */
public interface MedicalRecordCommandUseCase {

    ProfileViews.MedicalRecordView setBloodGroup(Long patientUserId, MedicalRecordCommands.SetBloodGroup command);

    ProfileViews.MedicalRecordView addHistoryEntry(Long patientUserId,
                                                   MedicalRecordCommands.AddHistoryEntry command);

    ProfileViews.MedicalRecordView removeHistoryEntry(Long patientUserId, String entryId);

    ProfileViews.MedicalRecordView addFamilyHistory(Long patientUserId,
                                                    MedicalRecordCommands.AddFamilyHistory command);

    ProfileViews.MedicalRecordView removeFamilyHistory(Long patientUserId, String entryId);

    ProfileViews.MedicalRecordView declareAllergy(Long patientUserId,
                                                  MedicalRecordCommands.DeclareAllergy command);

    ProfileViews.MedicalRecordView resolveAllergy(Long patientUserId, String entryId);

    ProfileViews.MedicalRecordView removeAllergy(Long patientUserId, String entryId);

    ProfileViews.MedicalRecordView recordVaccination(Long patientUserId,
                                                     MedicalRecordCommands.RecordVaccination command);

    ProfileViews.MedicalRecordView removeVaccination(Long patientUserId, String entryId);

    ProfileViews.MedicalRecordView addChronicCondition(Long patientUserId,
                                                       MedicalRecordCommands.AddChronicCondition command);

    ProfileViews.MedicalRecordView removeChronicCondition(Long patientUserId, String entryId);

    ProfileViews.MedicalRecordView startTreatment(Long patientUserId,
                                                  MedicalRecordCommands.StartTreatment command);

    ProfileViews.MedicalRecordView endTreatment(Long patientUserId, String entryId,
                                                MedicalRecordCommands.EndTreatment command);

    ProfileViews.MedicalRecordView removeTreatment(Long patientUserId, String entryId);

    ProfileViews.MedicalRecordView addSurgery(Long patientUserId, MedicalRecordCommands.AddSurgery command);

    ProfileViews.MedicalRecordView removeSurgery(Long patientUserId, String entryId);

    ProfileViews.MedicalRecordView addHospitalization(Long patientUserId,
                                                      MedicalRecordCommands.AddHospitalization command);

    ProfileViews.MedicalRecordView removeHospitalization(Long patientUserId, String entryId);

    ProfileViews.MedicalRecordView declareDisability(Long patientUserId,
                                                     MedicalRecordCommands.DeclareDisability command);

    ProfileViews.MedicalRecordView removeDisability(Long patientUserId, String entryId);

    ProfileViews.MedicalRecordView recordVitalSigns(Long patientUserId,
                                                    MedicalRecordCommands.RecordVitalSigns command);
}
