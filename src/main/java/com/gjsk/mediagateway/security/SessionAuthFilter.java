package com.gjsk.mediagateway.security;

import com.gjsk.mediagateway.common.api.BaseResponse;
import com.gjsk.mediagateway.common.util.JsonUtils;
import com.gjsk.mediagateway.config.SecurityProperties;
import com.gjsk.mediagateway.service.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 基于会话令牌的鉴权过滤器（magpie 同款思路）。
 *
 * <p>从 {@code Authorization: Bearer <sessionToken>} 读取令牌，查 Redis 获取会话，
 * 写入 {@link AuthContext} 与 MDC；公开路径直接放行；校验失败返回 401 统一响应。</p>
 */
public class SessionAuthFilter extends OncePerRequestFilter {

    /** Bearer 前缀。 */
    public static final String BEARER_PREFIX = "Bearer ";
    /** MDC 用户 key。 */
    public static final String MDC_USER_ID = "userId";

    private static final Logger log = LoggerFactory.getLogger(SessionAuthFilter.class);
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final SessionService sessionService;
    private final SecurityProperties securityProperties;

    public SessionAuthFilter(SessionService sessionService, SecurityProperties securityProperties) {
        this.sessionService = sessionService;
        this.securityProperties = securityProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // CORS 预检请求直接放行，由 Spring MVC CORS 处理
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        if (isPublicPath(request.getServletPath())) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            log.warn("缺少认证令牌: {} {}", request.getMethod(), request.getServletPath());
            writeUnauthorized(response, "未提供有效的认证令牌");
            return;
        }

        String sessionToken = header.substring(BEARER_PREFIX.length()).trim();
        if (sessionToken.isEmpty()) {
            writeUnauthorized(response, "未提供有效的认证令牌");
            return;
        }

        SessionInfo session = sessionService.getSessionByToken(sessionToken);
        if (session == null) {
            log.warn("会话已失效: tokenPrefix={}", tokenPrefix(sessionToken));
            writeUnauthorized(response, "会话已失效，请重新登录");
            return;
        }

        AuthContext.set(session);
        MDC.put(MDC_USER_ID, session.getUserId());
        try {
            if (securityProperties.getSession().isRefreshOnAccess()) {
                sessionService.refreshSession(sessionToken, securityProperties.getSession().getTtlSeconds());
            }
            log.debug("鉴权通过: userId={} roles={}", session.getUserId(), session.getRoles());
            filterChain.doFilter(request, response);
        } finally {
            AuthContext.clear();
            MDC.remove(MDC_USER_ID);
        }
    }

    private boolean isPublicPath(String path) {
        List<String> publicPaths = securityProperties.getPublicPaths();
        if (publicPaths == null || publicPaths.isEmpty()) {
            return false;
        }
        for (String pattern : publicPaths) {
            if (PATH_MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JsonUtils.toJson(BaseResponse.error(BaseResponse.UNAUTHORIZED, message)));
    }

    private static String tokenPrefix(String token) {
        return token.length() <= 8 ? "***" : token.substring(0, 8) + "***";
    }
}
