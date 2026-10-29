package com.gjsk.mediagateway.controller;

import com.gjsk.mediagateway.common.api.BaseResponse;
import com.gjsk.mediagateway.common.error.UnauthorizedException;
import com.gjsk.mediagateway.dto.auth.LoginRequest;
import com.gjsk.mediagateway.dto.auth.LoginResponse;
import com.gjsk.mediagateway.dto.auth.MeResponse;
import com.gjsk.mediagateway.security.SessionAuthFilter;
import com.gjsk.mediagateway.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口。
 */
@Tag(name = "认证", description = "登录、登出与当前用户信息")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * 登录并获取会话令牌。
     */
    @Operation(summary = "登录", description = "使用配置的开发账号登录，成功后返回 Bearer 会话令牌。")
    @PostMapping("/login")
    public BaseResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return BaseResponse.ok(authService.login(request.username(), request.password()));
    }

    /**
     * 登出并删除当前会话。
     */
    @Operation(summary = "登出", description = "删除当前 Authorization 令牌对应的会话。")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public BaseResponse<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.logout(extractToken(authorization));
        return BaseResponse.ok();
    }

    /**
     * 查询当前登录用户信息。
     */
    @Operation(summary = "当前用户", description = "返回当前会话的用户 ID、登录名与角色集合。")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public BaseResponse<MeResponse> me() {
        return BaseResponse.ok(authService.currentUser());
    }

    private String extractToken(String authorization) {
        if (authorization == null || !authorization.startsWith(SessionAuthFilter.BEARER_PREFIX)) {
            throw new UnauthorizedException("未提供有效的认证令牌");
        }
        String token = authorization.substring(SessionAuthFilter.BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new UnauthorizedException("未提供有效的认证令牌");
        }
        return token;
    }
}
