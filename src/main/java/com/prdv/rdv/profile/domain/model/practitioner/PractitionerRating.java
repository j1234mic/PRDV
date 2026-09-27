package com.prdv.rdv.profile.domain.model.practitioner;

import com.prdv.rdv.profile.domain.exception.ProfileErrorCode;
import com.prdv.rdv.profile.domain.exception.ProfileException;
import lombok.Getter;
import lombok.Setter;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Avis patient sur un praticien (module 2.2 — Visibilite &amp; Marketing).
 *
 * <p>Un patient ne peut noter un praticien qu'une seule fois : l'unicite est
 * garantie par la couche application (requete sur le couple
 * praticien/patient) puis verifiee ici. La moderation (signalement,
 * masquage) est tracee.
 */
@Getter
@Setter
public class PractitionerRating {

    public static final int MIN_SCORE = 1;
    public static final int MAX_SCORE = 5;
    public static final int MAX_COMMENT_CHARS = 2_000;

    private String id;
    private Long practitionerUserId;
    private Long patientUserId;
    private int score;
    private String comment;
    private Instant createdAt;
    private boolean hidden;
    private String moderationNote;
    private Instant moderatedAt;

    public static PractitionerRating submit(Long practitionerUserId, Long patientUserId, int score,
                                            String comment, Clock clock) {
        if (practitionerUserId == null || patientUserId == null) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "Un avis exige un praticien et un patient");
        }
        if (practitionerUserId.equals(patientUserId)) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "Un praticien ne peut pas se noter lui-meme");
        }
        if (score < MIN_SCORE || score > MAX_SCORE) {
            throw ProfileException.of(ProfileErrorCode.RATING_OUT_OF_RANGE,
                    "La note doit etre comprise entre " + MIN_SCORE + " et " + MAX_SCORE);
        }
        if (comment != null && comment.length() > MAX_COMMENT_CHARS) {
            throw ProfileException.of(ProfileErrorCode.VALIDATION_ERROR,
                    "Le commentaire est limite a " + MAX_COMMENT_CHARS + " caracteres");
        }
        PractitionerRating rating = new PractitionerRating();
        rating.id = UUID.randomUUID().toString();
        rating.practitionerUserId = practitionerUserId;
        rating.patientUserId = patientUserId;
        rating.score = score;
        rating.comment = comment == null || comment.isBlank() ? null : comment.trim();
        rating.createdAt = clock.instant();
        return rating;
    }

    /** Masque l'avis apres moderation (propos injurieux, conflit d'interet...). */
    public void hide(String note, Clock clock) {
        this.hidden = true;
        this.moderationNote = note;
        this.moderatedAt = clock.instant();
    }

    public void restore(Clock clock) {
        this.hidden = false;
        this.moderationNote = null;
        this.moderatedAt = clock.instant();
    }
}
