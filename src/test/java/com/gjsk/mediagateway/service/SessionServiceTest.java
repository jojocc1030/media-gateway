package com.gjsk.mediagateway.service;

import com.gjsk.mediagateway.common.util.JsonUtils;
import com.gjsk.mediagateway.security.SessionInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        lenient().when(redis.opsForValue()).thenReturn(valueOperations);
        sessionService = new SessionService(redis);
    }

    @Test
    void createSessionShouldStoreJsonUnderPrefixedKey() {
        SessionInfo session = sampleSession();

        String token = sessionService.createSession(session, 60);

        assertThat(token).matches("[0-9a-f]{32}");
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture(), eq(Duration.ofSeconds(60)));
        assertThat(keyCaptor.getValue()).isEqualTo("mgw:session:" + token);
        assertThat(valueCaptor.getValue()).contains("\"userId\":\"user001\"");
    }

    @Test
    void getSessionByTokenShouldReturnNullWhenMissing() {
        when(valueOperations.get("mgw:session:missing")).thenReturn(null);

        assertThat(sessionService.getSessionByToken("missing")).isNull();
    }

    @Test
    void getSessionByTokenShouldDeserializeStoredSession() {
        when(valueOperations.get("mgw:session:token001")).thenReturn(JsonUtils.toJson(sampleSession()));

        SessionInfo restored = sessionService.getSessionByToken("token001");

        assertThat(restored).isNotNull();
        assertThat(restored.getUserId()).isEqualTo("user001");
        assertThat(restored.getRoles()).contains("ADMIN");
    }

    @Test
    void removeSessionShouldDeleteKey() {
        when(redis.delete("mgw:session:token001")).thenReturn(true);

        sessionService.removeSessionByToken("token001");

        verify(redis).delete("mgw:session:token001");
    }

    @Test
    void refreshSessionShouldExpireKey() {
        when(redis.expire("mgw:session:token001", Duration.ofSeconds(120))).thenReturn(true);

        sessionService.refreshSession("token001", 120);

        verify(redis).expire(eq("mgw:session:token001"), eq(Duration.ofSeconds(120)));
    }

    @Test
    void saveSessionShouldIgnoreBlankTokenSegmentSafety() {
        sessionService.saveSession("token with space", sampleSession(), 30);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(keyCaptor.capture(), anyString(), eq(Duration.ofSeconds(30)));
        assertThat(keyCaptor.getValue()).isEqualTo("mgw:session:token_with_space");
    }

    private SessionInfo sampleSession() {
        SessionInfo session = new SessionInfo();
        session.setUserId("user001");
        session.setUsername("admin");
        session.setRoles(Set.of("ADMIN"));
        return session;
    }
}
