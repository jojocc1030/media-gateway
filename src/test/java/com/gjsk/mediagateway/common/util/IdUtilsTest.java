package com.gjsk.mediagateway.common.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class IdUtilsTest {

    private static final Pattern ID_PATTERN = Pattern.compile("[0-9a-f]{32}");

    @Test
    void newIdShouldFollowUuidV7Layout() {
        String id = IdUtils.newId();

        assertThat(id).matches(ID_PATTERN);
        assertThat(id.charAt(12)).isEqualTo('7');
        assertThat("89ab").contains(String.valueOf(id.charAt(16)));
    }

    @Test
    void newIdTimestampShouldBeCloseToNow() {
        long before = System.currentTimeMillis();
        String id = IdUtils.newId();
        long after = System.currentTimeMillis();

        long timestamp = Long.parseLong(id.substring(0, 12), 16);

        assertThat(timestamp).isBetween(before - 1, after + 1);
    }

    @Test
    void newIdShouldBeUnique() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            ids.add(IdUtils.newId());
        }

        assertThat(ids).hasSize(10_000);
    }

    @Test
    void newTokenShouldBeRandom128BitHex() {
        String first = IdUtils.newToken();
        String second = IdUtils.newToken();

        assertThat(first).matches(ID_PATTERN);
        assertThat(second).matches(ID_PATTERN);
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void newRequestIdShouldBeUuidV7() {
        assertThat(IdUtils.newRequestId()).matches(ID_PATTERN);
    }
}
