package com.gjsk.mediagateway.common.idempotent;

import com.gjsk.mediagateway.common.error.BadRequestException;
import com.gjsk.mediagateway.common.error.ConflictException;
import com.gjsk.mediagateway.common.util.RedisKey;
import com.gjsk.mediagateway.security.AuthContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.regex.Pattern;

/**
 * 幂等拦截器：基于 Redis SETNX 实现。
 *
 * <p>读取 {@code Idempotency-Key} 请求头，按“用户 + 方法 + 路径 + key”维度占位，
 * 占位失败说明重复请求，直接返回 409。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotentInterceptor implements HandlerInterceptor {

    /** 幂等请求头。 */
    public static final String HEADER = "Idempotency-Key";

    private static final Pattern KEY_PATTERN = Pattern.compile("[A-Za-z0-9_.:-]{8,128}");

    private final StringRedisTemplate redis;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Idempotent annotation = findAnnotation(handler);
        if (annotation == null) {
            return true;
        }

        String key = request.getHeader(HEADER);
        if (key == null || !KEY_PATTERN.matcher(key).matches()) {
            throw new BadRequestException("缺少或非法的 " + HEADER + " 请求头（8-128 位字母、数字或 _ . : -）");
        }

        String redisKey = RedisKey.idempotent(AuthContext.getUserId(), request.getMethod(),
                request.getServletPath(), key);
        Boolean acquired = redis.opsForValue().setIfAbsent(redisKey, "1",
                Duration.ofSeconds(annotation.ttlSeconds()));
        if (!Boolean.TRUE.equals(acquired)) {
            log.warn("重复请求: key={} user={} {} {}",
                    key, AuthContext.getUserId(), request.getMethod(), request.getServletPath());
            throw new ConflictException("重复请求，请勿重复提交");
        }
        return true;
    }

    private Idempotent findAnnotation(Object handler) {
        if (handler instanceof HandlerMethod handlerMethod) {
            return handlerMethod.getMethodAnnotation(Idempotent.class);
        }
        return null;
    }
}
