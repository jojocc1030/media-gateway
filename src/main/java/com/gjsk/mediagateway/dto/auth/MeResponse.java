package com.gjsk.mediagateway.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

/**
 * 当前用户信息。
 */
@Schema(description = "当前用户信息")
public record MeResponse(
        @Schema(description = "用户 ID", example = "admin") String userId,
        @Schema(description = "登录名", example = "admin") String username,
        @Schema(description = "角色集合", example = "[\"ADMIN\",\"OPERATOR\"]") Set<String> roles) {
}
