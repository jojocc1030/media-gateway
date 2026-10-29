package com.gjsk.mediagateway.common.idempotent;

import com.gjsk.mediagateway.common.error.BadRequestException;
import com.gjsk.mediagateway.common.error.ConflictException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotentInterceptorTest {

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private IdempotentInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new IdempotentInterceptor(redis);
    }

    @AfterEach
    void tearDown() {
        com.gjsk.mediagateway.security.AuthContext.clear();
    }

    @Test
    void methodWithoutAnnotationShouldPassWithoutRedis() throws Exception {
        MockHttpServletRequest request = request(null);
        HandlerMethod handler = handler("open");

        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), handler)).isTrue();
        verifyNoInteractions(redis);
    }

    @Test
    void missingIdempotencyKeyShouldReturnBadRequest() throws Exception {
        MockHttpServletRequest request = request(null);
        HandlerMethod handler = handler("create");

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), handler))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(IdempotentInterceptor.HEADER);
    }

    @Test
    void malformedIdempotencyKeyShouldReturnBadRequest() throws Exception {
        MockHttpServletRequest request = request("short");
        HandlerMethod handler = handler("create");

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), handler))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void firstRequestShouldAcquireKey() throws Exception {
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), eq("1"), eq(Duration.ofSeconds(120)))).thenReturn(true);
        MockHttpServletRequest request = request("key-12345678");
        HandlerMethod handler = handler("create");

        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), handler)).isTrue();
        verify(valueOperations).setIfAbsent(
                eq("mgw:idem:anonymous:POST:_api_demo_items:key-12345678"), eq("1"), eq(Duration.ofSeconds(120)));
    }

    @Test
    void duplicateRequestShouldConflict() throws Exception {
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), eq("1"), eq(Duration.ofSeconds(120)))).thenReturn(false);
        MockHttpServletRequest request = request("key-12345678");
        HandlerMethod handler = handler("create");

        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), handler))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("重复请求");
    }

    private MockHttpServletRequest request(String idempotencyKey) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/demo/items");
        request.setServletPath("/api/demo/items");
        if (idempotencyKey != null) {
            request.addHeader(IdempotentInterceptor.HEADER, idempotencyKey);
        }
        return request;
    }

    private HandlerMethod handler(String methodName) throws NoSuchMethodException {
        return new HandlerMethod(new TestController(), TestController.class.getMethod(methodName));
    }

    static class TestController {

        @Idempotent(ttlSeconds = 120)
        public void create() {
        }

        public void open() {
        }
    }
}
