package com.gjsk.mediagateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 应用基础配置，绑定 {@code mgw.app.*}。
 */
@Data
@ConfigurationProperties(prefix = "mgw.app")
public class AppProperties {

    /** 应用版本，用于健康检查与接口文档展示。 */
    private String version = "0.1.0";
}
