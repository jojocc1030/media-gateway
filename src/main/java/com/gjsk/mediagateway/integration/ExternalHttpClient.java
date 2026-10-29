package com.gjsk.mediagateway.integration;

import com.gjsk.mediagateway.common.error.ThirdPartyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.function.Supplier;

/**
 * 外部 HTTP 调用封装。
 *
 * <p>统一处理超时/网络异常与上游错误码映射，业务代码只需关注成功响应。
 * 上游返回 4xx/5xx 时抛出 {@link ThirdPartyException}（对外 502），
 * 响应体只截断记录到日志，不返回给调用方。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalHttpClient {

    private static final int MAX_LOGGED_BODY_LENGTH = 512;

    private final RestTemplate restTemplate;

    /**
     * GET 请求。
     *
     * @param service      上游服务名（仅用于日志与错误信息）
     * @param url          完整 URL
     * @param responseType 响应类型
     */
    public <T> T get(String service, String url, Class<T> responseType) {
        return execute(service, () -> restTemplate.getForObject(url, responseType));
    }

    /**
     * POST 请求（JSON）。
     *
     * @param service      上游服务名
     * @param url          完整 URL
     * @param body         请求体
     * @param responseType 响应类型
     */
    public <T> T post(String service, String url, Object body, Class<T> responseType) {
        return execute(service, () -> restTemplate.postForObject(url, body, responseType));
    }

    private <T> T execute(String service, Supplier<T> call) {
        try {
            return call.get();
        } catch (RestClientResponseException e) {
            log.warn("上游返回错误: service={} status={} body={}",
                    service, e.getStatusCode().value(), abbreviate(e.getResponseBodyAsString()));
            throw new ThirdPartyException(service, e.getStatusCode().value());
        } catch (RestClientException e) {
            log.error("上游调用失败: service={} err={}", service, e.getMessage());
            throw new ThirdPartyException("上游服务不可用: " + service);
        }
    }

    private String abbreviate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() <= MAX_LOGGED_BODY_LENGTH
                ? body
                : body.substring(0, MAX_LOGGED_BODY_LENGTH) + "...";
    }
}
