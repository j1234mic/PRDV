package com.prdv.config;

import com.prdv.adapters.out.security.JwtProperties;
import com.prdv.events.SpringDomainEventPublisher;
import com.prdv.identity.application.port.in.*;
import com.prdv.identity.application.port.out.*;
import com.prdv.identity.application.service.*;
import com.prdv.identity.domain.policy.AccountLockoutPolicy;
import com.prdv.notification.application.service.AppointmentEventHandler;
import com.prdv.notification.application.service.NotificationService;
import com.prdv.notification.application.port.out.AuditLog;
import com.prdv.notification.application.port.out.NotificationGateway;
import com.prdv.profile.application.port.in.*;
import com.prdv.profile.application.port.out.*;
import com.prdv.profile.application.service.*;
import com.prdv.schedule.application.port.in.*;
import com.prdv.schedule.application.port.out.*;
import com.prdv.schedule.application.service.*;
import com.prdv.schedule.domain.service.SlotGenerator;
import com.prdv.shared.event.DomainEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

/**
 * COMPOSITION ROOT (module d'assemblage).
 *
 * Pourquoi tout est cable ICI : le coeur (prdv-core) reste independant des frameworks ;
 * ce fichier est le SEUL endroit qui connait a la fois les cas d'usage et les
 * adaptateurs. L'equivalent de ce qu'un conteneur IoC ferait, ecrit comme un
 * test d'architecture : changer d'ORM/de mailing ne touche que ce fichier +
 * l'adaptateur retire.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class CoreBeansConfig {

    // ----- services transverses ------------------------------------------------

    @Bean
    Clock prdvClock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    DomainEventPublisher domainEventPublisher(ApplicationEventPublisher publisher) {
        return new SpringDomainEventPublisher(publisher);
    }

    @Bean
    AccountLockoutPolicy accountLockoutPolicy(
            @Value("${prdv.security.max-failed-attempts:5}") int maxAttempts,
            @Value("${prdv.security.lock-duration:PT15M}") Duration lockDuration) {
        return new AccountLockoutPolicy(maxAttempts, lockDuration);
    }

    @Bean
    OtpIssuer otpIssuer(OtpStore store, OtpSender sender,
                        @Value("${prdv.otp.validity:PT10M}") Duration validity, Clock clock) {
        return new OtpIssuer(store, sender, validity, clock);
    }

    @Bean
    SlotGenerator slotGenerator(Clock clock) {
        return new SlotGenerator(clock);
    }

    @Bean
    NotificationService notificationService(NotificationGateway gateway, UserRepository users) {
        return new NotificationService(gateway, users);
    }

    // ----- module 1 : authentification ----------------------------------------

    @Bean
    RegisterUserUseCase registerUserUseCase(UserRepository users, PasswordHasher hasher,
                                            OtpIssuer otpIssuer, Clock clock) {
        return new RegisterUserHandler(users, hasher, otpIssuer, clock);
    }

    @Bean
    VerifyEmailUseCase verifyEmailUseCase(UserRepository users, OtpIssuer otpIssuer,
                                          DomainEventPublisher events) {
        return new VerifyEmailHandler(users, otpIssuer, events);
    }

    @Bean
    LoginUseCase loginUseCase(UserRepository users, PasswordHasher hasher, TokenIssuer tokens,
                               AccountLockoutPolicy lockout, DomainEventPublisher events, Clock clock) {
        return new LoginHandler(users, hasher, tokens, lockout, events, clock);
    }

    @Bean
    RefreshTokenUseCase refreshTokenUseCase(TokenVerifier verifier, TokenIssuer issuer,
                                            UserRepository users, Clock clock) {
        return new RefreshTokenHandler(verifier, issuer, users, clock);
    }

    // ----- module 2 : profils ---------------------------------------------------

    @Bean
    CreatePatientProfileUseCase createPatientProfileUseCase(PatientProfileRepository patients) {
        return new PatientProfileHandler(patients);
    }

    @Bean
    UpdateMedicalFactsUseCase updateMedicalFactsUseCase(PatientProfileRepository patients) {
        return new PatientProfileHandler(patients);
    }

    @Bean
    CreateDoctorProfileUseCase createDoctorProfileUseCase(DoctorProfileRepository doctors,
                                                           MedicalRegistryGateway registry) {
        return new DoctorProfileHandler(doctors, registry);
    }

    @Bean
    UpdateDoctorProfileUseCase updateDoctorProfileUseCase(DoctorProfileRepository doctors,
                                                           MedicalRegistryGateway registry) {
        return new DoctorProfileHandler(doctors, registry);
    }

    @Bean
    ValidateDoctorProfileUseCase validateDoctorProfileUseCase(DoctorProfileRepository doctors,
                                                                DomainEventPublisher events) {
        return new DoctorValidationHandler(doctors, events);
    }

    @Bean
    DoctorDirectoryUseCase doctorDirectoryUseCase(DoctorProfileRepository doctors) {
        return new DoctorDirectoryHandler(doctors);
    }

    // ----- module 4 : agenda & rendez-vous ---------------------------------------

    @Bean
    UpdateScheduleUseCase updateScheduleUseCase(DoctorScheduleRepository schedules) {
        return new ScheduleHandler(schedules);
    }

    @Bean
    FindSlotsUseCase findSlotsUseCase(DoctorScheduleRepository schedules, AppointmentRepository appointments,
                                      SlotGenerator generator) {
        return new FindSlotsHandler(schedules, appointments, generator);
    }

    @Bean
    BookAppointmentUseCase bookAppointmentUseCase(DoctorScheduleRepository schedules,
                                                   AppointmentRepository appointments,
                                                   WaitingListRepository waitingList,
                                                   DoctorProfileRepository doctors,
                                                   PatientProfileRepository patients,
                                                   DomainEventPublisher events, Clock clock) {
        return new BookingHandler(schedules, appointments, waitingList, doctors, patients, events, clock);
    }

    @Bean
    ManageAppointmentUseCase manageAppointmentUseCase(AppointmentRepository appointments,
                                                      DoctorScheduleRepository schedules,
                                                      DomainEventPublisher events, Clock clock) {
        return new AppointmentManagementHandler(appointments, schedules, events, clock);
    }

    @Bean
    WaitingListUseCase waitingListUseCase(WaitingListRepository waitingList, Clock clock) {
        return new WaitingListHandler(waitingList, clock);
    }

    @Bean
    GetAppointmentsUseCase getAppointmentsUseCase(AppointmentRepository appointments) {
        return new AppointmentQueryHandler(appointments);
    }

    @Bean
    GetScheduleUseCase getScheduleUseCase(DoctorScheduleRepository schedules) {
        return new ScheduleHandler(schedules);
    }

    // ----- observateurs (modules 4.3 + 5.1) --------------------------------------

    @Bean
    AppointmentEventHandler appointmentEventHandler(NotificationService notifications, AuditLog audit) {
        return new AppointmentEventHandler(notifications, audit);
    }

    @Bean
    WaitlistAutoAssignHandler waitlistAutoAssignHandler(WaitingListRepository waitingList,
                                                       AppointmentRepository appointments,
                                                       DoctorScheduleRepository schedules,
                                                       DomainEventPublisher events,
                                                       NotificationService notifications, Clock clock) {
        return new WaitlistAutoAssignHandler(waitingList, appointments, schedules, events, notifications, clock);
    }
}
