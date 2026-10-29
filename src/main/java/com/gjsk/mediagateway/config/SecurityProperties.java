package com.gjsk.mediagateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 安全与登录配置，绑定 {@code mgw.security.*}。
 *
 * <p>开发账号来自配置，生产环境应通过外部配置文件或环境变量覆盖，
 * 或替换 {@code AuthService} 接入公司统一身份系统（Keycloak / MaxKey 等）。</p>
 */
@Data
@ConfigurationProperties(prefix = "mgw.security")
public class SecurityProperties {

    /** 无需登录即可访问的路径，Ant 风格，基于应用内相对路径。 */
    private List<String> publicPaths = new ArrayList<>();

    /** 是否启用 {@code @RequireRole} 角色校验。 */
    private boolean roleCheckEnabled = true;

    /** 会话配置。 */
    private Session session = new Session();

    /** 登录账号列表。 */
    private List<LoginUser> users = new ArrayList<>();

    /**
     * 会话配置。
     */
    @Data
    public static class Session {

        /** 会话有效期（秒），每次访问可续期。 */
        private long ttlSeconds = 7200;

        /** 是否在访问时刷新会话 TTL（滑动过期）。 */
        private boolean refreshOnAccess = true;
    }

    /**
     * 登录账号配置。
     */
    @Data
    public static class LoginUser {

        /** 对外用户 ID，为空时使用 username。 */
        private String userId;

        /** 登录名。 */
        private String username;

        /** PBKDF2 口令哈希，格式 {@code pbkdf2$迭代次数$盐Base64$哈希Base64}。 */
        private String passwordHash;

        /** 是否启用。 */
        private boolean enabled = true;

        /** 角色集合，对应 {@code RoleType} 名称。 */
        private Set<String> roles = new LinkedHashSet<>();
    }
}
