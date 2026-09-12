package com.prdv.rdv.iam.application.service.support;

import com.prdv.rdv.iam.application.command.RequestMetadata;
import com.prdv.rdv.iam.application.port.output.PasswordHasher;
import com.prdv.rdv.iam.application.port.output.RoleRepository;
import com.prdv.rdv.iam.application.port.output.UserRepository;
import com.prdv.rdv.iam.application.result.Views;
import com.prdv.rdv.iam.domain.model.auth.OtpChallenge;
import com.prdv.rdv.iam.domain.model.rbac.Role;
import com.prdv.rdv.iam.domain.model.user.AccountStatus;
import com.prdv.rdv.iam.domain.model.user.Email;
import com.prdv.rdv.iam.domain.model.user.PasswordPolicy;
import com.prdv.rdv.iam.domain.model.user.PhoneNumber;
import com.prdv.rdv.iam.domain.model.user.ProfileType;
import com.prdv.rdv.iam.domain.model.user.User;
import com.prdv.rdv.iam.domain.exception.IamErrorCode;
import com.prdv.rdv.iam.domain.exception.IamException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Fabrique commune des inscriptions : unicite, robustesse du mot de passe,
 * hachage, role par defaut et OTP. Isole la logique partagee par les
 * 4 cas d'usage d'inscription (DRY, Single Responsibility).
 */
@Component
public class RegistrationSupport {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordHasher passwordHasher;
    private final OtpIssuer otpIssuer;
    private final Clock clock;

    public RegistrationSupport(UserRepository userRepository, RoleRepository roleRepository,
                               PasswordHasher passwordHasher, OtpIssuer otpIssuer, Clock clock) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordHasher = passwordHasher;
        this.otpIssuer = otpIssuer;
        this.clock = clock;
    }

    @Transactional
    public User createAccount(String rawEmail, String rawPhone, String rawPassword,
                              ProfileType profileType, String defaultRole) {
        Email email = Email.of(rawEmail);
        PhoneNumber phone = rawPhone == null || rawPhone.isBlank() ? null : PhoneNumber.of(rawPhone);
        PasswordPolicy.validate(rawPassword);

        if (userRepository.existsByEmail(email.value())) {
            throw IamException.of(IamErrorCode.EMAIL_ALREADY_EXISTS,
                    "Un compte existe deja avec cet email");
        }
        if (phone != null && userRepository.existsByPhone(phone.value())) {
            throw IamException.of(IamErrorCode.PHONE_ALREADY_EXISTS,
                    "Un compte existe deja avec ce numero de telephone");
        }

        User user = User.register(email, phone, passwordHasher.hash(rawPassword), profileType, clock);
        user = userRepository.save(user);

        Role role = roleRepository.findByName(defaultRole)
                .orElseThrow(() -> new IllegalStateException("Role amorce manquant : " + defaultRole));
        user.addRole(role);
        return userRepository.save(user);
    }

    /** Declenche l'OTP d'inscription sur le canal email (ou SMS si pas d'email). */
    public Views.OtpSentView sendContactOtp(User user) {
        OtpChallenge.Channel channel = OtpChallenge.Channel.EMAIL;
        String target = user.getEmail();
        if (target == null || target.isBlank()) {
            channel = OtpChallenge.Channel.SMS;
            target = user.getPhone();
        }
        return otpIssuer.issue(target, channel, OtpChallenge.Purpose.REGISTRATION);
    }

    /** Profils soumis a validation manuelle apres confirmation OTP. */
    public static AccountStatus statusAfterOtp(ProfileType type) {
        return switch (type) {
            case PRACTITIONER, ESTABLISHMENT -> AccountStatus.PENDING_VALIDATION;
            case PATIENT, SECRETARY, ADMIN -> AccountStatus.ACTIVE;
        };
    }

    public RequestMetadata unknownMetadata() {
        return RequestMetadata.unknown();
    }
}
