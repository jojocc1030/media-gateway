package com.gjsk.mediagateway.service;

import com.gjsk.mediagateway.common.error.UnauthorizedException;
import com.gjsk.mediagateway.config.SecurityProperties;
import com.gjsk.mediagateway.dto.auth.LoginResponse;
import com.gjsk.mediagateway.dto.auth.MeResponse;
import com.gjsk.mediagateway.security.AuthContext;
import com.gjsk.mediagateway.security.PasswordHasher;
import com.gjsk.mediagateway.security.SessionInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private SecurityProperties properties;
    private SessionService sessionService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        properties = new SecurityProperties();
        properties.getSession().setTtlSeconds(7200);

        SecurityProperties.LoginUser admin = new SecurityProperties.LoginUser();
        admin.setUserId("user001");
        admin.setUsername("admin");
        admin.setPasswordHash(PasswordHasher.hash("admin123"));
        admin.setRoles(new LinkedHashSet<>(Set.of("ADMIN", "OPERATOR")));
        properties.setUsers(List.of(admin));

        sessionService = mock(SessionService.class);
        authService = new AuthService(properties, sessionService);
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    @Test
    void loginShouldCreateSessionWithConfiguredRoles() {
        when(sessionService.createSession(any(SessionInfo.class), anyLong())).thenReturn("token001");

        LoginResponse response = authService.login("admin", "admin123");

        assertThat(response.token()).isEqualTo("token001");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(7200);

        verify(sessionService).createSession(org.mockito.ArgumentMatchers.argThat(session ->
                "user001".equals(session.getUserId())
                        && "admin".equals(session.getUsername())
                        && session.getRoles().containsAll(Set.of("ADMIN", "OPERATOR"))), eq(7200L));
    }

    @Test
    void loginShouldRejectWrongPassword() {
        assertThatThrownBy(() -> authService.login("admin", "wrong"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("用户名或密码错误");
        verifyNoInteractions(sessionService);
    }

    @Test
    void loginShouldRejectUnknownUser() {
        assertThatThrownBy(() -> authService.login("nobody", "admin123"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("用户名或密码错误");
        verifyNoInteractions(sessionService);
    }

    @Test
    void loginShouldRejectDisabledUser() {
        properties.getUsers().get(0).setEnabled(false);

        assertThatThrownBy(() -> authService.login("admin", "admin123"))
                .isInstanceOf(UnauthorizedException.class);
        verify(sessionService, never()).createSession(any(), anyLong());
    }

    @Test
    void loginShouldFallbackToUsernameWhenUserIdBlank() {
        properties.getUsers().get(0).setUserId(" ");
        when(sessionService.createSession(any(SessionInfo.class), anyLong())).thenReturn("token002");

        authService.login("admin", "admin123");

        verify(sessionService).createSession(org.mockito.ArgumentMatchers.argThat(
                session -> "admin".equals(session.getUserId())), eq(7200L));
    }

    @Test
    void currentUserShouldReturnSessionInfo() {
        SessionInfo session = new SessionInfo();
        session.setUserId("user001");
        session.setUsername("admin");
        session.setRoles(Set.of("ADMIN"));
        AuthContext.set(session);

        MeResponse response = authService.currentUser();

        assertThat(response.userId()).isEqualTo("user001");
        assertThat(response.username()).isEqualTo("admin");
        assertThat(response.roles()).containsExactly("ADMIN");
    }

    @Test
    void currentUserWithoutSessionShouldThrowUnauthorized() {
        assertThatThrownBy(() -> authService.currentUser())
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void logoutShouldRemoveSession() {
        authService.logout("token001");

        verify(sessionService).removeSessionByToken("token001");
    }
}
