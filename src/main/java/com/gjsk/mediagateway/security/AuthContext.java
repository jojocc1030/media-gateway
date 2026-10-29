package com.gjsk.mediagateway.security;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * 请求级认证上下文，由 {@link SessionAuthFilter} 校验通过后写入，请求结束清理。
 *
 * <p>业务代码通过本类获取当前用户与角色，禁止直接解析 Authorization 头。</p>
 */
public final class AuthContext {

    private static final ThreadLocal<SessionInfo> HOLDER = new ThreadLocal<>();

    private AuthContext() {
    }

    /**
     * 绑定当前请求的会话。
     */
    public static void set(SessionInfo session) {
        HOLDER.set(session);
    }

    /**
     * 当前会话，未认证时为 null。
     */
    public static SessionInfo get() {
        return HOLDER.get();
    }

    /**
     * 当前用户 ID，未认证时为 null。
     */
    public static String getUserId() {
        SessionInfo session = HOLDER.get();
        return session == null ? null : session.getUserId();
    }

    /**
     * 当前登录名，未认证时为 null。
     */
    public static String getUsername() {
        SessionInfo session = HOLDER.get();
        return session == null ? null : session.getUsername();
    }

    /**
     * 当前角色集合，未认证时为空集合。
     */
    public static Set<String> getRoles() {
        SessionInfo session = HOLDER.get();
        return session == null || session.getRoles() == null
                ? Collections.emptySet()
                : session.getRoles();
    }

    /**
     * 是否拥有指定角色。
     */
    public static boolean hasRole(String role) {
        return role != null && getRoles().contains(role);
    }

    /**
     * 是否拥有任意一个指定角色。
     */
    public static boolean hasAnyRole(Collection<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        Set<String> current = getRoles();
        for (String role : roles) {
            if (current.contains(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 清理上下文（由过滤器在请求结束时调用）。
     */
    public static void clear() {
        HOLDER.remove();
    }
}
