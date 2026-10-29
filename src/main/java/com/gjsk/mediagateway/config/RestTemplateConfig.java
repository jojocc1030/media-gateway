package com.gjsk.mediagateway.config;

import com.gjsk.mediagateway.common.web.TraceIdFilter;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * 外部 HTTP 客户端装配：统一超时 + 链路 ID 透传。
 *
 * <p>业务调用上游系统（如 ZLMediaKit REST API）统一注入 {@link RestTemplate}，
 * 或使用 {@code ExternalHttpClient} 封装以获得统一的异常映射。</p>
 */
@Configuration
@RequiredArgsConstructor
public class RestTemplateConfig {

    private final HttpClientProperties httpClientProperties;

    /**
     * 通用 RestTemplate。
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(httpClientProperties.getConnectTimeoutMillis());
        factory.setReadTimeout(httpClientProperties.getReadTimeoutMillis());

        RestTemplate restTemplate = new RestTemplate(factory);
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>(restTemplate.getInterceptors());
        interceptors.add(traceIdInterceptor());
        restTemplate.setInterceptors(interceptors);
        return restTemplate;
    }

    private ClientHttpRequestInterceptor traceIdInterceptor() {
        return (request, body, execution) -> {
            String traceId = MDC.get(TraceIdFilter.MDC_TRACE_ID);
            if (traceId != null) {
                request.getHeaders().set(TraceIdFilter.TRACE_ID_HEADER, traceId);
            }
            return execution.execute(request, body);
        };
    }
}
