package com.prdv.rdv.profile.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Avis patient sur un praticien (un seul avis par couple patient/praticien). */
@Entity
@Table(name = "profile_practitioner_ratings", indexes = {
        @Index(name = "idx_rating_practitioner", columnList = "practitioner_user_id"),
        @Index(name = "idx_rating_unique_patient", columnList = "practitioner_user_id,patient_user_id",
                unique = true)
})
@Getter
@Setter
public class PractitionerRatingEntity {

    @Id
    @Column(length = 40)
    private String id;

    @Column(name = "practitioner_user_id", nullable = false)
    private Long practitionerUserId;

    @Column(name = "patient_user_id", nullable = false)
    private Long patientUserId;

    @Column(nullable = false)
    private int score;

    @Lob
    @Column(name = "comment_text")
    private String comment;

    private Instant createdAt;

    private boolean hidden;

    @Column(length = 500)
    private String moderationNote;

    private Instant moderatedAt;
}
