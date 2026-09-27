package com.prdv.rdv.profile.domain.model.medical;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Service de domaine : calcul des rappels de vaccination.
 *
 * <p>Regle metier isolee de l'agregat (Single Responsibility) : un vaccin est
 * « a rappeler » si sa date de rappel est atteinte ou echue, ou dans la
 * fenetre d'anticipation demandee. Le service est pur et testable sans
 * infrastructure.
 */
public final class VaccinationReminderCalculator {

    private VaccinationReminderCalculator() {
    }

    /** Rappels echus ou prevus dans les {@code lookaheadDays} prochains jours, les plus proches d'abord. */
    public static List<MedicalRecord.Vaccination> dueReminders(MedicalRecord record,
                                                              LocalDate today,
                                                              int lookaheadDays) {
        if (record == null || today == null) {
            return List.of();
        }
        int lookahead = Math.max(0, lookaheadDays);
        LocalDate horizon = today.plusDays(lookahead);
        return record.getVaccinations().stream()
                .filter(vaccination -> vaccination.nextReminderOn() != null)
                .filter(vaccination -> !vaccination.nextReminderOn().isAfter(horizon))
                .sorted(Comparator.comparing(MedicalRecord.Vaccination::nextReminderOn))
                .toList();
    }

    public static boolean isOverdue(MedicalRecord.Vaccination vaccination, LocalDate today) {
        return vaccination.nextReminderOn() != null && vaccination.nextReminderOn().isBefore(today);
    }

    /** Nombre de jours avant le prochain rappel (negatif si le rappel est depasse). */
    public static long daysUntilReminder(MedicalRecord.Vaccination vaccination, LocalDate today) {
        if (vaccination.nextReminderOn() == null) {
            return Long.MAX_VALUE;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(today, vaccination.nextReminderOn());
    }
}
