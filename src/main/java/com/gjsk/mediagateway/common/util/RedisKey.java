package com.gjsk.mediagateway.common.util;

/**
 * Redis key 统一构造器。
 *
 * <p>约定：所有 key 以 {@code mgw:} 开头，中间带模块名，必须设置 TTL（会话、幂等、缓存）。
 * 业务代码禁止散落硬编码 key。</p>
 */
public final class RedisKey {

    private static final String PREFIX = "mgw:";
    private static final int MAX_SEGMENT_LENGTH = 64;

    private RedisKey() {
    }

    /**
     * 会话 key。
     *
     * @param sessionToken 会话令牌
     */
    public static String session(String sessionToken) {
        return PREFIX + "session:" + segment(sessionToken);
    }

    /**
     * 幂等 key。
     *
     * @param subject 请求主体（登录用户 ID，匿名请求为 anonymous）
     * @param method  HTTP 方法
     * @param path    请求路径
     * @param key     调用方提供的 Idempotency-Key
     */
    public static String idempotent(String subject, String method, String path, String key) {
        return PREFIX + "idem:" + segment(subject) + ":" + segment(method) + ":"
                + segment(path) + ":" + segment(key);
    }

    /**
     * 对 key 片段做安全化处理，避免长度与特殊字符问题。
     */
    private static String segment(String raw) {
        if (raw == null || raw.isBlank()) {
            return "anonymous";
        }
        String sanitized = raw.replaceAll("[^A-Za-z0-9_.-]", "_");
        return sanitized.length() <= MAX_SEGMENT_LENGTH
                ? sanitized
                : sanitized.substring(0, MAX_SEGMENT_LENGTH);
    }
}
