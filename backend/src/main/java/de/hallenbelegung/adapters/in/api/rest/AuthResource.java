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
import de.hallenbelegung.application.domain.model.User;
import de.hallenbelegung.application.domain.port.in.GetCurrentUserUseCase;
import de.hallenbelegung.application.domain.port.in.LoginUseCase;
import de.hallenbelegung.application.domain.port.in.LogoutUseCase;
import de.hallenbelegung.application.domain.port.in.RequestPasswordResetUseCase;
import de.hallenbelegung.application.domain.port.in.ResetPasswordUseCase;
import de.hallenbelegung.application.domain.view.AuthSessionView;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    @org.eclipse.microprofile.config.inject.ConfigProperty(
            name = "app.session.cookie.secure", defaultValue = "true")
    boolean sessionCookieSecure;

    private static final String SESSION_COOKIE_NAME = "HB_SESSION";
    private final LoginUseCase loginUseCase;
    private final LogoutUseCase logoutUseCase;
    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final RequestPasswordResetUseCase requestPasswordResetUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final AuthApiMapper authApiMapper;
    private final UserApiMapper userApiMapper;

    public AuthResource(
            LoginUseCase loginUseCase,
            LogoutUseCase logoutUseCase,
            GetCurrentUserUseCase getCurrentUserUseCase,
            RequestPasswordResetUseCase requestPasswordResetUseCase,
            ResetPasswordUseCase resetPasswordUseCase,
            AuthApiMapper authApiMapper,
            UserApiMapper userApiMapper
    ) {
        this.loginUseCase = loginUseCase;
        this.logoutUseCase = logoutUseCase;
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.requestPasswordResetUseCase = requestPasswordResetUseCase;
        this.resetPasswordUseCase = resetPasswordUseCase;
        this.authApiMapper = authApiMapper;
        this.userApiMapper = userApiMapper;
    }

    @POST
    @Path("/login")
    public Response login(LoginRequestDTO request) {
        if (request == null) {
            throw new ValidationException("Request body is required");
        }
        if (request.email() == null || request.email().isBlank()) {
            throw new ValidationException("email is required");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new ValidationException("password is required");
        }

        AuthSessionView sessionView = loginUseCase.login(request.email(), request.password());
        LoginResponseDTO responseDto = authApiMapper.toLoginResponse(sessionView);

        return Response.ok(responseDto)
                .cookie(createSessionCookie(sessionView.getSessionId()))
                .build();
    }

    @POST
    @Path("/logout")
    public Response logout(@CookieParam(SESSION_COOKIE_NAME) String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            logoutUseCase.logout(sessionId);
        }

        return Response.ok(new EmptyResponseDTO())
                .cookie(deleteSessionCookie())
                .build();
    }

    @GET
    @Path("/me")
    public Response me(@CookieParam(SESSION_COOKIE_NAME) String sessionId) {
        User currentUser = getCurrentUserUseCase.getCurrentUser(requireSessionId(sessionId));
        UserDTO responseDto = userApiMapper.toDTO(currentUser);
        return Response.ok(responseDto).build();
    }

    @POST
    @Path("/forgot-password")
    public Response forgotPassword(ForgotPasswordRequestDTO request) {
        if (request == null) {
            throw new ValidationException("Request body is required");
        }
        if (request.email() == null || request.email().isBlank()) {
            throw new ValidationException("email is required");
        }
        requestPasswordResetUseCase.requestPasswordReset(request.email());
        return Response.ok(new EmptyResponseDTO()).build();
    }

    @POST
    @Path("/reset-password")
    public Response resetPassword(ResetPasswordRequestDTO request) {
        if (request == null) {
            throw new ValidationException("Request body is required");
        }
        if (request.token() == null || request.token().isBlank()) {
            throw new ValidationException("token is required");
        }
        if (request.newPassword() == null || request.newPassword().isBlank()) {
            throw new ValidationException("newPassword is required");
        }
        resetPasswordUseCase.resetPassword(request.token(), request.newPassword());
        return Response.ok(new EmptyResponseDTO()).build();
    }

    private String requireSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new UnauthorizedException("Missing session cookie");
        }
        return sessionId;
    }

    private NewCookie createSessionCookie(String sessionId) {
        return new NewCookie.Builder(SESSION_COOKIE_NAME)
                .value(sessionId)
                .path("/")
                .httpOnly(true)
                .secure(sessionCookieSecure)
                .sameSite(NewCookie.SameSite.LAX)
                .build();
    }

    private NewCookie deleteSessionCookie() {
        return new NewCookie.Builder(SESSION_COOKIE_NAME)
                .value("")
                .path("/")
                .httpOnly(true)
                .secure(sessionCookieSecure)
                .sameSite(NewCookie.SameSite.LAX)
                .maxAge(0)
                .build();
    }
}