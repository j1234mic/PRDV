package com.prdv.rdv.iam.application.port.output;

import com.prdv.rdv.iam.domain.model.auth.OtpChallenge;

import java.util.Optional;

public interface OtpChallengeRepository {

    OtpChallenge save(OtpChallenge challenge);

    /** Dernier challenge non consomme pour une cible et un usage donnes. */
    Optional<OtpChallenge> findLatestActive(String target, OtpChallenge.Purpose purpose);
}
