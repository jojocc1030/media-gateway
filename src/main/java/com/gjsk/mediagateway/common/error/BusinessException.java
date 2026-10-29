package com.gjsk.mediagateway.common.error;

/**
 * 业务异常基类，由 {@code GlobalExceptionHandler} 统一转换为接口响应。
 *
 * <p>业务代码抛出本类或其子类表达可预期的失败；未预期异常由兜底处理返回 500。</p>
 */
public class BusinessException extends RuntimeException {

    private final int code;

    /**
     * @param code    错误码
     * @param message 可直接返回给调用方的错误消息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 使用错误码枚举的默认消息。
     *
     * @param errorCode 错误码枚举
     */
    public BusinessException(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getDefaultMessage());
    }

    public int getCode() {
        return code;
    }
}
