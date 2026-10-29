package com.gjsk.mediagateway.security;

import lombok.Data;

import java.util.Collections;
import java.util.Set;

/**
 * 登录会话信息，存储在 Redis，并作为 {@link AuthContext} 的载体。
 */
@Data
public class SessionInfo {

    /** 对外用户 ID。 */
    private String userId;

    /** 登录名。 */
    private String username;

    /** 角色集合（RoleType 名称）。 */
    private Set<String> roles = Collections.emptySet();
}
