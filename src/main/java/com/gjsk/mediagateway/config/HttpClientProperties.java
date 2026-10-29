package com.gjsk.mediagateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 外部 HTTP 客户端配置，绑定 {@code mgw.http.*}。
 *
 * <p>用于调用上游系统（如 ZLMediaKit REST API）的通用超时设置；
 * 业务对接时通过 {@code RestTemplate} 注入使用。</p>
 */
@Data
@ConfigurationProperties(prefix = "mgw.http")
public class HttpClientProperties {

    /** 连接超时（毫秒）。 */
    private int connectTimeoutMillis = 3000;

    /** 读取超时（毫秒）。 */
    private int readTimeoutMillis = 10000;
}
