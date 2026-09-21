package com.prdv.rdv.iam.application.service.support;

import com.prdv.rdv.iam.application.port.output.DomainEventPublisher;
import com.prdv.rdv.iam.application.port.output.NotificationPort;
import com.prdv.rdv.iam.application.port.output.OtpChallengeRepository;
import com.prdv.rdv.iam.application.port.output.PasswordHasher;
import com.prdv.rdv.iam.application.port.output.RandomCodeGenerator;
import com.prdv.rdv.iam.application.result.Views;
import com.prdv.rdv.iam.config.IamProperties;
import com.prdv.rdv.iam.domain.event.DomainEvent;
import com.prdv.rdv.iam.domain.model.auth.OtpChallenge;
import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Production et verification des challenges OTP.
 * Centralise le cycle (emission, hash, envoi, comparaison constante).
 */
@Component
public class OtpIssuer {

    private static final int CODE_LENGTH = 6;

    private final RandomCodeGenerator randomCodeGenerator;
    private final PasswordHasher passwordHasher;
    private final OtpChallengeRepository challengeRepository;
    private final TransactionalStateSaver stateSaver;
    private final NotificationPort notificationPort;
    private final DomainEventPublisher eventPublisher;
    private final IamProperties properties;
    private final Clock clock;

    public OtpIssuer(RandomCodeGenerator randomCodeGenerator, PasswordHasher passwordHasher,
                     OtpChallengeRepository challengeRepository, TransactionalStateSaver stateSaver,
                     NotificationPort notificationPort,
                     DomainEventPublisher eventPublisher, IamProperties properties, Clock clock) {
        this.randomCodeGenerator = randomCodeGenerator;
        this.passwordHasher = passwordHasher;
        this.challengeRepository = challengeRepository;
        this.stateSaver = stateSaver;
        this.notificationPort = notificationPort;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
        this.clock = clock;
    }

    public Views.OtpSentView issue(String target, OtpChallenge.Channel channel, OtpChallenge.Purpose purpose) {
        String code = randomCodeGenerator.numericCode(CODE_LENGTH);
        Duration ttl = Duration.ofSeconds(properties.getSecurity().getOtp().getTtlSeconds());
        OtpChallenge challenge = OtpChallenge.issue(
                target, channel, purpose, passwordHasher.digest(code), ttl, clock);
        // Le challenge OTP doit persister meme si l'envoi de notification ou la
        // publication d'evenement echoue ensuite : on utilise stateSaver pour
        // commiter immediatement dans une transaction independante.
        stateSaver.saveOtpChallenge(challenge);

        // L'envoi de notification est best-effort : une panne SMTP/SMS ne doit
        // pas faire disparaitre ni le compte cree ni le challenge (l'utilisateur
        // peut demander un renvoi via l'endpoint /otp/resend).
        try {
            notificationPort.sendOtp(target, channel, code, purpose);
        } catch (RuntimeException notifFailure) {
            org.slf4j.LoggerFactory.getLogger(OtpIssuer.class)
                    .error("Echec d'envoi de la notification OTP vers {} : {}", target, notifFailure.getMessage());
        }

        try {
            eventPublisher.publish(new DomainEvent.OtpGenerated(target, channel, purpose, clock.instant()));
        } catch (RuntimeException eventFailure) {
            org.slf4j.LoggerFactory.getLogger(OtpIssuer.class)
                    .warn("Echec de publication d'evenement OTP : {}", eventFailure.getMessage());
        }
        return new Views.OtpSentView(target, channel.name(), purpose.name(), ttl.toSeconds());
    }

    /**
     * Verifie le dernier challenge actif d'une cible ; le consomme si valide.
     */
    public OtpChallenge verifyLatest(String target, OtpChallenge.Purpose purpose, String rawCode) {
        OtpChallenge challenge = challengeRepository.findLatestActive(target, purpose)
                .orElseThrow(() -> IamException.of(IamErrorCode.OTP_INVALID,
                        "Aucun code actif pour cette destination"));
        boolean matches = constantTimeEquals(passwordHasher.digest(rawCode), challenge.getCodeHash());
        try {
            challenge.verify(matches, clock);
        } catch (IamException e) {
            // Persiste la tentative (et la consommation au-dela du seuil) avant
            // de renvoyer l'erreur. Transaction independante pour survivre au
            // rollback provoque par l'exception.
            stateSaver.saveOtpChallenge(challenge);
            throw e;
        }
        return stateSaver.saveOtpChallenge(challenge);
    }

    /** Anti-abus : impose un delai entre deux renvois. */
    public void assertResendCooldown(String target, OtpChallenge.Purpose purpose) {
        challengeRepository.findLatestActive(target, purpose).ifPresent(last -> {
            long cooldown = properties.getSecurity().getOtp().getResendCooldownSeconds();
            if (last.getCreatedAt().plus(Duration.ofSeconds(cooldown)).isAfter(clock.instant())) {
                throw IamException.of(IamErrorCode.OTP_RATE_LIMITED,
                        "Un code a deja ete envoye, patientez " + cooldown + " secondes");
            }
        });
    }

    public Instant now() {
        return clock.instant();
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    /** Empreinte SHA-256 (utilisee par les adapteurs de tokens aleatoires). */
    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}
