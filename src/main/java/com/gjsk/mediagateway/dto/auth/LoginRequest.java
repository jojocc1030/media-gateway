package com.gjsk.mediagateway.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 登录请求。
 */
@Schema(description = "登录请求")
public record LoginRequest(
        @Schema(description = "登录名", example = "admin")
        @NotBlank(message = "登录名不能为空")
        @Size(max = 64, message = "登录名长度不能超过 64")
        String username,

        @Schema(description = "口令", example = "admin123")
        @NotBlank(message = "口令不能为空")
        @Size(max = 128, message = "口令长度不能超过 128")
        String password) {
}
