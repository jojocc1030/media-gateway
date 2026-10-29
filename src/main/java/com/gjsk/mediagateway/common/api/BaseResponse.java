package com.gjsk.mediagateway.common.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 统一接口响应包装。
 *
 * <p>所有业务接口统一返回该结构；错误响应使用真实 HTTP 状态码 + 稳定的 {@code code}。</p>
 *
 * @param <T> 业务数据类型
 */
@Schema(description = "统一接口响应")
public class BaseResponse<T> {

    /** 成功。 */
    public static final int SUCCESS = 0;
    /** 请求参数错误。 */
    public static final int BAD_REQUEST = 400;
    /** 未认证或登录已失效。 */
    public static final int UNAUTHORIZED = 401;
    /** 权限不足。 */
    public static final int FORBIDDEN = 403;
    /** 资源不存在。 */
    public static final int NOT_FOUND = 404;
    /** 资源冲突（重复提交、唯一键冲突等）。 */
    public static final int CONFLICT = 409;
    /** 请求过于频繁。 */
    public static final int TOO_MANY_REQUESTS = 429;
    /** 服务器内部错误。 */
    public static final int INTERNAL_ERROR = 500;
    /** 上游服务异常。 */
    public static final int BAD_GATEWAY = 502;

    @Schema(description = "响应码：0-成功，其他值表示错误", example = "0")
    private int code;

    @Schema(description = "响应消息", example = "ok")
    private String message;

    @Schema(description = "业务数据，错误或无返回数据时为 null")
    private T data;

    @Schema(description = "非标准附加元数据，常规业务数据一律放 data")
    private Object extra;

    private BaseResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功响应（无数据）。
     */
    public static <T> BaseResponse<T> ok() {
        return new BaseResponse<>(SUCCESS, "ok", null);
    }

    /**
     * 成功响应。
     *
     * @param data 业务数据
     */
    public static <T> BaseResponse<T> ok(T data) {
        return new BaseResponse<>(SUCCESS, "ok", data);
    }

    /**
     * 成功响应，携带非标准附加元数据。
     *
     * @param data  业务数据
     * @param extra 附加元数据
     */
    public static <T> BaseResponse<T> ok(T data, Object extra) {
        BaseResponse<T> response = new BaseResponse<>(SUCCESS, "ok", data);
        response.extra = extra;
        return response;
    }

    /**
     * 错误响应。
     *
     * @param code    错误码
     * @param message 错误消息
     */
    public static <T> BaseResponse<T> error(int code, String message) {
        return new BaseResponse<>(code, message, null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Object getExtra() {
        return extra;
    }

    public void setExtra(Object extra) {
        this.extra = extra;
    }
}
