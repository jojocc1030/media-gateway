package com.gjsk.mediagateway.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 登录响应。
 */
@Schema(description = "登录响应")
public record LoginResponse(
        @Schema(description = "会话令牌", example = "9f8e7d6c5b4a39281706f5e4d3c2b1a0") String token,
        @Schema(description = "令牌类型", example = "Bearer") String tokenType,
        @Schema(description = "有效期（秒）", example = "7200") long expiresIn) {
}
