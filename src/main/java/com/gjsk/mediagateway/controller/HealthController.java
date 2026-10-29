package com.gjsk.mediagateway.controller;

import com.gjsk.mediagateway.common.api.BaseResponse;
import com.gjsk.mediagateway.config.AppProperties;
import com.gjsk.mediagateway.dto.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查接口。
 */
@Tag(name = "健康检查", description = "服务健康状态")
@RestController
@RequiredArgsConstructor
public class HealthController {

    private final AppProperties appProperties;

    /**
     * 服务健康检查。
     */
    @Operation(summary = "服务健康检查", description = "返回服务状态、服务名和当前版本，可用于容器探针。")
    @GetMapping("/health")
    public BaseResponse<HealthResponse> health() {
        return BaseResponse.ok(new HealthResponse("UP", "media-gateway", appProperties.getVersion()));
    }
}
