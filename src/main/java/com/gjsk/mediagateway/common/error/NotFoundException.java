package com.gjsk.mediagateway.common.error;

/**
 * 资源不存在（404）。
 */
public class NotFoundException extends BusinessException {

    public NotFoundException(String message) {
        super(ErrorCode.NOT_FOUND.getCode(), message);
    }
}
