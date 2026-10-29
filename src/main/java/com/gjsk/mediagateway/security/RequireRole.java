package com.gjsk.mediagateway.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色校验注解。
 *
 * <p>标注在方法或类上，由 {@link RoleCheckInterceptor} 校验当前会话角色；
 * 满足任意一个角色即通过。未标注的接口只要求登录，不区分角色。</p>
 *
 * <pre>{@code
 * @RequireRole({RoleType.ROLE_ADMIN, RoleType.ROLE_OPERATOR})
 * }</pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {

    /**
     * 允许访问的角色集合。
     */
    String[] value();
}
