package de.hallenbelegung.application.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class User {

    private final UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String passwordHash;
    private Role role;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;

    public User(
            UUID id,
            String firstName,
            String lastName,
            String email,
            String passwordHash,
            Role role,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.firstName = Objects.requireNonNull(firstName);
        this.lastName = Objects.requireNonNull(lastName);
        this.email = Objects.requireNonNull(email);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.role = Objects.requireNonNull(role);
        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static User createNew(
            String firstName,
            String lastName,
            String email,
            String passwordHash,
            Role role
    ) {
        Instant now = Instant.now();

        return new User(
                null,
                firstName,
                lastName,
                email,
                passwordHash,
                role,
                true,
                now,
                now
        );
    }

    public UUID getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isClubRepresentative() {
        return role == Role.CLUB_REPRESENTATIVE;
    }

    public void updateProfile(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.updatedAt = Instant.now();
    }

    public void changeRole(Role role) {
        this.role = role;
        this.updatedAt = Instant.now();
    }

    public void changePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.active = true;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = Instant.now();
    }
}
