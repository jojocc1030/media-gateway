package com.gjsk.mediagateway.service;

import com.gjsk.mediagateway.common.util.IdUtils;
import com.gjsk.mediagateway.common.util.JsonUtils;
import com.gjsk.mediagateway.common.util.RedisKey;
import com.gjsk.mediagateway.security.SessionInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 会话管理：基于 Redis 存储会话信息，支持多端登录与滑动过期。
 *
 * <p>Key 结构：{@code mgw:session:{sessionToken}}，Value 为 SessionInfo JSON。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final StringRedisTemplate redis;

    /**
     * 创建会话并返回会话令牌。
     *
     * @param session    会话信息
     * @param ttlSeconds 有效期（秒）
     */
    public String createSession(SessionInfo session, long ttlSeconds) {
        String token = IdUtils.newToken();
        saveSession(token, session, ttlSeconds);
        log.info("会话创建: userId={} roles={} ttl={}s", session.getUserId(), session.getRoles(), ttlSeconds);
        return token;
    }

    /**
     * 保存（覆盖）会话。
     */
    public void saveSession(String sessionToken, SessionInfo session, long ttlSeconds) {
        redis.opsForValue().set(RedisKey.session(sessionToken),
                JsonUtils.toJson(session), Duration.ofSeconds(ttlSeconds));
        log.info("会话写入: userId={} ttl={}s", session.getUserId(), ttlSeconds);
    }

    /**
     * 按令牌查询会话，不存在或已过期返回 null。
     */
    public SessionInfo getSessionByToken(String sessionToken) {
        String json = redis.opsForValue().get(RedisKey.session(sessionToken));
        return JsonUtils.fromJson(json, SessionInfo.class);
    }

    /**
     * 删除会话（登出）。
     */
    public void removeSessionByToken(String sessionToken) {
        Boolean deleted = redis.delete(RedisKey.session(sessionToken));
        log.info("会话移除: deleted={}", deleted);
    }

    /**
     * 刷新会话 TTL。
     */
    public void refreshSession(String sessionToken, long ttlSeconds) {
        Boolean extended = redis.expire(RedisKey.session(sessionToken), Duration.ofSeconds(ttlSeconds));
        log.debug("会话续期: extended={} ttl={}s", extended, ttlSeconds);
    }
}
