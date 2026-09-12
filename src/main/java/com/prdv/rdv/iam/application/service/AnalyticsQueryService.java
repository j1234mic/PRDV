package com.prdv.rdv.iam.application.service;

import com.prdv.rdv.iam.application.port.input.AnalyticsQueryUseCase;
import com.prdv.rdv.iam.application.port.output.PasswordHasher;
import com.prdv.rdv.iam.application.port.output.PatientProfileRepository;
import com.prdv.rdv.iam.application.port.output.UserRepository;
import com.prdv.rdv.iam.application.result.Views;
import com.prdv.rdv.iam.domain.model.user.AccountStatus;
import com.prdv.rdv.iam.domain.model.user.PatientProfile;
import com.prdv.rdv.iam.domain.model.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Vue analytique sur des donnees anonymisees / pseudonymisees :
 * aucun PII direct (pseudonyme derive par hachage, tranche d'age seulement).
 */
@Service
public class AnalyticsQueryService implements AnalyticsQueryUseCase {

    private static final int PAGE_SIZE = 500;

    private final UserRepository userRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public AnalyticsQueryService(UserRepository userRepository,
                                 PatientProfileRepository patientProfileRepository,
                                 PasswordHasher passwordHasher, Clock clock) {
        this.userRepository = userRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Views.AnonymizedUserView> anonymizedUsers() {
        List<User> users = userRepository.findAll(0, PAGE_SIZE);
        List<Views.AnonymizedUserView> result = new ArrayList<>();
        LocalDate today = LocalDate.now(clock);

        for (User user : users) {
            if (user.getStatus() == AccountStatus.DELETED || user.getEmail() == null) {
                continue;
            }
            String pseudonym = passwordHasher.digest(user.getEmail()).substring(0, 16);

            Integer ageMin = null;
            Integer ageMax = null;
            PatientProfile patient = patientProfileRepository.findByUserId(user.getId()).orElse(null);
            if (patient != null && patient.getBirthDate() != null) {
                int age = Period.between(patient.getBirthDate(), today).getYears();
                ageMin = (age / 10) * 10;
                ageMax = ageMin + 9;
            }
            String kyc = patient == null || patient.getKycLevel() == null ? null
                    : patient.getKycLevel().name();

            LocalDate creationDate = LocalDate.ofInstant(user.getCreatedAt(), ZoneOffset.UTC);
            int accountAgeYears = Math.max(0, Period.between(creationDate, today).getYears());

            result.add(new Views.AnonymizedUserView(pseudonym, user.getProfileType(),
                    kyc, ageMin, ageMax, accountAgeYears));
        }
        return result;
    }
}
