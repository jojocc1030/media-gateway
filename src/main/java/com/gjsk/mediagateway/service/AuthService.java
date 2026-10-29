package com.gjsk.mediagateway.service;

import com.gjsk.mediagateway.common.error.UnauthorizedException;
import com.gjsk.mediagateway.config.SecurityProperties;
import com.gjsk.mediagateway.dto.auth.LoginResponse;
import com.gjsk.mediagateway.dto.auth.MeResponse;
import com.gjsk.mediagateway.security.AuthContext;
import com.gjsk.mediagateway.security.PasswordHasher;
import com.gjsk.mediagateway.security.SessionInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 认证服务：登录、登出、当前用户信息。
 *
 * <p>账号来自配置 {@code mgw.security.users}。生产环境建议替换本类的账号来源
 * （数据库/统一身份系统），会话与鉴权链路无需改动。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SecurityProperties securityProperties;
    private final SessionService sessionService;

    /**
     * 登录并创建会话。
     *
     * @param username 登录名
     * @param password 明文口令
     * @return 会话令牌与有效期
     */
    public LoginResponse login(String username, String password) {
        SecurityProperties.LoginUser user = findUser(username);
        if (user == null || !user.isEnabled() || !PasswordHasher.matches(password, user.getPasswordHash())) {
            log.warn("登录失败: username={}", username);
            throw new UnauthorizedException("用户名或密码错误");
        }

        SessionInfo session = new SessionInfo();
        session.setUserId(blankToDefault(user.getUserId(), user.getUsername()));
        session.setUsername(user.getUsername());
        session.setRoles(user.getRoles() == null ? new LinkedHashSet<>() : new LinkedHashSet<>(user.getRoles()));

        long ttlSeconds = securityProperties.getSession().getTtlSeconds();
        String token = sessionService.createSession(session, ttlSeconds);
        log.info("登录成功: userId={} roles={}", session.getUserId(), session.getRoles());
        return new LoginResponse(token, "Bearer", ttlSeconds);
    }

    /**
     * 登出并删除会话。
     */
    public void logout(String sessionToken) {
        sessionService.removeSessionByToken(sessionToken);
        log.info("登出成功");
    }

    /**
     * 当前登录用户信息。
     */
    public MeResponse currentUser() {
        SessionInfo session = AuthContext.get();
        if (session == null) {
            throw new UnauthorizedException("未登录");
        }
        Set<String> roles = session.getRoles() == null ? Set.of() : session.getRoles();
        return new MeResponse(session.getUserId(), session.getUsername(), roles);
    }

    private SecurityProperties.LoginUser findUser(String username) {
        if (username == null) {
            return null;
        }
        return securityProperties.getUsers().stream()
                .filter(user -> username.equals(user.getUsername()))
                .findFirst()
                .orElse(null);
    }

    private static String blankToDefault(String value, String defaultValue) {
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}
