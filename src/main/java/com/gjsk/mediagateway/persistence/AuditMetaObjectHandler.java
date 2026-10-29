package com.gjsk.mediagateway.persistence;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.gjsk.mediagateway.security.AuthContext;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 审计字段自动填充。
 *
 * <p>create_time / update_time 由应用层填充；create_by / update_by 取当前登录用户，
 * 无登录上下文（如定时任务、系统初始化）时记为 {@code system}。</p>
 */
@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {

    private static final String SYSTEM_OPERATOR = "system";

    @Override
    public void insertFill(MetaObject metaObject) {
        Instant now = Instant.now();
        String operator = currentOperator();
        strictInsertFill(metaObject, "createTime", Instant.class, now);
        strictInsertFill(metaObject, "updateTime", Instant.class, now);
        strictInsertFill(metaObject, "createBy", String.class, operator);
        strictInsertFill(metaObject, "updateBy", String.class, operator);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updateTime", Instant.class, Instant.now());
        strictUpdateFill(metaObject, "updateBy", String.class, currentOperator());
    }

    private String currentOperator() {
        String userId = AuthContext.getUserId();
        return (userId == null || userId.isBlank()) ? SYSTEM_OPERATOR : userId;
    }
}
