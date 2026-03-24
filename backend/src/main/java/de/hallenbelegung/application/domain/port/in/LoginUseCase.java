package de.hallenbelegung.application.domain.port.in;

import de.hallenbelegung.application.domain.view.AuthSessionView;

public interface LoginUseCase {
    AuthSessionView login(String email, String password);
}