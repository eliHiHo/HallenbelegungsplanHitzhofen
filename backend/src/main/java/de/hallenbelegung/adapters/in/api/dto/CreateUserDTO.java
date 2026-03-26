package de.hallenbelegung.adapters.in.api.dto;

public record CreateUserDTO(
        String firstName,
        String lastName,
        String email,
        String password,
        String role
) {
}
