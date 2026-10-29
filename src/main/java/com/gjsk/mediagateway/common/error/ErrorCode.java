package com.gjsk.mediagateway.common.error;

/**
 * 统一错误码。
 *
 * <p>{@code code} 与 HTTP 状态码对齐，前端/调用方可稳定依赖；业务扩展错误码从 10000 起。</p>
 */
public enum ErrorCode {

    /** 成功。 */
    SUCCESS(0, "ok"),
    /** 请求参数错误。 */
    BAD_REQUEST(400, "请求参数错误"),
    /** 未认证或登录已失效。 */
    UNAUTHORIZED(401, "未认证或登录已失效"),
    /** 权限不足。 */
    FORBIDDEN(403, "权限不足"),
    /** 资源不存在。 */
    NOT_FOUND(404, "资源不存在"),
    /** 方法不允许。 */
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    /** 资源冲突。 */
    CONFLICT(409, "资源冲突"),
    /** 请求过于频繁。 */
    TOO_MANY_REQUESTS(429, "请求过于频繁"),
    /** 服务器内部错误。 */
    INTERNAL_ERROR(500, "服务器内部错误"),
    /** 上游服务异常。 */
    BAD_GATEWAY(502, "上游服务异常");

    private final int code;
    private final String defaultMessage;

    ErrorCode(int code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public int getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
