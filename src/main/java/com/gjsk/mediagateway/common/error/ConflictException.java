package com.gjsk.mediagateway.common.error;

/**
 * 资源冲突（409），例如重复提交、唯一键冲突。
 */
public class ConflictException extends BusinessException {

    public ConflictException(String message) {
        super(ErrorCode.CONFLICT.getCode(), message);
    }
}
