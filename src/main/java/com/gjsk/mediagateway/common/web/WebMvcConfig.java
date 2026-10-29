package com.gjsk.mediagateway.common.web;

import com.gjsk.mediagateway.common.idempotent.IdempotentInterceptor;
import com.gjsk.mediagateway.config.SecurityProperties;
import com.gjsk.mediagateway.security.RoleCheckInterceptor;
import com.gjsk.mediagateway.security.SessionAuthFilter;
import com.gjsk.mediagateway.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web 层装配：过滤器、拦截器、CORS。
 *
 * <p>过滤器顺序：TraceId(0) → SessionAuth(1) → AccessLog(2)。
 * 拦截器顺序：角色校验(0) → 幂等(1)。</p>
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final SecurityProperties securityProperties;
    private final SessionService sessionService;
    private final RoleCheckInterceptor roleCheckInterceptor;
    private final IdempotentInterceptor idempotentInterceptor;

    /**
     * 链路追踪过滤器：覆盖全部请求。
     */
    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration() {
        FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>(new TraceIdFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(0);
        return registration;
    }

    /**
     * 会话鉴权过滤器：仅覆盖业务接口。
     */
    @Bean
    public FilterRegistrationBean<SessionAuthFilter> sessionAuthFilterRegistration() {
        FilterRegistrationBean<SessionAuthFilter> registration =
                new FilterRegistrationBean<>(new SessionAuthFilter(sessionService, securityProperties));
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1);
        return registration;
    }

    /**
     * 访问日志过滤器：覆盖全部请求。
     */
    @Bean
    public FilterRegistrationBean<AccessLogFilter> accessLogFilterRegistration() {
        FilterRegistrationBean<AccessLogFilter> registration = new FilterRegistrationBean<>(new AccessLogFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(2);
        return registration;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders(TraceIdFilter.TRACE_ID_HEADER)
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        String[] publicPaths = securityProperties.getPublicPaths().toArray(new String[0]);
        if (securityProperties.isRoleCheckEnabled()) {
            registry.addInterceptor(roleCheckInterceptor)
                    .addPathPatterns("/api/**")
                    .excludePathPatterns(publicPaths)
                    .order(0);
        }
        registry.addInterceptor(idempotentInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(publicPaths)
                .order(1);
    }
}
