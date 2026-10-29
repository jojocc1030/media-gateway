package com.gjsk.mediagateway.common.error;

/**
 * 未认证或登录已失效（401）。
 */
public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED.getCode(), message);
    }
}
