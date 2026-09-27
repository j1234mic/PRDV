package com.prdv.rdv.profile.adapter.out.iam;

import com.prdv.rdv.iam.application.port.output.PractitionerProfileRepository;
import com.prdv.rdv.iam.application.port.output.UserRepository;
import com.prdv.rdv.profile.application.port.output.PractitionerVerificationPort;
import com.prdv.rdv.profile.domain.model.practitioner.BadgePolicy;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

/**
 * Adapteur : etat des verifications reglementaires (RPPS/ADELI, diplomes,
 * assurance RC pro, RIB) et anciennete du compte, detenus par le contexte IAM.
 * Alimente la {@link BadgePolicy} (badge « verifie », badge « nouveau »).
 */
@Component
public class IamPractitionerVerificationAdapter implements PractitionerVerificationPort {

    private final PractitionerProfileRepository practitionerProfiles;
    private final UserRepository userRepository;

    public IamPractitionerVerificationAdapter(PractitionerProfileRepository practitionerProfiles,
                                              UserRepository userRepository) {
        this.practitionerProfiles = practitionerProfiles;
        this.userRepository = userRepository;
    }

    @Override
    public BadgePolicy.VerificationState verificationOf(Long practitionerUserId) {
        return practitionerProfiles.findByUserId(practitionerUserId)
                .map(profile -> new BadgePolicy.VerificationState(
                        profile.isRppsVerified() || profile.isAdeliVerified(),
                        profile.isDiplomaVerified(),
                        profile.isProfessionalInsuranceVerified(),
                        profile.isBankAccountVerified()))
                .orElseGet(BadgePolicy.VerificationState::none);
    }

    @Override
    public Optional<Instant> registeredAt(Long practitionerUserId) {
        return userRepository.findById(practitionerUserId).map(user -> user.getCreatedAt());
    }
}
