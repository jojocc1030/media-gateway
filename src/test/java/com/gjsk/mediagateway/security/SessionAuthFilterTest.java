package com.gjsk.mediagateway.security;

import com.gjsk.mediagateway.config.SecurityProperties;
import com.gjsk.mediagateway.service.SessionService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SessionAuthFilterTest {

    private SessionService sessionService;
    private SessionAuthFilter filter;

    @BeforeEach
    void setUp() {
        sessionService = mock(SessionService.class);
        SecurityProperties properties = new SecurityProperties();
        properties.setPublicPaths(List.of("/api/public/**"));
        properties.getSession().setTtlSeconds(7200);
        properties.getSession().setRefreshOnAccess(true);
        filter = new SessionAuthFilter(sessionService, properties);
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    @Test
    void publicPathShouldPassWithoutToken() throws Exception {
        MockHttpServletRequest request = request("GET", "/api/public/ping");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Boolean> invoked = new AtomicReference<>(false);

        FilterChain chain = (req, res) -> invoked.set(true);
        filter.doFilter(request, response, chain);

        assertThat(invoked.get()).isTrue();
        verifyNoInteractions(sessionService);
    }

    @Test
    void missingTokenShouldReturn401() throws Exception {
        MockHttpServletRequest request = request("GET", "/api/demo/items");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            throw new AssertionError("不应进入业务链路");
        });

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(body(response)).contains("\"code\":401");
        verify(sessionService, never()).getSessionByToken(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void validTokenShouldBindAuthContextAndRefreshSession() throws Exception {
        SessionInfo session = new SessionInfo();
        session.setUserId("user001");
        session.setUsername("admin");
        session.setRoles(Set.of("ADMIN"));
        when(sessionService.getSessionByToken("token001")).thenReturn(session);

        MockHttpServletRequest request = request("GET", "/api/demo/items");
        request.addHeader("Authorization", "Bearer token001");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> insideUserId = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> insideUserId.set(AuthContext.getUserId()));

        assertThat(insideUserId.get()).isEqualTo("user001");
        assertThat(AuthContext.get()).isNull();
        verify(sessionService).refreshSession("token001", 7200);
    }

    @Test
    void invalidTokenShouldReturn401() throws Exception {
        when(sessionService.getSessionByToken("expired")).thenReturn(null);
        MockHttpServletRequest request = request("GET", "/api/demo/items");
        request.addHeader("Authorization", "Bearer expired");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            throw new AssertionError("不应进入业务链路");
        });

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(body(response)).contains("会话已失效");
    }

    @Test
    void optionsRequestShouldPass() throws Exception {
        MockHttpServletRequest request = request("OPTIONS", "/api/demo/items");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Boolean> invoked = new AtomicReference<>(false);

        filter.doFilter(request, response, (req, res) -> invoked.set(true));

        assertThat(invoked.get()).isTrue();
        verifyNoInteractions(sessionService);
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        return request;
    }

    private String body(MockHttpServletResponse response) throws Exception {
        return response.getContentAsString(StandardCharsets.UTF_8);
    }
}
