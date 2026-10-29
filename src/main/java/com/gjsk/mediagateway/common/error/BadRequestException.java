package com.gjsk.mediagateway.common.error;

/**
 * 请求参数错误（400）。
 */
public class BadRequestException extends BusinessException {

    public BadRequestException(String message) {
        super(ErrorCode.BAD_REQUEST.getCode(), message);
    }
}
