package com.gjsk.mediagateway.common.util;

import com.gjsk.mediagateway.security.SessionInfo;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JsonUtilsTest {

    @Test
    void shouldReturnNullForNullInput() {
        assertThat(JsonUtils.toJson(null)).isNull();
        assertThat(JsonUtils.fromJson(null, Map.class)).isNull();
        assertThat(JsonUtils.fromJson("   ", Map.class)).isNull();
    }

    @Test
    void shouldRoundTripSessionInfo() {
        SessionInfo session = new SessionInfo();
        session.setUserId("user001");
        session.setUsername("admin");
        session.setRoles(Set.of("ADMIN", "OPERATOR"));

        String json = JsonUtils.toJson(session);
        SessionInfo restored = JsonUtils.fromJson(json, SessionInfo.class);

        assertThat(restored.getUserId()).isEqualTo("user001");
        assertThat(restored.getUsername()).isEqualTo("admin");
        assertThat(restored.getRoles()).containsExactlyInAnyOrder("ADMIN", "OPERATOR");
    }

    @Test
    void shouldIgnoreUnknownProperties() {
        Map<String, Object> map = JsonUtils.toMap("{\"known\":1,\"unknown\":2}");

        assertThat(map).containsEntry("known", 1).containsEntry("unknown", 2);
    }

    @Test
    void shouldSerializeInstantAsIsoString() {
        Instant instant = Instant.parse("2026-01-01T00:00:00Z");

        String json = JsonUtils.toJson(instant);

        assertThat(json).isEqualTo("\"2026-01-01T00:00:00Z\"");
    }
}
