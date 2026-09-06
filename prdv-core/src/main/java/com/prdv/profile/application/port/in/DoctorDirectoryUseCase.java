package com.prdv.profile.application.port.in;

import com.prdv.profile.application.model.DoctorSummary;

import java.util.List;

/** Recherche "Pres de moi" (module 3 - noyau annuaire uniquement). */
public interface DoctorDirectoryUseCase {
    List<DoctorSummary> search(String specialty, int limit);
}
