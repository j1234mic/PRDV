package com.prdv.profile.domain.model;

/** Lieu d'exercice du praticien (multi-cabinets - module 2.2). Serialise en JSON dans la BDD (adaptateur). */
public record PracticeLocation(String name, String address, String postalCode, String city, String phone) {
}
