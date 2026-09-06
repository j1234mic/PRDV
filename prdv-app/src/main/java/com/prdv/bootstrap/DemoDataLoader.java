package com.prdv.bootstrap;

import com.prdv.identity.application.port.in.RegisterUserUseCase;
import com.prdv.identity.application.port.in.VerifyEmailUseCase;
import com.prdv.identity.application.model.UserInfo;
import com.prdv.identity.application.service.OtpIssuer;
import com.prdv.identity.application.port.out.PasswordHasher;
import com.prdv.identity.application.port.out.UserRepository;
import com.prdv.identity.domain.model.PasswordHash;
import com.prdv.identity.domain.model.Role;
import com.prdv.identity.domain.model.User;
import com.prdv.profile.application.port.in.CreateDoctorProfileUseCase;
import com.prdv.profile.application.port.in.CreatePatientProfileUseCase;
import com.prdv.profile.application.port.in.ValidateDoctorProfileUseCase;
import com.prdv.schedule.application.port.in.UpdateScheduleUseCase;
import com.prdv.schedule.application.port.in.WaitingListUseCase;
import com.prdv.schedule.domain.policy.CancellationMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Donnees de demonstration (profile actif avec prdv.seed=true, voir docker-compose "dev").
 * Passe par les MEMES cas d'usage que l'API REST : le seed est donc un test
 * d'integration permanent du wiring complet.
 */
@Component
@ConditionalOnProperty(prefix = "prdv", name = "seed", havingValue = "true")
public class DemoDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    private final RegisterUserUseCase registerUser;
    private final VerifyEmailUseCase verifyEmail;
    private final OtpIssuer otpIssuer;
    private final CreatePatientProfileUseCase createPatient;
    private final CreateDoctorProfileUseCase createDoctor;
    private final ValidateDoctorProfileUseCase validateDoctor;
    private final UpdateScheduleUseCase updateSchedule;
    private final WaitingListUseCase waitingList;
    private final UserRepository users;
    private final PasswordHasher hasher;
    private final Clock clock;

    public DemoDataLoader(RegisterUserUseCase registerUser, VerifyEmailUseCase verifyEmail, OtpIssuer otpIssuer,
                          CreatePatientProfileUseCase createPatient, CreateDoctorProfileUseCase createDoctor,
                          ValidateDoctorProfileUseCase validateDoctor, UpdateScheduleUseCase updateSchedule,
                          WaitingListUseCase waitingList, UserRepository users, PasswordHasher hasher,
                          Clock clock) {
        this.registerUser = registerUser;
        this.verifyEmail = verifyEmail;
        this.otpIssuer = otpIssuer;
        this.createPatient = createPatient;
        this.createDoctor = createDoctor;
        this.validateDoctor = validateDoctor;
        this.updateSchedule = updateSchedule;
        this.waitingList = waitingList;
        this.users = users;
        this.hasher = hasher;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.findByEmail("admin@prdv.local").isPresent()) {
            log.info("Seed deja applique, on ne fait rien.");
            return;
        }
        // ADMIN cree hors du libre-service (module 1.1 : les roles internes passent par l'exploitation).
        User admin = User.register("admin@prdv.local", null, new PasswordHash(hasher.hash("Admin!2026demo")),
                Role.ADMIN, LocalDateTime.now(clock));
        admin.verifyEmail();
        users.save(admin);

        Long drSophie = signup("dr.sophie@prdv.local", "0600112233", "Medecin!2026demo", Role.DOCTOR);
        signupPatient();
        signup("dr.marc@prdv.local", "0600445566", "Medecin!2026demo", Role.DOCTOR);

        createDoctor.create(new CreateDoctorProfileUseCase.Command(drSophie, "Dr Sophie Martin",
                "10000379512", "Medecine generale", List.of("Pedagogie PDSA"), "Medecine familiale et preventive",
                1, 2500, List.of("fr", "en"),
                List.of(new CreateDoctorProfileUseCase.PracticeLocationDto("Cabinet Centre",
                        "12 rue de la Paix", "75002", "Paris", "0145667788"))));
        validateDoctor.approve(drSophie);
        updateSchedule.addRule(drSophie, new UpdateScheduleUseCase.RuleDto(DayOfWeek.MONDAY,
                LocalTime.of(9, 0), LocalTime.of(12, 0), 20));
        for (DayOfWeek d : List.of(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)) {
            updateSchedule.addRule(drSophie, new UpdateScheduleUseCase.RuleDto(d,
                    LocalTime.of(9, 0), LocalTime.of(12, 0), 20));
        }
        updateSchedule.addRule(drSophie, new UpdateScheduleUseCase.RuleDto(DayOfWeek.TUESDAY,
                LocalTime.of(14, 0), LocalTime.of(18, 0), 30));
        updateSchedule.blockDay(drSophie, LocalDate.now(clock).plusDays(7), "Conges");
        updateSchedule.updateSettings(drSophie, 2, 45, CancellationMode.STANDARD_24H, false);

        log.info("""
                ================ PRDV DEMO =========================
                Patients :   alice@prdv.local  / Patient!2026demo
                Medecins :   dr.sophie@prdv.local / Medecin!2026demo
                             dr.marc@prdv.local   / Medecin!2026demo (profil non verifie)
                Admin :      admin@prdv.local     / Admin!2026demo
                ======================================================""");
    }

    private Long signup(String email, String phone, String password, Role role) {
        registerUser.register(new RegisterUserUseCase.Command(email, phone, password, role.name(), null));
        String otp = otpIssuer.issue(email); // mode log : on connait le code, on verifie direct
        verifyEmail.verify(email, otp);
        return users.findByEmail(email).orElseThrow().id();
    }

    private void signupPatient() {
        UserInfo info = registerUser.register(new RegisterUserUseCase.Command(
                "alice@prdv.local", "0612345678", "Patient!2026demo", Role.PATIENT.name(), null));
        String otp = otpIssuer.issue("alice@prdv.local");
        verifyEmail.verify("alice@prdv.local", otp);
        createPatient.create(new CreatePatientProfileUseCase.Command(info.id(), "Alice", "Durand",
                LocalDate.of(1990, 5, 15), "F", "0612345678", "3 rue des Lilas", "75011", "Paris", "France",
                "169054958815702", "MGEN", List.of("Penicilline"), List.of(), "Paul Durand", "0698765432"));
    }
}
