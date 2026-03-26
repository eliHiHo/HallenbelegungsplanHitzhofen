package de.hallenbelegung.adapters.in.api.dto;

public record ResetPasswordRequestDTO(
        String token,
        String newPassword
) {
}
