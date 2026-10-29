package com.gjsk.mediagateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 视频数据网关（media-gateway）启动类。
 *
 * <p>通用脚手架：不包含具体视频/流媒体业务，业务模块在本工程基础上按
 * {@code docs/adding-a-module.md} 的步骤扩展。</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class MediaGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(MediaGatewayApplication.class, args);
    }
}
