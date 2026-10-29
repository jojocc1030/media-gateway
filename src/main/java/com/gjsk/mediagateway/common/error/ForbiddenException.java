package com.gjsk.mediagateway.common.error;

/**
 * 权限不足（403）。
 */
public class ForbiddenException extends BusinessException {

    public ForbiddenException(String message) {
        super(ErrorCode.FORBIDDEN.getCode(), message);
    }
}
