package de.hallenbelegung.adapters.in.api.rest;

import de.hallenbelegung.adapters.in.api.dto.EmptyResponseDTO;
import de.hallenbelegung.adapters.in.api.dto.ForgotPasswordRequestDTO;
import de.hallenbelegung.adapters.in.api.dto.LoginRequestDTO;
import de.hallenbelegung.adapters.in.api.dto.LoginResponseDTO;
import de.hallenbelegung.adapters.in.api.dto.ResetPasswordRequestDTO;
import de.hallenbelegung.adapters.in.api.dto.UserDTO;
import de.hallenbelegung.adapters.in.api.mapper.AuthApiMapper;
import de.hallenbelegung.adapters.in.api.mapper.UserApiMapper;
import de.hallenbelegung.application.domain.exception.UnauthorizedException;
import de.hallenbelegung.application.domain.exception.ValidationException;
import de.hallenbelegung.application.domain.model.Role;
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.in.GetCurrentUserUseCase;
import de.hallenbelegung.application.domain.port.in.LoginUseCase;
import de.hallenbelegung.application.domain.port.in.LogoutUseCase;
import de.hallenbelegung.application.domain.port.in.RequestPasswordResetUseCase;
import de.hallenbelegung.application.domain.port.in.ResetPasswordUseCase;
import de.hallenbelegung.application.domain.view.AuthSessionView;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthResourceTest {

    private LoginUseCase loginUseCase;
    private LogoutUseCase logoutUseCase;
    private GetCurrentUserUseCase getCurrentUserUseCase;
    private RequestPasswordResetUseCase requestPasswordResetUseCase;
    private ResetPasswordUseCase resetPasswordUseCase;

    private AuthResource resource;

    @BeforeEach
    void setUp() {
        loginUseCase = mock(LoginUseCase.class);
        logoutUseCase = mock(LogoutUseCase.class);
        getCurrentUserUseCase = mock(GetCurrentUserUseCase.class);
        requestPasswordResetUseCase = mock(RequestPasswordResetUseCase.class);
        resetPasswordUseCase = mock(ResetPasswordUseCase.class);

        resource = new AuthResource(
                loginUseCase,
                logoutUseCase,
                getCurrentUserUseCase,
                requestPasswordResetUseCase,
                resetPasswordUseCase,
                new AuthApiMapper(),
                new UserApiMapper()
        );
        resource.sessionCookieSecure = false;
    }

    @Test
    void login_returns_response_and_session_cookie() {
        UUID userId = UUID.randomUUID();
        AuthSessionView view = new AuthSessionView("session-token", userId, "Max", "Mustermann", "max@example.com", Role.ADMIN);

        when(loginUseCase.login("max@example.com", "secret")).thenReturn(view);

        Response response = resource.login(new LoginRequestDTO("max@example.com", "secret"));

        assertEquals(200, response.getStatus());
        assertInstanceOf(LoginResponseDTO.class, response.getEntity());

        LoginResponseDTO dto = (LoginResponseDTO) response.getEntity();
        assertEquals("session-token", dto.token());
        assertEquals(userId, dto.userId());

        NewCookie cookie = response.getCookies().get("HB_SESSION");
        assertEquals("session-token", cookie.getValue());
        assertEquals("/", cookie.getPath());
        assertTrue(!cookie.isSecure());
    }

    @Test
    void login_rejects_missing_body_or_fields() {
        ValidationException ex1 = assertThrows(ValidationException.class, () -> resource.login(null));
        assertEquals("Request body is required", ex1.getMessage());

        ValidationException ex2 = assertThrows(ValidationException.class, () -> resource.login(new LoginRequestDTO(" ", "x")));
        assertEquals("email is required", ex2.getMessage());

        ValidationException ex3 = assertThrows(ValidationException.class, () -> resource.login(new LoginRequestDTO("a@b.c", " ")));
        assertEquals("password is required", ex3.getMessage());
    }

    @Test
    void logout_invalidates_session_if_cookie_present_and_always_deletes_cookie() {
        Response response = resource.logout("abc");

        assertEquals(200, response.getStatus());
        assertInstanceOf(EmptyResponseDTO.class, response.getEntity());
        verify(logoutUseCase).logout("abc");

        NewCookie cookie = response.getCookies().get("HB_SESSION");
        assertEquals("", cookie.getValue());
        assertEquals(0, cookie.getMaxAge());
    }

    @Test
    void logout_skips_invalidation_for_blank_cookie() {
        resource.logout("   ");

        verify(logoutUseCase, never()).logout("   ");
    }

    @Test
    void me_requires_cookie_and_maps_current_user() {
        UUID userId = UUID.randomUUID();
        User user = new User(
                userId,
                "Max",
                "Mustermann",
                "max@example.com",
                "hash",
                Role.CLUB_REPRESENTATIVE,
                true,
                Instant.now(),
                Instant.now()
        );

        when(getCurrentUserUseCase.getCurrentUser("sess")).thenReturn(user);

        Response response = resource.me("sess");

        assertEquals(200, response.getStatus());
        assertInstanceOf(UserDTO.class, response.getEntity());
        UserDTO dto = (UserDTO) response.getEntity();
        assertEquals(userId, dto.id());
        assertEquals("Max Mustermann", dto.fullName());

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> resource.me("  "));
        assertEquals("Missing session cookie", ex.getMessage());
    }

    @Test
    void forgotPassword_validates_and_calls_use_case() {
        ValidationException ex1 = assertThrows(ValidationException.class, () -> resource.forgotPassword(null));
        assertEquals("Request body is required", ex1.getMessage());

        ValidationException ex2 = assertThrows(
                ValidationException.class,
                () -> resource.forgotPassword(new ForgotPasswordRequestDTO("   "))
        );
        assertEquals("email is required", ex2.getMessage());

        Response response = resource.forgotPassword(new ForgotPasswordRequestDTO("max@example.com"));
        assertEquals(200, response.getStatus());
        assertInstanceOf(EmptyResponseDTO.class, response.getEntity());
        verify(requestPasswordResetUseCase).requestPasswordReset("max@example.com");
    }

    @Test
    void resetPassword_validates_and_calls_use_case() {
        ValidationException ex1 = assertThrows(ValidationException.class, () -> resource.resetPassword(null));
        assertEquals("Request body is required", ex1.getMessage());

        ValidationException ex2 = assertThrows(
                ValidationException.class,
                () -> resource.resetPassword(new ResetPasswordRequestDTO(" ", "secret"))
        );
        assertEquals("token is required", ex2.getMessage());

        ValidationException ex3 = assertThrows(
                ValidationException.class,
                () -> resource.resetPassword(new ResetPasswordRequestDTO("token", " "))
        );
        assertEquals("newPassword is required", ex3.getMessage());

        Response response = resource.resetPassword(new ResetPasswordRequestDTO("token", "secret"));
        assertEquals(200, response.getStatus());
        assertInstanceOf(EmptyResponseDTO.class, response.getEntity());
        verify(resetPasswordUseCase).resetPassword("token", "secret");
    }
}

