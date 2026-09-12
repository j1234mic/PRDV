package com.prdv.rdv.iam.adapter.out.persistence.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Geolocalisation embarquee dans la session (pays / ville / coordonnees).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GeoLocationEmbeddable {

    private String country;
    private String city;
    private Double latitude;
    private Double longitude;
}
