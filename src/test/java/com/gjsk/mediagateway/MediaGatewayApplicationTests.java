package com.gjsk.mediagateway;

import com.gjsk.mediagateway.config.AppProperties;
import com.gjsk.mediagateway.config.SecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 上下文装配冒烟测试：验证 Bean 装配与配置绑定（不连接外部 PostgreSQL/Redis）。
 */
@SpringBootTest
class MediaGatewayApplicationTests {

    @Autowired
    private SecurityProperties securityProperties;

    @Autowired
    private AppProperties appProperties;

    @Test
    void contextLoads() {
        assertThat(securityProperties).isNotNull();
    }

    @Test
    void securityUsersShouldBeBoundFromConfiguration() {
        assertThat(securityProperties.getUsers()).isNotEmpty();
        assertThat(securityProperties.getUsers())
                .anySatisfy(user -> {
                    assertThat(user.getUsername()).isEqualTo("admin");
                    assertThat(user.getPasswordHash()).startsWith("pbkdf2$210000$");
                    assertThat(user.getRoles()).contains("ADMIN");
                    assertThat(user.isEnabled()).isTrue();
                });
        assertThat(securityProperties.getSession().getTtlSeconds()).isEqualTo(7200);
        assertThat(securityProperties.getSession().isRefreshOnAccess()).isTrue();
    }

    @Test
    void publicPathsShouldBeBound() {
        assertThat(securityProperties.getPublicPaths())
                .contains("/health", "/api/auth/login", "/api/public/**")
                .allSatisfy(path -> assertThat(path).startsWith("/"));
    }

    @Test
    void versionShouldBeBound() {
        assertThat(appProperties.getVersion()).isEqualTo("dev");
    }
}
