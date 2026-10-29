package com.gjsk.mediagateway.common.idempotent;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 幂等控制注解。
 *
 * <p>标注在写接口方法上，要求调用方携带 {@code Idempotency-Key} 请求头；
 * 同一用户、同一方法、同一路径下该 key 在 TTL 内只允许成功处理一次，
 * 重复请求返回 409。适用于创建、提交、状态流转等重试敏感操作。</p>
 *
 * <pre>{@code
 * @Idempotent(ttlSeconds = 300)
 * @PostMapping
 * public BaseResponse<DemoItemVO> create(...) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * 幂等 key 保留时长（秒）。
     */
    int ttlSeconds() default 300;
}
