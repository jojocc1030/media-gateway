package com.gjsk.mediagateway.common.web;

import com.gjsk.mediagateway.config.AppProperties;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 接口文档配置。
 */
@Configuration
@RequiredArgsConstructor
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    private final AppProperties appProperties;

    /**
     * 文档基础信息与 Bearer 会话令牌鉴权方案。
     */
    @Bean
    public OpenAPI mediaGatewayOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("视频数据网关接口文档")
                        .version(appProperties.getVersion())
                        .description("统一返回 BaseResponse；登录后通过 Authorization: Bearer <sessionToken> 访问受保护接口。"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("Session Token")));
    }
}
