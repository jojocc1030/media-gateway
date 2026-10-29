package com.gjsk.mediagateway.security;

import com.gjsk.mediagateway.common.error.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/**
 * {@link RequireRole} 角色校验拦截器。
 *
 * <p>方法级注解优先于类级注解；命中任意一个角色即放行，否则抛
 * {@link ForbiddenException}，由全局异常处理返回 403。</p>
 */
@Component
public class RoleCheckInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        RequireRole annotation = findRequireRole(handler);
        if (annotation == null) {
            return true;
        }
        String[] requiredRoles = annotation.value();
        if (AuthContext.hasAnyRole(Arrays.asList(requiredRoles))) {
            return true;
        }
        throw new ForbiddenException("需要以下角色之一: " + Arrays.toString(requiredRoles));
    }

    private RequireRole findRequireRole(Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return null;
        }
        RequireRole methodAnnotation = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        return handlerMethod.getBeanType().getAnnotation(RequireRole.class);
    }
}
