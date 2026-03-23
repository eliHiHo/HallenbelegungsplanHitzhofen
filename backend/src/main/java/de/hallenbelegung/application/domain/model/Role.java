package de.hallenbelegung.application.domain.model;

public enum Role {

    ADMIN,
    CLUB_REPRESENTATIVE;

    public boolean isAdmin() {
        return this == ADMIN;
    }

    public boolean isClubRepresentative() {
        return this == CLUB_REPRESENTATIVE;
    }
}