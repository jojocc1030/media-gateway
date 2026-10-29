package com.gjsk.mediagateway.common.web;

import com.gjsk.mediagateway.common.api.BaseResponse;
import com.gjsk.mediagateway.common.error.BusinessException;
import com.gjsk.mediagateway.common.error.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理：把异常统一转换为 {@link BaseResponse}，同时保持真实 HTTP 状态码。
 *
 * <p>4xx 只记录 warn 且不带堆栈；5xx 记录 error 与堆栈，对外不泄露 SQL、堆栈、密钥细节。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常：按异常携带的错误码映射 HTTP 状态。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<BaseResponse<Void>> handleBusinessException(BusinessException e) {
        HttpStatus status = resolveStatus(e.getCode());
        if (status.is5xxServerError()) {
            log.error("业务异常(5xx): code={} message={}", e.getCode(), e.getMessage(), e);
        } else {
            log.warn("业务异常: code={} message={}", e.getCode(), e.getMessage());
        }
        return ResponseEntity.status(status).body(BaseResponse.error(e.getCode(), e.getMessage()));
    }

    /**
     * {@code @RequestBody} 参数校验失败。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(this::describeFieldError)
                .distinct()
                .collect(Collectors.joining("; "));
        if (message.isBlank()) {
            message = ErrorCode.BAD_REQUEST.getDefaultMessage();
        }
        log.warn("参数校验失败: {}", message);
        return badRequest(message);
    }

    /**
     * 方法参数（如 query/path 参数）校验失败。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<BaseResponse<Void>> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .distinct()
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", message);
        return badRequest(message.isBlank() ? ErrorCode.BAD_REQUEST.getDefaultMessage() : message);
    }

    /**
     * 缺少必填 query 参数。
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<BaseResponse<Void>> handleMissingParameter(MissingServletRequestParameterException e) {
        String message = "缺少请求参数: " + e.getParameterName();
        log.warn(message);
        return badRequest(message);
    }

    /**
     * 参数类型不匹配。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<BaseResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message = "请求参数类型错误: " + e.getName();
        log.warn(message);
        return badRequest(message);
    }

    /**
     * 请求体不可解析（JSON 格式错误、字段类型不符等）。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResponse<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return badRequest("请求体格式错误");
    }

    /**
     * 请求方法不支持。
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<BaseResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("请求方法不支持: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(BaseResponse.error(ErrorCode.METHOD_NOT_ALLOWED.getCode(),
                        ErrorCode.METHOD_NOT_ALLOWED.getDefaultMessage()));
    }

    /**
     * 静态资源/路径不存在。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<BaseResponse<Void>> handleNoResource(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(BaseResponse.error(ErrorCode.NOT_FOUND.getCode(), ErrorCode.NOT_FOUND.getDefaultMessage()));
    }

    /**
     * 数据访问异常：对外统一 500，避免泄露 SQL 细节。
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<BaseResponse<Void>> handleDataAccess(DataAccessException e) {
        log.error("数据访问异常", e);
        return internalError();
    }

    /**
     * 兜底异常。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleUnknown(Exception e) {
        log.error("未捕获异常", e);
        return internalError();
    }

    private String describeFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private ResponseEntity<BaseResponse<Void>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error(ErrorCode.BAD_REQUEST.getCode(), message));
    }

    private ResponseEntity<BaseResponse<Void>> internalError() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BaseResponse.error(ErrorCode.INTERNAL_ERROR.getCode(),
                        ErrorCode.INTERNAL_ERROR.getDefaultMessage()));
    }

    private HttpStatus resolveStatus(int code) {
        HttpStatus status = HttpStatus.resolve(code);
        return status == null ? HttpStatus.BAD_REQUEST : status;
    }
}
