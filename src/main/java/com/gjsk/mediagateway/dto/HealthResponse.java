package com.gjsk.mediagateway.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 健康检查响应。
 */
@Schema(description = "健康检查响应")
public record HealthResponse(
        @Schema(description = "服务状态", example = "UP") String status,
        @Schema(description = "服务名", example = "media-gateway") String service,
        @Schema(description = "当前版本", example = "dev") String version) {
}
