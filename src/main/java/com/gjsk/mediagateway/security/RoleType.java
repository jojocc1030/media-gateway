package com.gjsk.mediagateway.security;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 通用角色类型。
 *
 * <p>角色属于平台能力，不绑定具体业务；业务模块通过 {@code @RequireRole} 引用。
 * 新增角色时同时补充静态常量，便于注解引用。</p>
 */
@Schema(description = "角色类型")
public enum RoleType {

    /** 管理员：全部读写。 */
    ADMIN("管理员"),
    /** 操作员：业务读写。 */
    OPERATOR("操作员"),
    /** 只读：仅查询。 */
    VIEWER("只读");

    /** 供 @RequireRole 注解引用的字符串常量。 */
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_OPERATOR = "OPERATOR";
    public static final String ROLE_VIEWER = "VIEWER";

    private final String displayName;

    RoleType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
