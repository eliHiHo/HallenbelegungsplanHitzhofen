package de.hallenbelegung.application.domain.port.in;
public interface LoginUseCase {

    AuthTokenDto login(String email, String password);
}