package com.prdv.rdv.profile.adapter.out.persistence.entity;

import com.prdv.rdv.profile.adapter.out.persistence.converter.JsonConverters;
import com.prdv.rdv.profile.domain.model.practitioner.PracticeLocation;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Lieu d'exercice d'un praticien (multi-cabinets). */
@Entity
@Table(name = "profile_practice_locations", indexes = {
        @Index(name = "idx_location_practitioner", columnList = "practitioner_user_id"),
        @Index(name = "idx_location_city", columnList = "city")
})
@Getter
@Setter
public class PracticeLocationEntity {

    @Id
    @Column(length = 40)
    private String id;

    @Column(name = "practitioner_user_id", nullable = false)
    private Long practitionerUserId;

    @Column(length = 120)
    private String name;

    private boolean mainLocation;

    @Column(length = 255)
    private String line1;

    @Column(length = 255)
    private String line2;

    @Column(length = 12)
    private String postalCode;

    @Column(length = 120)
    private String city;

    @Column(length = 3)
    private String country;

    private Double latitude;

    private Double longitude;

    @Lob
    @Convert(converter = JsonConverters.OpeningHoursList.class)
    @Column(name = "opening_hours")
    private List<PracticeLocation.OpeningHours> openingHours = new ArrayList<>();

    @Lob
    @Convert(converter = JsonConverters.LocationPhotoList.class)
    @Column(name = "photos")
    private List<PracticeLocation.Photo> photos = new ArrayList<>();

    @Lob
    @Convert(converter = JsonConverters.SocialLinkMap.class)
    @Column(name = "social_links")
    private Map<String, String> socialLinks = new LinkedHashMap<>();

    @Column(length = 500)
    private String virtualTourUrl;

    @Column(length = 30)
    private String phone;

    @Column(length = 30)
    private String mobilePhone;

    @Column(length = 30)
    private String fax;

    @Column(length = 160)
    private String email;

    @Column(length = 255)
    private String website;

    private boolean wheelchairAccessible;

    private boolean parkingAvailable;

    @Column(length = 500)
    private String publicTransportInfo;

    private Instant createdAt;

    private Instant updatedAt;
}
